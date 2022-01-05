package com.dclingcloud.kubetopo.service.impl;

import com.dclingcloud.kubetopo.beanmapper.PodPOMapper;
import com.dclingcloud.kubetopo.entity.NodePO;
import com.dclingcloud.kubetopo.entity.PodPO;
import com.dclingcloud.kubetopo.repository.PodRepository;
import com.dclingcloud.kubetopo.service.NodeService;
import com.dclingcloud.kubetopo.service.PodService;
import com.dclingcloud.kubetopo.util.K8sServiceException;
import io.kubernetes.client.openapi.models.V1ContainerStatus;
import io.kubernetes.client.openapi.models.V1Pod;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
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
        String state = "NotReady";
        List<V1ContainerStatus> containerStatuses = pod.getStatus().getContainerStatuses();
        StringBuilder containerIds = new StringBuilder();
        if (CollectionUtils.isNotEmpty(containerStatuses)) {
            state = "Ready";
            for (V1ContainerStatus containerStatus : containerStatuses) {
                if (!containerStatus.getReady()) {
                    state = "NotReady";
                }
                if (containerStatus.getState().getRunning() != null) {
                    containerIds.append(containerStatus.getContainerID()).append(",");
                }
            }
        }
        int lastCommaIndex = containerIds.lastIndexOf(",");
        if (lastCommaIndex > 0) {
            containerIds.deleteCharAt(lastCommaIndex);
        }
        PodPO podPO = PodPO.builder()
                .uid(pod.getMetadata().getUid())
                .gmtCreate(pod.getMetadata().getCreationTimestamp().toLocalDateTime())
                .name(pod.getMetadata().getName())
                .namespace(pod.getMetadata().getNamespace())
                .status(status)
                .state(state)
                .ip(pod.getStatus().getHostIP())
                .hostname(pod.getSpec().getHostname())
                .subdomain(pod.getSpec().getSubdomain())
                .containerIds(containerIds.toString())
                .build();
        if (podPO.getNode() == null) {
            Optional<NodePO> nodeOpt = nodeService.findByHostIP(pod.getStatus().getHostIP());
            podPO.setNode(nodeOpt.orElse(null));
        }
        saveOrUpdate(podPO);
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
