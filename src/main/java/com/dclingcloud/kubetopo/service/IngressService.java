package com.dclingcloud.kubetopo.service;

import com.dclingcloud.kubetopo.util.K8sServiceException;
import io.kubernetes.client.openapi.models.V1Ingress;

public interface IngressService {
    void save(V1Ingress ingress, String status) throws K8sServiceException;

    /**
     * it's not a real delete
     *
     * @param ingress
     * @throws K8sServiceException
     */
    void delete(V1Ingress ingress) throws K8sServiceException;
}
