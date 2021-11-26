package com.dclingcloud.kubetopo.repository;

import com.dclingcloud.kubetopo.entity.ServicePortPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServicePortRepository extends JpaRepository<ServicePortPO, String> {
}
