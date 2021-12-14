package com.dclingcloud.kubetopo.repository;

import com.dclingcloud.kubetopo.entity.ServicePO;
import com.dclingcloud.kubetopo.entity.ServicePortPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServicePortRepository extends JpaRepository<ServicePortPO, String> {
    List<ServicePortPO> findAllByService(ServicePO svc);

    @Query("select distinct p from ServicePortPO p join ServicePO s on s.uid = p.service where s.name = :serviceName and p.name = :portName and s.status <> 'DELETED' and p.status <> 'DELETED'")
    Optional<ServicePortPO> findByServiceNameAndPortName(String serviceName, String portName);

    @Query("select distinct p from ServicePortPO p join ServicePO s on s.uid = p.service where s.name = :serviceName and p.port = :portNumber and (p.protocol = 'TCP' or p.protocol is null ) and s.status <> 'DELETED' and p.status <> 'DELETED'")
    Optional<ServicePortPO> findByServiceNameAndTCPPortNumber(String serviceName, Integer portNumber);
}
