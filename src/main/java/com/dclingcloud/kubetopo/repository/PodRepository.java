package com.dclingcloud.kubetopo.repository;

import com.dclingcloud.kubetopo.entity.PodPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PodRepository extends JpaRepository<PodPO, String> {
}
