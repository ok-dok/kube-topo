package com.dclingcloud.kubetopo.service.impl;

import com.dclingcloud.kubetopo.entity.IngressPO;
import com.dclingcloud.kubetopo.repository.IngressRepository;
import com.dclingcloud.kubetopo.service.IngressService;
import com.dclingcloud.kubetopo.util.K8sServiceException;
import io.kubernetes.client.openapi.models.V1Ingress;
import io.kubernetes.client.openapi.models.V1LoadBalancerIngress;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.persistence.PersistenceException;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class IngressServiceImpl implements IngressService {
    @Resource
    private IngressRepository ingressRepository;

    @Transactional
    @Override
    public void save(V1Ingress ingress, String status) throws K8sServiceException {
        IngressPO ingressPO = IngressPO.builder()
                .uid(ingress.getMetadata().getUid())
                .name(ingress.getMetadata().getName())
                .namespace(ingress.getMetadata().getNamespace())
                .className(ingress.getSpec().getIngressClassName())
                .status(status)
                .gmtCreate(ingress.getMetadata().getCreationTimestamp().toLocalDateTime())
                .build();
        // 获取负载均衡IP地址列表
        List<V1LoadBalancerIngress> lbIngresses = ingress.getStatus().getLoadBalancer().getIngress();
        List<String> ips = new ArrayList<>(lbIngresses.size());
        for (V1LoadBalancerIngress lbIngress : lbIngresses) {
            if (StringUtils.isNotBlank(lbIngress.getIp())) {
                ips.add(lbIngress.getIp());
            } else {
                ips.add(lbIngress.getHostname());
            }
        }
        ingressPO.setLoadBalancerHosts(StringUtils.join(ips, ","));
        try {
            ingressRepository.save(ingressPO);
        } catch (PersistenceException e) {
            log.error("Error: save {} failed. {}", IngressPO.class.getName(), ingressPO, e);
            throw new K8sServiceException("Unable to save " + IngressPO.class.getSimpleName(), e);
        }
    }

    @Transactional
    @Override
    public void delete(V1Ingress ingress) throws K8sServiceException {
        String uid = ingress.getMetadata().getUid();
        try {
            ingressRepository.updateStatusByUid(uid, "DELETED");
        } catch (Exception e) {
            log.error("Error: update {}'s status to 'DELETED' failed. uid={}", IngressPO.class.getName(), uid, e);
            throw new K8sServiceException("Unable to delete {}" + IngressPO.class.getSimpleName(), e);
        }
    }
}
