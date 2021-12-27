package com.dclingcloud.kubetopo.service;

import com.dclingcloud.kubetopo.entity.PodPortPO;
import com.dclingcloud.kubetopo.util.K8sServiceException;
import io.kubernetes.client.custom.IntOrString;

import java.util.Collection;
import java.util.Optional;

public interface PodPortService {
    void save(PodPortPO podPortPO) throws K8sServiceException;

    void saveAll(Collection<PodPortPO> podPorts) throws K8sServiceException;

    /**
     * it's not a real delete action
     *
     * @param podUid
     * @throws K8sServiceException
     */
    void deleteAllByPodUid(String podUid) throws K8sServiceException;

    Optional<PodPortPO> find(String podUid, IntOrString targetPort, String protocol);
}
