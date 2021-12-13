package com.dclingcloud.kubetopo.watch;

import com.dclingcloud.kubetopo.service.NodeService;
import com.dclingcloud.kubetopo.util.K8sApi;
import com.dclingcloud.kubetopo.util.K8sServiceException;
import com.google.gson.reflect.TypeToken;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.util.Watch;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Optional;

@Component
@Slf4j
@Async
public class NodeEventWatcher extends EventWatcher<V1Node> {
    @Resource
    private NodeService nodeService;

    @Override
    protected void processEventObject(String type, Object object, StringBuilder eventLog) {
        V1Node node = (V1Node) object;
        eventLog.append(", IP: ")
                .append(Optional.ofNullable(node.getStatus().getAddresses())
                        .map(l -> l.get(0))
                        .map(addr -> addr.getAddress())
                        .orElse(""))
                .append(", PodCIDR: ")
                .append(node.getSpec().getPodCIDR());
        switch (type.toUpperCase()) {
            case ADDED:
                try {
                    nodeService.saveOrUpdate(node, ADDED);
                } catch (K8sServiceException e) {
                    // TODO 保存异常处理，重试？
                    throw e;
                }
                break;
            case DELETED:
                try {
                    nodeService.delete(node);
                } catch (K8sServiceException e) {
                    // TODO 保存异常处理，重试？
                    throw e;
                }
                break;
            case MODIFIED:
                try {
                    nodeService.saveOrUpdate(node, MODIFIED);
                } catch (K8sServiceException e) {
                    // TODO 保存异常处理，重试？
                    throw e;
                }
                break;
        }
    }

    @Override
    protected Watch<V1Node> createWatch() throws ApiException {
        Call call = K8sApi.createNodesCall(this.resourceVersion);
        Watch<V1Node> watch = Watch.createWatch(apiClient, call, new TypeToken<Watch.Response<V1Node>>() {
        }.getType());
        return watch;
    }
}
