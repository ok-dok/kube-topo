package com.dclingcloud.kubetopo.service.impl;

import com.dclingcloud.kubetopo.entity.ServicePortPO;
import com.dclingcloud.kubetopo.repository.ServicePortRepository;
import com.dclingcloud.kubetopo.service.ServicePortService;
import com.dclingcloud.kubetopo.util.K8sServiceException;
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
    public Optional<ServicePortPO> findOneByServiceNameAndPort(String serviceName, V1ServiceBackendPort port) {
        try {
            if (StringUtils.isNotBlank(port.getName())) {
                return servicePortRepository.findByServiceNameAndPortName(serviceName, port.getName());
            } else {
                return servicePortRepository.findByServiceNameAndTCPPortNumber(serviceName, port.getNumber());
            }
        } catch (Exception e) {
            log.error("Error: query {} failed by service name \"{}\" and port name \"{}\" or number \"{}\".", ServicePortPO.class.getName(), serviceName, port.getName(), port.getNumber(), e);
            throw new K8sServiceException("Unable to query " + ServicePortPO.class.getName(), e);
        }
    }
}
