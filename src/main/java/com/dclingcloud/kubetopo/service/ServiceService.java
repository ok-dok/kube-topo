package com.dclingcloud.kubetopo.service;

import com.dclingcloud.kubetopo.util.K8sServiceException;
import io.kubernetes.client.openapi.models.V1Service;

public interface ServiceService {
    void save(V1Service service, String status) throws K8sServiceException;

    /**
     * it's not a real delete
     * @param service
     * @throws K8sServiceException
     */
    void delete(V1Service service) throws K8sServiceException;
}
