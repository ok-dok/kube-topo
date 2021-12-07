package com.dclingcloud.kubetopo.watch;

import com.google.common.reflect.TypeToken;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.NetworkingV1beta1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.Watch;
import okhttp3.Call;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;

@Component
@Async
public class EventWatcher implements InitializingBean {
    public static final String ADDED = "ADDED";
    public static final String MODIFIED = "MODIFIED";
    public static final String DELETED = "DELETED";
    public static final String BOOKMARK = "BOOKMARK";
    public static final String ERROR = "ERROR";
    @Resource
    private ApiClient apiClient;
    @Resource
    private CoreV1Api coreV1Api;
    @Resource
    private NetworkingV1beta1Api networkingV1beta1Api;
    private Watch<V1Pod> podWatch;
    private Watch<V1Service> svcWatch;
    private Watch<V1Node> nodeWatch;
    private Watch<NetworkingV1beta1Ingress> ingressWatch;
    private String resourceVersion = null;

    @Scheduled(cron = "0/10 * *  * * ? ")
    public void watchNode() throws ApiException {
        if (nodeWatch == null) {
            try {
                Call nodeCall = coreV1Api.listNodeCall(null, false, null, null, null, null, resourceVersion, 10, true, null);
                nodeWatch = Watch.createWatch(apiClient, nodeCall, new TypeToken<Watch.Response<V1Node>>() {
                }.getType());
                for (Watch.Response<V1Node> item : nodeWatch) {
                    if (BOOKMARK.equals(item.type)) {
                        continue;
                    }
                    System.out.printf("WatchType: Node, EventType: %s, Name: %s\n", item.type, item.object.getMetadata().getName());
                }
            } catch (ApiException e) {
                if (e.getCode() == 404) {
                    //
                } else {
                    throw e;
                }
            }
        }
    }

    @Scheduled(cron = "0/10 * *  * * ? ")
    public void watchService() throws ApiException {
        if (svcWatch == null) {
            try {
                Call svcCall = coreV1Api.listServiceForAllNamespacesCall(false, null, null, null, null, null, resourceVersion, 10, true, null);
                svcWatch = Watch.createWatch(apiClient, svcCall, new TypeToken<Watch.Response<V1Service>>() {
                }.getType());
                for (Watch.Response<V1Service> item : svcWatch) {
                    if (BOOKMARK.equals(item.type)) {
//                    this.resourceVersion = item.object.getMetadata().getResourceVersion();
//                    System.out.printf("<BOOKMARK> ResourceVersion: %s\n", item.object.getMetadata().getResourceVersion());
                        continue;
                    }
                    System.out.printf("WatchType: Service, EventType: %s, Name: %s, NS: %s\n", item.type, item.object.getMetadata().getName(), item.object.getMetadata().getNamespace());
                }
            } catch (ApiException e) {
                if (e.getCode() == 404) {
                    // 没有service资源存在
                } else {
                    throw e;
                }
            }
        }
    }

    @Scheduled(cron = "0/10 * *  * * ? ")
    public void watchIngress() throws ApiException {
        if (ingressWatch == null) {
            try {
                Call ingressCall = networkingV1beta1Api.listIngressForAllNamespacesCall(false, null, null, null, null, null, resourceVersion, 10, true, null);
                ingressWatch = Watch.createWatch(apiClient, ingressCall, new TypeToken<Watch.Response<NetworkingV1beta1Ingress>>() {
                }.getType());
                for (Watch.Response<NetworkingV1beta1Ingress> item : ingressWatch) {
                    if (BOOKMARK.equals(item.type)) {
                        continue;
                    }
                    System.out.printf("WatchType: Ingress, EventType: %s, Name: %s, NS: %s\n", item.type, item.object.getMetadata().getName(), item.object.getMetadata().getNamespace());
                }
            } catch (ApiException e) {
                if (e.getCode() == 404) {
                    // 没有ingress资源存在
                } else {
                    throw e;
                }
            }
        }
    }

    @Scheduled(cron = "0/10 * *  * * ? ")
    public void watchPod() throws ApiException {
        if (podWatch == null) {
            try {
                Call podCall = coreV1Api.listPodForAllNamespacesCall(false, null, null, null, null, null, resourceVersion, 10, true, null);
                podWatch = Watch.createWatch(apiClient, podCall, new TypeToken<Watch.Response<V1Pod>>() {
                }.getType());
                for (Watch.Response<V1Pod> item : podWatch) {
                    if (BOOKMARK.equals(item.type)) {
                        continue;
                    }
                    System.out.printf("WatchType: Pod, EventType: %s", item.type);
                    if (item.object == null) {
                        System.out.println(item.status);
                        continue;
                    }
                    List<V1PodCondition> conditions = item.object.getStatus().getConditions();
                    String status = item.object.getStatus().getPhase();
                    if (CollectionUtils.isNotEmpty(conditions)) {
                        conditions.sort((c1, c2) -> c2.getLastTransitionTime().compareTo(c1.getLastTransitionTime()));
                        status = conditions.get(0).getType();
                    }

                    System.out.printf(", Status: %s, Name: %s, NS: %s", status, item.object.getMetadata().getName(), item.object.getMetadata().getNamespace());
                    switch (item.type) {
                        case ADDED:
//                            System.out.printf("Type: %s, Status: %s, Name: %s, ContainerID: %s \n", item.type, item.object.getStatus().getPhase(), item.object.getMetadata().getName(), item.object.getStatus().getContainerStatuses().get(0).getContainerID());
                            break;
                        case DELETED:
//                            System.out.printf("Type: %s, Status: %s, Name: %s, ContainerID: %s \n", item.type, item.object.getStatus().getPhase(), item.object.getMetadata().getName(), item.object.getStatus().getContainerStatuses().get(0).getContainerID());
                            break;
                        case MODIFIED:
//                            System.out.printf("Type: %s, Status: %s, Name: %s, ContainerID: %s \n", item.type, item.object.getStatus().getPhase(), item.object.getMetadata().getName(), item.object.getStatus().getContainerStatuses());
                            break;
                    }
                    System.out.println();
                }
            } catch (ApiException e) {
                if (e.getCode() == 404) {
                    // 没有pod资源存在
                } else {
                    throw e;
                }
            }
        }

    }

    @Override
    public void afterPropertiesSet() throws Exception {
    }

    @PreDestroy
    public void destroy() {
        if (podWatch != null) {
            try {
                podWatch.close();
            } catch (IOException e) {
                //
            }
        }
        if (svcWatch != null) {
            try {
                svcWatch.close();
            } catch (IOException e) {
                //
            }
        }
        if (ingressWatch != null) {
            try {
                ingressWatch.close();
            } catch (IOException e) {
                //
            }
        }
        if(nodeWatch != null){
            try {
                nodeWatch.close();
            } catch (IOException e) {
                //
            }
        }
    }
}
