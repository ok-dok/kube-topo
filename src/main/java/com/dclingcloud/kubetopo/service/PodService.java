package com.dclingcloud.kubetopo.service;

import com.dclingcloud.kubetopo.entity.PodPO;
import com.dclingcloud.kubetopo.util.K8sServiceException;
import io.kubernetes.client.openapi.models.V1Pod;

public interface PodService {
    void saveOrUpdate(V1Pod pod, String status) throws K8sServiceException;

    void save(PodPO podPO) throws K8sServiceException;

    /**
     * it's not a real delete
     *
     * @param pod
     * @throws K8sServiceException
     */
    void delete(V1Pod pod) throws K8sServiceException;
}
