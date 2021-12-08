package com.dclingcloud.kubetopo.repository;

import com.dclingcloud.kubetopo.entity.PodPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PodRepository extends JpaRepository<PodPO, String> {
    @Query("update PodPO set status = :status where uid = :uid")
    void updateStatusByUid(String uid, String status);
}
