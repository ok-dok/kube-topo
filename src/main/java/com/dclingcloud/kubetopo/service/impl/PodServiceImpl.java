package com.dclingcloud.kubetopo.service.impl;

import com.dclingcloud.kubetopo.entity.PodPO;
import com.dclingcloud.kubetopo.repository.PodRepository;
import com.dclingcloud.kubetopo.service.NodeService;
import com.dclingcloud.kubetopo.service.PodService;
import com.dclingcloud.kubetopo.util.K8sServiceException;
import io.kubernetes.client.openapi.models.V1Pod;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.transaction.Transactional;
import java.util.Optional;

@Service
@Slf4j
public class PodServiceImpl implements PodService {
    @Resource
    private PodRepository podRepository;
    @Resource
    private NodeService nodeService;

    @Transactional
    @Override
    public void saveOrUpdate(V1Pod pod, String status) throws K8sServiceException {
        PodPO podPO = podRepository.findById(pod.getMetadata().getUid())
                .orElse(PodPO.builder().uid(pod.getMetadata().getUid()).build());
        podPO.setName(pod.getMetadata().getName())
                .setIp(pod.getStatus().getPodIP())
                .setContainerId(Optional.ofNullable(pod.getStatus().getContainerStatuses())
                        .map(l -> l.get(0)).map(s -> s.getContainerID()).orElse(null))
                .setStatus(status)
                .setGmtCreate(pod.getMetadata().getCreationTimestamp().toLocalDateTime());
        if (StringUtils.isBlank(podPO.getNodeName())) {
            String nodeName = nodeService.getNameByHostIP(pod.getStatus().getHostIP());
            podPO.setNodeName(nodeName);
        }
        save(podPO);
    }

    @Override
    public void save(PodPO podPO) throws K8sServiceException {
        try {
            podRepository.save(podPO);
        } catch (Exception e) {
            log.error("Error: save {} failed. {}", PodPO.class.getName(), podPO, e);
            throw new K8sServiceException("Unable to save " + PodPO.class.getSimpleName(), e);
        }
    }

    @Transactional
    @Override
    public void delete(V1Pod pod) throws K8sServiceException {
        String uid = pod.getMetadata().getUid();
        try {
            podRepository.updateStatusByUid(uid, "DELETED");
        } catch (Exception e) {
            log.error("Error: update {}'s status to 'DELETED' failed. uid={}", PodPO.class.getName(), uid, e);
            throw new K8sServiceException("Unable to delete {}" + PodPO.class.getSimpleName(), e);
        }
    }
}
