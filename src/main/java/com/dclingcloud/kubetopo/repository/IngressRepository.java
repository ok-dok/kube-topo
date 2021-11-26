package com.dclingcloud.kubetopo.repository;

import com.dclingcloud.kubetopo.entity.IngressPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IngressRepository extends JpaRepository<IngressPO, String> {
}
