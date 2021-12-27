package com.dclingcloud.kubetopo.repository;

import com.dclingcloud.kubetopo.entity.BackendEndpointRelationPO;
import com.dclingcloud.kubetopo.entity.PodPortPO;
import com.dclingcloud.kubetopo.entity.ServicePortPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BackendEndpointRelationRepository extends JpaRepository<BackendEndpointRelationPO, Long> {
    Optional<BackendEndpointRelationPO> findByServicePortAndPodPort(ServicePortPO servicePortPO, PodPortPO podPortPO);
}
