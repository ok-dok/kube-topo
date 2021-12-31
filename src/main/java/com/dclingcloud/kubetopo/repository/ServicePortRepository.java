package com.dclingcloud.kubetopo.repository;

import com.dclingcloud.kubetopo.entity.ServicePO;
import com.dclingcloud.kubetopo.entity.ServicePortPO;
import io.kubernetes.client.custom.IntOrString;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ServicePortRepository extends JpaRepository<ServicePortPO, String> {
    List<ServicePortPO> findAllByService(ServicePO svc);

    @Query("select distinct p from ServicePortPO p join ServicePO s on s.uid = p.service where s.namespace = :namespace and s.name = :serviceName and p.name = :portName and s.status <> 'DELETED' and p.status <> 'DELETED'")
    Optional<ServicePortPO> findByNamespacedServiceNameAndPortName(String namespace, String serviceName, String portName);

    @Query("select distinct p from ServicePortPO p join ServicePO s on s.uid = p.service where s.namespace = :namespace and s.name = :serviceName and p.port = :portNumber and (p.protocol = 'TCP' or p.protocol is null ) and s.status <> 'DELETED' and p.status <> 'DELETED'")
    Optional<ServicePortPO> findByNamespacedServiceNameAndTCPPortNumber(String namespace, String serviceName, Integer portNumber);

    Optional<ServicePortPO> findByServiceAndTargetPortAndProtocol(ServicePO servicePO, IntOrString targetPort, String protocol);

    @Modifying
    @Query("update ServicePortPO set status = :status, gmtModified = current_timestamp where service = :serviceUid")
    void updateStatusByServiceUid(String serviceUid, String status);

    @Modifying
    @Query("update ServicePortPO set status = 'DELETED', gmtModified = current_timestamp where status <> 'DELETED'")
    void updateAllWithDeletedStatus();

    void deleteAllByStatusAndGmtModifiedBefore(String status, LocalDateTime dateTime);

    @Query("select distinct targetPort from ServicePortPO where uid = :uid and status <> 'DELETED'")
    Optional<IntOrString> getTargetPortByUid(String uid);

    @Query("select uid from ServicePortPO where service = :serviceUid")
    List<String> findAllUidByServiceUid(String serviceUid);
}
