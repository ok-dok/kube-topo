package com.dclingcloud.kubetopo.service.impl;

import com.dclingcloud.kubetopo.entity.PodPortPO;
import com.dclingcloud.kubetopo.entity.ServicePO;
import com.dclingcloud.kubetopo.entity.ServicePortPO;
import com.dclingcloud.kubetopo.repository.ServicePortRepository;
import com.dclingcloud.kubetopo.service.ServicePortService;
import com.dclingcloud.kubetopo.util.K8sServiceException;
import io.kubernetes.client.custom.IntOrString;
import io.kubernetes.client.openapi.models.V1ServiceBackendPort;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.persistence.PersistenceException;
import javax.transaction.Transactional;
import java.util.Collection;
import java.util.Optional;

@Service
@Slf4j
public class ServicePortServiceImpl implements ServicePortService {
    @Resource
    private ServicePortRepository servicePortRepository;

    @Transactional
    @Override
    public void saveOrUpdate(ServicePortPO servicePortPO) throws K8sServiceException {
        try {
            servicePortRepository.saveAndFlush(servicePortPO);
        } catch (PersistenceException e) {
            log.error("Error: save {} failed. {}", ServicePortPO.class.getName(), servicePortPO, e);
            throw new K8sServiceException("Unable to save " + ServicePortPO.class.getName(), e);
        }
    }

    @Transactional
    @Override
    public void saveAll(Collection<ServicePortPO> servicePorts) throws K8sServiceException {
        if (CollectionUtils.isNotEmpty(servicePorts)) {
            servicePorts.forEach(this::saveOrUpdate);
        }
    }

    @Override
    public Optional<ServicePortPO> findByNamespacedServiceNameAndPort(String namespace, String serviceName, V1ServiceBackendPort port) {
        try {
            if (StringUtils.isNotBlank(port.getName())) {
                return servicePortRepository.findByNamespacedServiceNameAndPortName(namespace, serviceName, port.getName());
            } else {
                return servicePortRepository.findByNamespacedServiceNameAndTCPPortNumber(namespace, serviceName, port.getNumber());
            }
        } catch (Exception e) {
            log.error("Error: query {} failed by service name \"{}\" and port name \"{}\" or number \"{}\".", ServicePortPO.class.getName(), serviceName, port.getName(), port.getNumber(), e);
            throw new K8sServiceException("Unable to query " + ServicePortPO.class.getName(), e);
        }
    }

    @Transactional
    @Override
    public void deleteAllByServiceUid(String serviceUid) {
        try {
            servicePortRepository.updateStatusByServiceUid(serviceUid, "DELETED");
        } catch (Exception e) {
            log.error("Error: update {}'s status to 'DELETED' failed. serviceUid={}", PodPortPO.class.getName(), serviceUid, e);
            throw new K8sServiceException("Unable to delete " + ServicePortPO.class.getSimpleName() + " by serviceUid", e);
        }
    }

    @Override
    public Optional<IntOrString> getTargetPort(String servicePortUid) {
        try {
            return servicePortRepository.getTargetPortByUid(servicePortUid);
        } catch (Exception e) {
            log.error("Error: can not find the targetPort of {} by servicePortUid. servicePortUid={}", PodPortPO.class.getName(), servicePortUid, e);
            throw new K8sServiceException("Unable to find " + ServicePortPO.class.getSimpleName() + "'s targetPort by servicePortUid", e);
        }
    }

    @Override
    public Optional<ServicePortPO> findByServiceUidAndTargetPortAndProtocol(String serviceUid, IntOrString targetPort, String protocol) {
        try {
            return servicePortRepository.findByServiceAndTargetPortAndProtocol(ServicePO.builder().uid(serviceUid).build(), targetPort, protocol);
        } catch (Exception e) {
            log.error("Error: can not find {} by serviceUid and targetPort and protocol. serviceUid={}, targetPort={}, protocol={}", PodPortPO.class.getName(), serviceUid, targetPort, protocol, e);
            throw new K8sServiceException("Unable to find " + ServicePortPO.class.getSimpleName() + "'s targetPort by serviceUid and targetPort and protocol", e);
        }
    }
}
