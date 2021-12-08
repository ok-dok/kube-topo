package com.dclingcloud.kubetopo.watch;

import com.dclingcloud.kubetopo.service.IngressService;
import com.dclingcloud.kubetopo.service.NodeService;
import com.dclingcloud.kubetopo.service.ServiceService;
import com.dclingcloud.kubetopo.util.K8sApi;
import com.dclingcloud.kubetopo.util.K8sServiceException;
import com.google.gson.reflect.TypeToken;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.NetworkingV1Api;
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
import java.net.SocketTimeoutException;
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
    private NodeService nodeService;
    @Resource
    private ServiceService serviceService;
    @Resource
    private IngressService ingressService;
    @Resource
    private ApiClient apiClient;
    @Resource
    private CoreV1Api coreV1Api;
    @Resource
    private NetworkingV1Api networkingV1Api;
    private String resourceVersion;
    private Watch<V1Pod> podWatch;
    private Watch<V1Service> svcWatch;
    private Watch<V1Node> nodeWatch;
    private Watch<V1Ingress> ingressWatch;

    //    @Scheduled(cron = "0/10 * *  * * ? ")
    public void watchNode() throws ApiException {
        if (nodeWatch == null) {
            try {
                Call nodeCall = K8sApi.watchNodesAsync();
                nodeWatch = Watch.createWatch(apiClient, nodeCall, new TypeToken<Watch.Response<V1Node>>() {
                }.getType());
                for (Watch.Response<V1Node> item : nodeWatch) {
                    if (BOOKMARK.equals(item.type)) {
                        continue;
                    }
                    System.out.printf("WatchType: Node, EventType: %s, Name: %s\n", item.type, item.object.getMetadata().getName());
                    switch (item.type) {
                        case ADDED:
                            try {
//                                nodeService.save(item.object, ADDED);
                            } catch (K8sServiceException e) {
                                // TODO 保存异常处理，重试？
                            }
                            break;
                        case DELETED:
                            try {
//                                nodeService.delete(item.object);
                            } catch (K8sServiceException e) {
                                // TODO 保存异常处理，重试？
                            }
                            break;
                        case ERROR:
                            break;
                    }
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

    //    @Scheduled(cron = "0/10 * *  * * ? ")
    public void watchService() throws ApiException {
        if (svcWatch == null) {
            try {
                Call svcCall = K8sApi.watchServicesAsync();
                svcWatch = Watch.createWatch(apiClient, svcCall, new TypeToken<Watch.Response<V1Service>>() {
                }.getType());
                for (Watch.Response<V1Service> item : svcWatch) {
                    if (BOOKMARK.equals(item.type)) {
//                    this.resourceVersion = item.object.getMetadata().getResourceVersion();
//                    System.out.printf("<BOOKMARK> ResourceVersion: %s\n", item.object.getMetadata().getResourceVersion());
                        continue;
                    }
                    System.out.printf("WatchType: Service, EventType: %s, Name: %s, NS: %s\n", item.type, item.object.getMetadata().getName(), item.object.getMetadata().getNamespace());
                    switch (item.type) {
                        case ADDED:
                            try {
//                                serviceService.save(item.object, ADDED);
                            } catch (K8sServiceException e) {
                                // TODO 保存异常处理，重试？
                            }
                            break;
                        case DELETED:
                            try {
//                                serviceService.delete(item.object);
                            } catch (K8sServiceException e) {
                                // TODO 保存异常处理，重试？
                            }
                            break;
                        case ERROR:
                            break;
                    }
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

    //    @Scheduled(cron = "0/10 * *  * * ? ")
    public void watchIngress() throws ApiException {
        if (ingressWatch == null) {
            try {
                Call ingressCall = K8sApi.watchIngressesAsync();
                ingressWatch = Watch.createWatch(apiClient, ingressCall, new TypeToken<Watch.Response<V1Ingress>>() {
                }.getType());
                for (Watch.Response<V1Ingress> item : ingressWatch) {
                    if (BOOKMARK.equals(item.type)) {
                        continue;
                    }
                    System.out.printf("WatchType: Ingress, EventType: %s, Name: %s, NS: %s\n", item.type, item.object.getMetadata().getName(), item.object.getMetadata().getNamespace());
                    switch (item.type) {
                        case ADDED:
                            try {
//                                ingressService.save(item.object, ADDED);
                            } catch (K8sServiceException e) {
                                // TODO 保存异常处理，重试？
                            }
                            break;
                        case DELETED:
                            try {
//                                ingressService.delete(item.object);
                            } catch (K8sServiceException e) {
                                // TODO 保存异常处理，重试？
                            }
                            break;
                        case ERROR:
                            break;
                    }
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
                Call podCall = K8sApi.createPodsCall(this.resourceVersion);
                podWatch = Watch.createWatch(apiClient, podCall, new TypeToken<Watch.Response<V1Pod>>() {
                }.getType());
                for (Watch.Response<V1Pod> item : podWatch) {
                    System.out.printf("WatchType: Pod, EventType: %s", item.type);
                    switch (item.type) {
                        case ERROR:
                            System.out.printf("Reason: %s\n", item.status.getReason());
                            break;
                        case BOOKMARK:
                            System.out.printf(", ResourceVersion: %s\n", item.object.getMetadata().getResourceVersion());
                            this.resourceVersion = item.object.getMetadata().getResourceVersion();
                            break;
                        default:
                            String status = item.object.getStatus().getPhase();
                            if ("Running".equalsIgnoreCase(status)) {
                                List<V1ContainerStatus> containerStatuses = item.object.getStatus().getContainerStatuses();
                                if (CollectionUtils.isNotEmpty(containerStatuses)) {
                                    long readyCount = containerStatuses.stream().filter(cs -> cs.getReady()).count();
                                    if (readyCount == containerStatuses.size()) {
                                        status = "Ready";
                                    } else {
                                        status = "NotReady";
                                    }
                                } else {
                                    status = "NotReady";
                                }
                            }

                            System.out.printf(", Status: %s, Name: %s, NS: %s", status, item.object.getMetadata().getName(), item.object.getMetadata().getNamespace());
                            switch (item.type) {
                                case ADDED:

                                    break;
                                case DELETED:
//                            System.out.printf("Type: %s, Status: %s, Name: %s, ContainerID: %s \n", item.type, item.object.getStatus().getPhase(), item.object.getMetadata().getName(), item.object.getStatus().getContainerStatuses().get(0).getContainerID());
                                    break;
                                case MODIFIED:
//                            System.out.printf("Type: %s, Status: %s, Name: %s, ContainerID: %s \n", item.type, item.object.getStatus().getPhase(), item.object.getMetadata().getName(), item.object.getStatus().getContainerStatuses());
                                    break;
                            }
                            this.resourceVersion = item.object.getMetadata().getResourceVersion();
                            System.out.printf(", ResourceVersion: %s\n", item.object.getMetadata().getResourceVersion());
                    }

                }
            } catch (ApiException e) {
                if (e.getCode() == 404) {
                    // 没有pod资源存在
                } else {
                    throw e;
                }
            } catch (Exception e) {
                // IO Exception or SocketTimeoutException
                if (e.getCause() instanceof SocketTimeoutException) {
                    try {
                        this.podWatch.close();
                    } catch (IOException ex) {
                        //
                    } finally {
                        this.podWatch = null;
                    }

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
        if (nodeWatch != null) {
            try {
                nodeWatch.close();
            } catch (IOException e) {
                //
            }
        }
    }
}
