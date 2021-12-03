package com.dclingcloud.kubetopo.repository;

import com.dclingcloud.kubetopo.entity.IngressPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface IngressRepository extends JpaRepository<IngressPO, String> {
    public List<IngressPO> findAllByStatusIn(Set<String> status);
}
