package com.dclingcloud.kubetopo.repository;

import com.dclingcloud.kubetopo.entity.PathRulePO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PathRuleRepository extends JpaRepository<PathRulePO, String> {
}
