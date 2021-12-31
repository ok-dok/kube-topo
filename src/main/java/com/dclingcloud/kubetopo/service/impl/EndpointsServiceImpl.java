package com.dclingcloud.kubetopo.service.impl;

import com.dclingcloud.kubetopo.entity.BackendEndpointRelationPO;
import com.dclingcloud.kubetopo.entity.PodPortPO;
import com.dclingcloud.kubetopo.entity.ServicePortPO;
import com.dclingcloud.kubetopo.repository.BackendEndpointRelationRepository;
import com.dclingcloud.kubetopo.service.EndpointsService;
import com.dclingcloud.kubetopo.util.K8sServiceException;
import com.dclingcloud.kubetopo.watch.EventType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.transaction.Transactional;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional(rollbackOn = K8sServiceException.class)
public class EndpointsServiceImpl implements EndpointsService {
    @Resource
    private BackendEndpointRelationRepository backendEndpointRelationRepository;

    @Override
    public void saveRelation(@NonNull final ServicePortPO servicePort, @NonNull Collection<PodPortPO> podPorts) throws K8sServiceException {
        if (servicePort == null) {
            throw new K8sServiceException("Error: save " + BackendEndpointRelationPO.class.getSimpleName() + " failed, " + ServicePortPO.class.getSimpleName() + " can not be null");
        }
        if (podPorts == null) {
            throw new K8sServiceException("Error: save " + BackendEndpointRelationPO.class.getSimpleName() + " failed, " + PodPortPO.class.getSimpleName() + " collection can not be null");
        }
        if (podPorts.size() == 0) {
            return;
        }
        List<BackendEndpointRelationPO> list = podPorts.stream().distinct().map(podPort ->
                BackendEndpointRelationPO.builder()
                        .podPort(podPort)
                        .servicePort(servicePort)
                        .status(EventType.ADDED)
                        .gmtCreate(servicePort.getGmtCreate())
                        .build()
        ).collect(Collectors.toList());
        saveAll(list);
    }

    @Override
    public void saveRelation(@NonNull PodPortPO podPort, @NonNull Collection<ServicePortPO> servicePorts) throws K8sServiceException {
        if (podPort == null) {
            throw new K8sServiceException("Error: save " + BackendEndpointRelationPO.class.getSimpleName() + " failed, " + PodPortPO.class.getSimpleName() + " can not be null");
        }
        if (servicePorts == null) {
            throw new K8sServiceException("Error: save " + BackendEndpointRelationPO.class.getSimpleName() + " failed, " + ServicePortPO.class.getSimpleName() + " collection can not be null");
        }
        if (servicePorts.size() == 0) {
            return;
        }
        List<BackendEndpointRelationPO> list = servicePorts.stream().distinct().map(servicePort ->
                BackendEndpointRelationPO.builder()
                        .podPort(podPort)
                        .servicePort(servicePort)
                        .status(EventType.ADDED)
                        .gmtCreate(podPort.getGmtCreate())
                        .build()
        ).collect(Collectors.toList());
        saveAll(list);
    }

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
}
