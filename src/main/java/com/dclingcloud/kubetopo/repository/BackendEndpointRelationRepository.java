package com.dclingcloud.kubetopo.repository;

import com.dclingcloud.kubetopo.entity.BackendEndpointRelationPO;
import com.dclingcloud.kubetopo.entity.PodPortPO;
import com.dclingcloud.kubetopo.entity.ServicePortPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BackendEndpointRelationRepository extends JpaRepository<BackendEndpointRelationPO, Long> {
    Optional<BackendEndpointRelationPO> findByServicePortAndPodPort(ServicePortPO servicePortPO, PodPortPO podPortPO);

    @Modifying
    @Query("update BackendEndpointRelationPO set status = 'DELETED' where servicePort in (:servicePortUids) and gmtModified < :gmtModified and status <> 'DELETED'")
    void deleteAllByServicePortUidsAndGmtMidfiedBefore(List<String> servicePortUids, LocalDateTime gmtModified);
}
