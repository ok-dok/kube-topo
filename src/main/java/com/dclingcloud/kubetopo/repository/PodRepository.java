package com.dclingcloud.kubetopo.repository;

import com.dclingcloud.kubetopo.entity.PodPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface PodRepository extends JpaRepository<PodPO, String> {
    @Modifying
    @Query("update PodPO set status = :status where uid = :uid")
    void updateStatusByUid(String uid, String status);

    @Modifying
    @Query("update PodPO set status = 'DELETED', gmtModified = current_timestamp where status <> 'DELETED'")
    void updateAllWithDeletedStatus();

    @Modifying
    void deleteAllByStatusAndGmtModifiedBefore(String status, LocalDateTime dateTime);
}
