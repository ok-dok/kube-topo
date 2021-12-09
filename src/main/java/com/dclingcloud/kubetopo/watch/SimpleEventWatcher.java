package com.dclingcloud.kubetopo.watch;

import com.dclingcloud.kubetopo.service.IngressService;
import com.dclingcloud.kubetopo.service.NodeService;
import com.dclingcloud.kubetopo.service.ServiceService;
import com.dclingcloud.kubetopo.util.K8sApi;
import com.dclingcloud.kubetopo.util.K8sServiceException;
import com.google.gson.reflect.TypeToken;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.Watch;
import okhttp3.Call;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.InitializingBean;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//@Component
//@Async
public class SimpleEventWatcher implements InitializingBean {
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
    private String resourceVersion = null;
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
                switch (e.getCode()) {
                    case 404:  // 没有该资源存在
                        break;
                    case 410: // to old resourceVersion
                        // clean watch, wait to retry
                        try {
                            this.nodeWatch.close();
                        } catch (IOException ex) {
                            // do nothing
                        } finally {
                            this.nodeWatch = null;
                        }
                        break;
                    default:
                        throw e;
                }
            } catch (Exception e) {
                // IO Exception or SocketTimeoutException
                if (e.getCause() instanceof SocketTimeoutException) {
                    // clean watch, wait to retry
                    try {
                        this.nodeWatch.close();
                    } catch (IOException ex) {
                        // do nothing
                    } finally {
                        this.nodeWatch = null;
                    }
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
                switch (e.getCode()) {
                    case 404:  // 没有该资源存在
                        break;
                    case 410: // to old resourceVersion
                        // clean watch, wait to retry
                        try {
                            this.svcWatch.close();
                        } catch (IOException ex) {
                            // do nothing
                        } finally {
                            this.svcWatch = null;
                        }
                        break;
                    default:
                        throw e;
                }
            } catch (Exception e) {
                // IO Exception or SocketTimeoutException
                if (e.getCause() instanceof SocketTimeoutException) {
                    // clean watch, wait to retry
                    try {
                        this.svcWatch.close();
                    } catch (IOException ex) {
                        // do nothing
                    } finally {
                        this.svcWatch = null;
                    }
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
                    System.out.printf("WatchType: Ingress, EventType: %s", item.type);
                    switch (item.type) {
                        case ERROR:
                            // record the new resourceVersion
                            processResponseError(item);
                            System.out.printf(", Reason: %s, the new resourceVersion: %s\n", item.status.getReason(), this.resourceVersion);
                            break;
                        case BOOKMARK:
                            // record the new resourceVersion
                            this.resourceVersion = item.object.getMetadata().getResourceVersion();
                            System.out.printf(", the new resourceVersion: %s\n", item.object.getMetadata().getResourceVersion());
                            break;
                        case ADDED:
                            System.out.printf(", Name: %s, NS: %s", item.object.getMetadata().getName(), item.object.getMetadata().getNamespace());
                            try {
                                ingressService.save(item.object, ADDED);
                            } catch (K8sServiceException e) {
                                // TODO 保存异常处理，重试？
                            }
                            break;
                        case DELETED:
                            System.out.printf(", Name: %s, NS: %s", item.object.getMetadata().getName(), item.object.getMetadata().getNamespace());
                            try {
                                ingressService.delete(item.object);
                            } catch (K8sServiceException e) {
                                // TODO 保存异常处理，重试？
                            }
                            break;
                    }
                }
            } catch (ApiException e) {
                switch (e.getCode()) {
                    case 404:  // 没有该资源存在
                        break;
                    case 410: // to old resourceVersion
                        // clean watch, wait to retry
                        try {
                            this.ingressWatch.close();
                        } catch (IOException ex) {
                            // do nothing
                        } finally {
                            this.ingressWatch = null;
                        }
                        break;
                    default:
                        throw e;
                }
            } catch (Exception e) {
                // IO Exception or SocketTimeoutException
                if (e.getCause() instanceof SocketTimeoutException) {
                    // clean watch, wait to retry
                    try {
                        this.ingressWatch.close();
                    } catch (IOException ex) {
                        // do nothing
                    } finally {
                        this.ingressWatch = null;
                    }
                } else {
                    throw e;
                }
            }
        }
    }

    //    @Scheduled(cron = "0/10 * *  * * ? ")
    public void watchPod() throws ApiException {
        if (podWatch == null) {
            try {
                Call podCall = K8sApi.createPodsCall(this.resourceVersion);
                podWatch = Watch.createWatch(apiClient, podCall, new TypeToken<Watch.Response<V1Pod>>() {
                }.getType());
                for (Watch.Response<V1Pod> item : podWatch) {
                    System.out.printf("WatchType: Pod, EventType: %s", item.type);
                    switch (item.type.toUpperCase()) {
                        case ERROR:
                            // record the new resourceVersion
                            processResponseError(item);
                            System.out.printf(", Reason: %s, the new resourceVersion: %s\n", item.status.getReason(), this.resourceVersion);
                            break;
                        case BOOKMARK:
                            // record the new resourceVersion
                            this.resourceVersion = item.object.getMetadata().getResourceVersion();
                            System.out.printf(", the new resourceVersion: %s\n", item.object.getMetadata().getResourceVersion());
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
                switch (e.getCode()) {
                    case 404:  // 没有该资源存在
                        break;
                    case 410: // to old resourceVersion
                        // clean watch, wait to retry
                        try {
                            this.podWatch.close();
                        } catch (IOException ex) {
                            // do nothing
                        } finally {
                            this.podWatch = null;
                        }
                        break;
                    default:
                        throw e;
                }
            } catch (Exception e) {
                // IO Exception or SocketTimeoutException
                if (e.getCause() instanceof SocketTimeoutException) {
                    // clean watch, wait to retry
                    try {
                        this.podWatch.close();
                    } catch (IOException ex) {
                        // do nothing
                    } finally {
                        this.podWatch = null;
                    }
                } else {
                    throw e;
                }
            }
        }

    }

    private void processResponseError(Watch.Response item) throws ApiException {
        Pattern compile = Pattern.compile("(\\D*)(\\d+)(\\D*)(\\d+)(.*)");
        Matcher matcher = compile.matcher("too old resource version: 23089614 (23411270)");
        if (matcher.matches()) {
            this.resourceVersion = matcher.group(4);
        }
        throw new ApiException(item.status.getCode(), item.status.getMessage());
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        resourceVersion = "23089614";
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
