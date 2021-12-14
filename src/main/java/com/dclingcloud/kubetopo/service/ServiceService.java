package com.dclingcloud.kubetopo.service;

import com.dclingcloud.kubetopo.entity.ServicePO;
import com.dclingcloud.kubetopo.util.K8sServiceException;
import io.kubernetes.client.openapi.models.V1Service;

import java.util.Optional;

public interface ServiceService {
    void saveOrUpdate(V1Service service, String status) throws K8sServiceException;

    /**
     * it's not a real delete
     * @param service
     * @throws K8sServiceException
     */
    void delete(V1Service service) throws K8sServiceException;

    Optional<ServicePO> findByName(String name);
}
