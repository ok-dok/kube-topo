package com.dclingcloud.kubetopo.service.impl;

import com.dclingcloud.kubetopo.entity.BackendEndpointRelationPO;
import com.dclingcloud.kubetopo.entity.PodPortPO;
import com.dclingcloud.kubetopo.entity.ServicePortPO;
import com.dclingcloud.kubetopo.repository.BackendEndpointRelationRepository;
import com.dclingcloud.kubetopo.service.BackendEndpointRelationService;
import com.dclingcloud.kubetopo.service.ServicePortService;
import com.dclingcloud.kubetopo.util.K8sServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.persistence.PersistenceException;
import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@Transactional(rollbackOn = K8sServiceException.class)
public class BackendEndpointRelationServiceImpl implements BackendEndpointRelationService {
    @Resource
    private BackendEndpointRelationRepository backendEndpointRelationRepository;
    @Resource
    private ServicePortService servicePortService;

    @Override
    @Transactional(rollbackOn = K8sServiceException.class)
    public void saveAll(Collection<BackendEndpointRelationPO> collection) throws K8sServiceException {
        try {
            backendEndpointRelationRepository.saveAll(collection);
        } catch (Exception e) {
            log.error("Error: save all entities for type {} failed.", BackendEndpointRelationPO.class.getName(), e);
            throw new K8sServiceException("Unable to save collection of " + BackendEndpointRelationPO.class.getSimpleName(), e);
        }
    }

    @Override
    public Optional<BackendEndpointRelationPO> findByServicePortUidAndPodPortUid(ServicePortPO servicePort, PodPortPO podPort) {
        try {
            return backendEndpointRelationRepository.findByServicePortAndPodPort(servicePort, podPort);
        } catch (Exception e) {
            log.error("Error: failed to find {} by servicePort and podPort.", BackendEndpointRelationPO.class.getName(), servicePort, podPort, e);
            throw new K8sServiceException("Unable to find " + BackendEndpointRelationPO.class.getSimpleName(), e);
        }
    }

    @Override
    public void deleteByServiceUidBeforeModifiedDateTime(String serviceUid, LocalDateTime gmtModified) {
        try {
        List<String> servicePortUids = servicePortService.findAllUidByServiceUid(serviceUid);
            backendEndpointRelationRepository.deleteAllByServicePortUidsAndGmtMidfiedBefore(servicePortUids, gmtModified);
        } catch (PersistenceException e) {
            log.error("Error: update all {}'s status to 'DELETED' by serviceUid failed, serviceUid={}", BackendEndpointRelationPO.class.getName(), serviceUid, e);
            throw new K8sServiceException("Unable to update " + BackendEndpointRelationPO.class.getSimpleName() + "' status to 'DELETED'", e);
        }
    }
}
