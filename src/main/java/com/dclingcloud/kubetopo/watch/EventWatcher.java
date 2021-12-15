package com.dclingcloud.kubetopo.watch;

import com.dclingcloud.kubetopo.service.impl.TopologyServiceImpl;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.util.Watch;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public abstract class EventWatcher<T extends KubernetesObject> implements EventType, InitializingBean {
    @Resource
    protected TopologyServiceImpl topologyService;
    @Resource
    protected ApiClient apiClient;
    protected String resourceVersion = null;
    protected Watch<T> watch = null;
    protected Class<T> tClazz = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];


    protected void processResponseError(Watch.Response item) throws ApiException {
        Pattern compile = Pattern.compile("(\\D*)(\\d+)(\\D*)(\\d+)(.*)");
        Matcher matcher = compile.matcher("too old resource version: 23089614 (23411270)");
        if (matcher.matches()) {
            this.resourceVersion = matcher.group(4);
        }
        throw new ApiException(item.status.getCode(), item.status.getMessage());
    }

    @Scheduled(cron = "0/10 * *  * * ? ")
    @Async
    public void execute() throws ApiException {
        if (watch != null) {
            return;
        }

        try {
            watch = createWatch();
            // 下面代码会阻塞持续运行，直到发生异常
            for (Watch.Response<T> item : watch) {
                StringBuilder eventLog = new StringBuilder();
                eventLog.append("WatchType: ")
                        .append(tClazz.getSimpleName())
                        .append(", EventType: ")
                        .append(item.type);
                switch (item.type.toUpperCase()) {
                    case ERROR:
                        // record the new resourceVersion
                        processResponseError(item);
                        eventLog.append(", Reason: ")
                                .append(item.status.getReason())
                                .append(", the new resourceVersion: ")
                                .append(this.resourceVersion);
                        break;
                    case BOOKMARK:
                        // record the new resourceVersion
                        this.resourceVersion = item.object.getMetadata().getResourceVersion();
                        eventLog.append(", the new resourceVersion: ")
                                .append(this.resourceVersion);
                        break;
                    default:
                        eventLog.append(", Name: ")
                                .append(item.object.getMetadata().getName());
                        if (!(item.object instanceof V1Node)) {
                            eventLog.append(", Namespace: ")
                                    .append(item.object.getMetadata().getNamespace());
                        }
                        processEventObject(item.type, item.object, eventLog);
                        this.resourceVersion = item.object.getMetadata().getResourceVersion();
                        eventLog.append(", ResourceVersion: ")
                                .append(this.resourceVersion);
                }
                log.info(eventLog.toString());
            }
        } catch (ApiException e) {
            switch (e.getCode()) {
                case 404:  // 没有该资源存在
                    break;
                case 410: // to old resourceVersion
                    // clean watch, wait to retry
                    cleanWatch();
                    break;
                default:
                    throw e;
            }
        } catch (Exception e) {
            // IO Exception or SocketTimeoutException
            if (e.getCause() instanceof SocketTimeoutException) {
                // clean watch, wait to retry
                cleanWatch();
            } else if (e.getCause() instanceof SocketException) {
                // Socket Closed
                cleanWatch();
            } else {
                throw e;
            }
        }
    }

    protected abstract void processEventObject(String type, T object, StringBuilder eventLog);

    protected abstract Watch<T> createWatch() throws ApiException;

    @Override
    public void afterPropertiesSet() throws Exception {
//        topologyService.loadResources();
//        this.resourceVersion = "23089614";
    }

    @PreDestroy
    public void cleanWatch() {
        try {
            if (this.watch != null) {
                this.watch.close();
            }
        } catch (IOException ex) {
            // do nothing
        } finally {
            this.watch = null;
        }
    }

}
