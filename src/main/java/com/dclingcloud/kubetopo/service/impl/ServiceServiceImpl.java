package com.dclingcloud.kubetopo.service.impl;

import com.dclingcloud.kubetopo.entity.ServicePO;
import com.dclingcloud.kubetopo.repository.ServiceRepository;
import com.dclingcloud.kubetopo.service.ServiceService;
import com.dclingcloud.kubetopo.util.K8sServiceException;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServiceSpec;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.persistence.PersistenceException;
import javax.transaction.Transactional;

@Service
@Slf4j
public class ServiceServiceImpl implements ServiceService {
    @Resource
    private ServiceRepository serviceRepository;

    @Override
    @Transactional
    public void save(V1Service service, String status) throws K8sServiceException {
        V1ServiceSpec spec = service.getSpec();
        ServicePO svcPO = ServicePO.builder()
                .uid(service.getMetadata().getUid())
                .name(service.getMetadata().getName())
                .namespace(service.getMetadata().getNamespace())
                .type(spec.getType())
                .clusterIP(spec.getClusterIP())
                .externalName(spec.getExternalName())
                .externalIPs(StringUtils.joinWith(",", spec.getExternalIPs()))
                .loadBalancerIP(spec.getLoadBalancerIP())
                .status(status)
                .gmtCreate(service.getMetadata().getCreationTimestamp().toLocalDateTime())
                .build();
        try {
            serviceRepository.save(svcPO);
        } catch (PersistenceException e) {
            log.error("Error: save {} failed. {}", svcPO.getClass().getName(), svcPO, e);
            throw new K8sServiceException("Unable to save service", e);
        }
    }

    @Override
    @Transactional
    public void delete(V1Service service) throws K8sServiceException {
        String uid = service.getMetadata().getUid();
        try {
            serviceRepository.updateStatusByUid(uid, "DELETED");
        } catch (PersistenceException e) {
            log.error("Error: update {}'s status to 'DELETED' failed. uid={}", V1Service.class.getName(), uid, e);
            throw new K8sServiceException("Unable to delete service", e);
        }
    }
}
