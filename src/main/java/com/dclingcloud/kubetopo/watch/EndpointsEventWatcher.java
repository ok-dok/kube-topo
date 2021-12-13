package com.dclingcloud.kubetopo.watch;

import com.dclingcloud.kubetopo.entity.PodPO;
import com.dclingcloud.kubetopo.entity.PodPortPO;
import com.dclingcloud.kubetopo.service.PodPortService;
import com.dclingcloud.kubetopo.util.K8sApi;
import com.google.gson.reflect.TypeToken;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.CoreV1EndpointPort;
import io.kubernetes.client.openapi.models.V1EndpointAddress;
import io.kubernetes.client.openapi.models.V1EndpointSubset;
import io.kubernetes.client.openapi.models.V1Endpoints;
import io.kubernetes.client.util.Watch;
import okhttp3.Call;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Base64Utils;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class EndpointsEventWatcher extends EventWatcher<V1Endpoints> {
    @Resource
    private PodPortService podPortService;

    @Override
    protected void processEventObject(String type, Object object, StringBuilder eventLog) {
        V1Endpoints endpoints = (V1Endpoints) object;
        List<V1EndpointSubset> subsets = endpoints.getSubsets();
        if (subsets == null)
            return;
        Map<String, List<PodPortPO>> podPortMap = new HashMap<>();
        for (V1EndpointSubset subset : subsets) {
            List<V1EndpointAddress> addresses = subset.getAddresses();
            if (CollectionUtils.isEmpty(addresses) && CollectionUtils.isNotEmpty(subset.getNotReadyAddresses())) {
                addresses = subset.getNotReadyAddresses();
            }
            for (V1EndpointAddress address : addresses) {
                PodPO podPO = null;
                if (address.getTargetRef() != null) {
                    podPO = PodPO.builder()
                            .uid(address.getTargetRef().getUid())
                            .name(address.getTargetRef().getName())
                            .namespace(address.getTargetRef().getNamespace())
                            .nodeName(address.getNodeName())
                            .hostname(address.getHostname())
                            .ip(address.getIp())
                            .build();
                    podPortService.save(podPO);
                }

                // Headless services with no ports.
                if (subset.getPorts().size() != 0) {
                    for (CoreV1EndpointPort port : subset.getPorts()) {
                        String id = endpoints.getMetadata().getUid() + ":" + Optional.ofNullable(address.getTargetRef()).map(t -> t.getUid()).map(Objects::toString).orElse("null") + ":" + port.getPort() + ":" + port.getProtocol();
                        String hash = DigestUtils.md5DigestAsHex(Base64Utils.encode(id.getBytes(StandardCharsets.UTF_8)));
                        String uid = new StringBuilder(hash).insert(20, '-').insert(16, '-').insert(12, '-').insert(8, '-').toString();
                        PodPortPO podPortPO = PodPortPO.builder()
                                .uid(uid)
                                .epUid(endpoints.getMetadata().getUid())
                                .pod(podPO)
                                .name(port.getName())
                                .port(port.getPort())
                                .protocol(port.getProtocol())
                                .appProtocol(port.getAppProtocol())
                                .build();
                        if (!podPortMap.containsKey(podPortPO.getPort()) && podPortPO.getPort() != null) {
                            podPortMap.put(podPortPO.getPort().toString(), new ArrayList<PodPortPO>());
                        }
                        if (!podPortMap.containsKey(podPortPO.getName()) && StringUtils.isNotBlank(podPortPO.getName())) {
                            podPortMap.put(podPortPO.getName(), new ArrayList<PodPortPO>());
                        }
                        if (podPortPO.getPort() != null) {
                            podPortMap.get(podPortPO.getPort().toString()).add(podPortPO);
                        }
                        if (StringUtils.isNotBlank(podPortPO.getName())) {
                            podPortMap.get(podPortPO.getName()).add(podPortPO);
                        }
                    }
                }
            }
        }
    }

    @Override
    protected Watch<V1Endpoints> createWatch() throws ApiException {
        Call endpointsCall = K8sApi.createEndpointsCall(this.resourceVersion);
        Watch<V1Endpoints> watch = Watch.createWatch(apiClient, endpointsCall, new TypeToken<Watch.Response<V1Endpoints>>() {
        }.getType());
        return watch;
    }
}
