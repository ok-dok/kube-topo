package com.dclingcloud.kubetopo.service;

import com.dclingcloud.kubetopo.entity.BackendEndpointRelationPO;
import com.dclingcloud.kubetopo.entity.PodPortPO;
import com.dclingcloud.kubetopo.entity.ServicePortPO;
import com.dclingcloud.kubetopo.util.K8sServiceException;
import org.springframework.lang.NonNull;

import java.util.Collection;
import java.util.Optional;

public interface BackendEndpointRelationService {
    void saveAll(Collection<BackendEndpointRelationPO> collection) throws K8sServiceException;

    Optional<BackendEndpointRelationPO> findByServicePortUidAndPodPortUid(ServicePortPO servicePort, PodPortPO podPort);
}
