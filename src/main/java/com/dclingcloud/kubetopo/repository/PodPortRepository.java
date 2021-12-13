package com.dclingcloud.kubetopo.repository;

import com.dclingcloud.kubetopo.entity.PodPO;
import com.dclingcloud.kubetopo.entity.PodPortPO;
import com.dclingcloud.kubetopo.entity.ServicePortPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PodPortRepository extends JpaRepository<PodPortPO, String> {
    @Query("select uid from PodPortPO where servicePort = :servicePort")
    List<String> findAllUidsByServicePort(ServicePortPO servicePort);

    List<PodPortPO> findAllByPod(PodPO pod);

    @Query("update PodPortPO set status = :status, gmtModified = current_timestamp where epUid = :epUid")
    void updateStatusByEpUid(String epUid, String status);
}
