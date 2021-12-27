package com.dclingcloud.kubetopo.repository;

import com.dclingcloud.kubetopo.entity.NodePO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NodeRepository extends JpaRepository<NodePO, String> {
    @Modifying
    @Query("update NodePO set status = :status where uid = :uid")
    void updateStatusByUid(String uid, String status);

    @Query("from NodePO where internalIP = :ip and status <> 'DELETED'")
    Optional<NodePO> getByInternalIP(String ip);

    @Modifying
    @Query("update NodePO set status = 'DELETED', gmtModified = current_timestamp where status <> 'DELETED'")
    void updateAllWithDeletedStatus();
}
