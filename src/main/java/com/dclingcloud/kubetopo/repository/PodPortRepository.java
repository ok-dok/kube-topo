package com.dclingcloud.kubetopo.repository;

import com.dclingcloud.kubetopo.entity.PodPortPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PodPortRepository extends JpaRepository<PodPortPO, String> {
}
