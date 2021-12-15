package com.dclingcloud.kubetopo.repository;

import com.dclingcloud.kubetopo.entity.IngressPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface IngressRepository extends JpaRepository<IngressPO, String> {
    public List<IngressPO> findAllByStatusIn(Set<String> status);

    @Modifying
    @Query("update IngressPO set status = :status where uid = :uid")
    void updateStatusByUid(String uid, String status);

    @Modifying
    @Query("update IngressPO set status = 'DELETED', gmtModified = current_timestamp where status <> 'DELETED'")
    void updateAllWithDeletedStatus();
}
