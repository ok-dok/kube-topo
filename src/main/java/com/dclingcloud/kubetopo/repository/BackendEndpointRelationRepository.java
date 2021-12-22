package com.dclingcloud.kubetopo.repository;

import com.dclingcloud.kubetopo.entity.BackendEndpointRelationPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BackendEndpointRelationRepository extends JpaRepository<BackendEndpointRelationPO, Long> {
}
