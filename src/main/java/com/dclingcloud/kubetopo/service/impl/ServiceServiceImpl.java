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
    public void saveOrUpdate(V1Service service, String status) throws K8sServiceException {
        ServicePO svcPO = serviceRepository.findById(service.getMetadata().getUid())
                .orElse(ServicePO.builder().uid(service.getMetadata().getUid()).build());
        V1ServiceSpec spec = service.getSpec();
        svcPO.setName(service.getMetadata().getName())
                .setNamespace(service.getMetadata().getNamespace())
                .setType(spec.getType())
                .setClusterIP(spec.getClusterIP())
                .setExternalName(spec.getExternalName())
                .setExternalIPs(StringUtils.joinWith(",", spec.getExternalIPs()))
                .setLoadBalancerIP(spec.getLoadBalancerIP())
                .setStatus(status)
                .setGmtCreate(service.getMetadata().getCreationTimestamp().toLocalDateTime());
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
