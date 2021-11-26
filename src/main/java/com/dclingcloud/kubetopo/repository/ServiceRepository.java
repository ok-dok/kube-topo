package com.dclingcloud.kubetopo.repository;

import com.dclingcloud.kubetopo.entity.ServicePO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceRepository extends JpaRepository<ServicePO, String> {
}
