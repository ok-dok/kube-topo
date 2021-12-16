package com.dclingcloud.kubetopo.service;

import com.dclingcloud.kubetopo.entity.ServicePortPO;
import com.dclingcloud.kubetopo.util.K8sServiceException;
import io.kubernetes.client.openapi.models.V1ServiceBackendPort;

import java.util.Collection;
import java.util.Optional;

public interface ServicePortService {
    void saveOrUpdate(ServicePortPO servicePortPO) throws K8sServiceException;

    void saveAll(Collection<ServicePortPO> servicePorts) throws K8sServiceException;

    Optional<ServicePortPO> findOneByNamespacedServiceNameAndPort(String namespace, String serviceName, V1ServiceBackendPort port);


    /**
     * it's not a real delete action
     *
     * @param serviceUid
     */
    void deleteAllByServiceUid(String serviceUid);
}
