package com.dclingcloud.kubetopo.repository;

import com.dclingcloud.kubetopo.entity.ServicePO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ServiceRepository extends JpaRepository<ServicePO, String> {
    @Modifying
    @Query("update ServicePO set status = :status where uid = :uid")
    void updateStatusByUid(String uid, String status);

    Optional<ServicePO> findByNameAndStatusNot(String name, String status);

    @Modifying
    @Query("update ServicePO set status = 'DELETED', gmtModified = current_timestamp where status <> 'DELETED'")
    void updateAllWithDeletedStatus();

    @Modifying
    void deleteAllByStatusAndGmtModifiedBefore(String status, LocalDateTime dateTime);

    Optional<ServicePO> findByNamespaceAndNameAndStatusNot(String name, String deleted, String statusNot);
}
