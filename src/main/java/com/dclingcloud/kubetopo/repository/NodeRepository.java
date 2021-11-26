package com.dclingcloud.kubetopo.repository;

import com.dclingcloud.kubetopo.entity.NodePO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NodeRepository extends JpaRepository<NodePO, String> {
}
