package com.dclingcloud.kubetopo.service;

import com.dclingcloud.kubetopo.entity.PodPortPO;
import com.dclingcloud.kubetopo.util.K8sServiceException;

import java.util.Collection;

public interface PodPortService {
    void save(PodPortPO podPortPO) throws K8sServiceException;

    void saveAll(Collection<PodPortPO> podPorts) throws K8sServiceException;

    /**
     * it's not a real delete action
     *
     * @param epUid
     * @throws K8sServiceException
     */
    void deleteAllByEndpointsUid(String epUid) throws K8sServiceException;
}
