package com.dclingcloud.kubetopo.repository;

import com.dclingcloud.kubetopo.entity.PodPO;
import com.dclingcloud.kubetopo.entity.PodPortPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PodPortRepository extends JpaRepository<PodPortPO, String> {

    List<PodPortPO> findAllByPod(PodPO pod);

    @Modifying
    @Query("update PodPortPO set status = :status, gmtModified = current_timestamp where pod = :podUid and status <> 'DELETED'")
    void updateStatusByPodUid(String podUid, String status);

    @Modifying
    @Query("update PodPortPO set status = 'DELETED', gmtModified = current_timestamp where status <> 'DELETED'")
    void updateAllWithDeletedStatus();

    @Modifying
    void deleteAllByStatusAndGmtModifiedBefore(String status, LocalDateTime dateTime);

    Optional<PodPortPO> findByPodAndNameAndProtocolAndStatusNot(PodPO pod, String name, String protocol, String status);

    Optional<PodPortPO> findByPodAndPortAndProtocolAndStatusNot(PodPO pod, Integer port, String protocol, String status);
}
