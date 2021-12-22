package com.dclingcloud.kubetopo.service.impl;

import com.dclingcloud.kubetopo.beanmapper.PodPOMapper;
import com.dclingcloud.kubetopo.entity.PodPO;
import com.dclingcloud.kubetopo.repository.PodRepository;
import com.dclingcloud.kubetopo.service.NodeService;
import com.dclingcloud.kubetopo.service.PodService;
import com.dclingcloud.kubetopo.util.K8sServiceException;
import io.kubernetes.client.openapi.models.V1Pod;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class PodServiceImpl implements PodService {
    @Resource
    private PodRepository podRepository;
    @Resource
    private NodeService nodeService;
    @Resource
    private PodPOMapper podPOMapper;

    @Transactional
    @Override
    public void saveOrUpdate(PodPO podPO) {
        Optional<PodPO> persistPodOpt = podRepository.findById(podPO.getUid());
        if (persistPodOpt.isPresent()) {
            PodPO persistPod = persistPodOpt.get();
            podPOMapper.updatePropertiesIgnoresNull(persistPod, podPO);
            save(persistPod);
        } else {
            save(podPO);
        }
    }

    @Transactional
    @Override
    public void saveOrUpdate(V1Pod pod, String status) throws K8sServiceException {
        PodPO podPO = podRepository.findById(pod.getMetadata().getUid()).orElse(PodPO.builder().uid(pod.getMetadata().getUid()).build());
        podPO.setName(pod.getMetadata().getName())
                .setNamespace(pod.getMetadata().getNamespace())
                .setIp(pod.getStatus().getPodIP())
                .setContainerId(Optional.ofNullable(pod.getStatus().getContainerStatuses()).map(l -> l.get(0)).map(s -> s.getContainerID()).orElse(null))
                .setStatus(status)
                .setGmtCreate(pod.getMetadata().getCreationTimestamp().toLocalDateTime());
        if (StringUtils.isBlank(podPO.getNodeName())) {
            String nodeName = nodeService.getNameByHostIP(pod.getStatus().getHostIP());
            podPO.setNodeName(nodeName);
        }
        save(podPO);
    }

    @Override
    public Optional<PodPO> findByUid(String uid) {
        try {
            return podRepository.findById(uid);
        } catch (Exception e) {
            log.error("Error: could not find the {} with uid={}", PodPO.class.getSimpleName(), uid, e);
            throw new K8sServiceException("Unable to find " + PodPO.class.getSimpleName() + " by uid", e);
        }
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

    @Override
    public void saveAll(List<PodPO> podPOList) throws K8sServiceException {
        if (CollectionUtils.isNotEmpty(podPOList)) {
            podPOList.forEach(this::save);
        }
    }


}
