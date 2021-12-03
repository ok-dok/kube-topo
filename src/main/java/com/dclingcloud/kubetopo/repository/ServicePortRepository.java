package com.dclingcloud.kubetopo.repository;

import com.dclingcloud.kubetopo.entity.PathRulePO;
import com.dclingcloud.kubetopo.entity.ServicePO;
import com.dclingcloud.kubetopo.entity.ServicePortPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServicePortRepository extends JpaRepository<ServicePortPO, String> {
    List<ServicePortPO> findAllByService(ServicePO svc);

}
