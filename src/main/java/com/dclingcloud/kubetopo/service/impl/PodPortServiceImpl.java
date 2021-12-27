package com.dclingcloud.kubetopo.service.impl;

import com.dclingcloud.kubetopo.entity.PodPO;
import com.dclingcloud.kubetopo.entity.PodPortPO;
import com.dclingcloud.kubetopo.repository.PodPortRepository;
import com.dclingcloud.kubetopo.service.PodPortService;
import com.dclingcloud.kubetopo.util.K8sServiceException;
import com.dclingcloud.kubetopo.watch.EventType;
import io.kubernetes.client.custom.IntOrString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.persistence.PersistenceException;
import javax.transaction.Transactional;
import java.util.Collection;
import java.util.Optional;

@Service
@Slf4j
public class PodPortServiceImpl implements PodPortService {
    @Resource
    private PodPortRepository podPortRepository;

    @Transactional
    @Override
    public void save(PodPortPO podPortPO) throws K8sServiceException {
        try {
            podPortRepository.save(podPortPO);
        } catch (Exception e) {
            log.error("Error: save {} failed. {}", PodPortPO.class.getName(), podPortPO, e);
            throw new K8sServiceException("Unable to save " + PodPortPO.class.getSimpleName(), e);
        }
    }

    @Transactional
    @Override
    public void saveAll(Collection<PodPortPO> podPorts) throws K8sServiceException {
        if (CollectionUtils.isNotEmpty(podPorts)) {
            podPorts.forEach(this::save);
        }
    }

    @Override
    @Transactional
    public void deleteAllByPodUid(String podUid) throws K8sServiceException {
        try {
            podPortRepository.updateStatusByPodUid(podUid, "DELETED");
        } catch (Exception e) {
            log.error("Error: update {}'s status to 'DELETED' failed. podUid={}", PodPortPO.class.getName(), podUid, e);
            throw new K8sServiceException("Unable to delete " + PodPortPO.class.getSimpleName() + " by podUid", e);
        }
    }

    @Override
    public Optional<PodPortPO> find(String podUid, IntOrString targetPort, String protocol) {
        try {
            PodPO po = PodPO.builder().uid(podUid).build();
            if (targetPort.isInteger()) {
                return podPortRepository.findByPodAndPortAndProtocolAndStatusNot(po, targetPort.getIntValue(), protocol, EventType.DELETED);
            } else {
                return podPortRepository.findByPodAndNameAndProtocolAndStatusNot(po, targetPort.getStrValue(), protocol, EventType.DELETED);
            }
        } catch (PersistenceException e) {
            log.error("Error: find {} by podUid and targetPort and protocol failed, podUid={}, targetPort={}, protocol={}", PodPortPO.class.getName(), podUid, targetPort, protocol);
            throw new K8sServiceException("can not find " + PodPortPO.class.getSimpleName(), e);
        }
    }
}
