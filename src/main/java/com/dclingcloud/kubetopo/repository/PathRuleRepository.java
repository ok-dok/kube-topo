package com.dclingcloud.kubetopo.repository;

import com.dclingcloud.kubetopo.entity.IngressPO;
import com.dclingcloud.kubetopo.entity.PathRulePO;
import com.dclingcloud.kubetopo.entity.ServicePortPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PathRuleRepository extends JpaRepository<PathRulePO, String> {

    List<PathRulePO> findAllByIngress(IngressPO ingressPO);

    @Query("select uid from PathRulePO where backend = :servicePort")
    List<String> findAllUidsByServicePort(ServicePortPO servicePort);

    @Query("update PathRulePO set status = :status where uid = :uid")
    void updateStatusByUid(String uid, String status);
}
