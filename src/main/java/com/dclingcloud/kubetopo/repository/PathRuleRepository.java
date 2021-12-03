package com.dclingcloud.kubetopo.repository;

import com.dclingcloud.kubetopo.entity.IngressPO;
import com.dclingcloud.kubetopo.entity.PathRulePO;
import com.dclingcloud.kubetopo.entity.ServicePortPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PathRuleRepository extends JpaRepository<PathRulePO, String> {
    String getUidByBackend(ServicePortPO backend);

    List<PathRulePO> findAllByIngress(IngressPO ingressPO);

}
