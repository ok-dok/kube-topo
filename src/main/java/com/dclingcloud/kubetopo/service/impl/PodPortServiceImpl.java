package com.dclingcloud.kubetopo.service.impl;

import com.dclingcloud.kubetopo.entity.PodPortPO;
import com.dclingcloud.kubetopo.repository.PodPortRepository;
import com.dclingcloud.kubetopo.service.PodPortService;
import com.dclingcloud.kubetopo.util.K8sServiceException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.transaction.Transactional;
import java.util.Collection;

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
    public void deleteAllByEndpointsUid(String epUid) throws K8sServiceException {
        try {
            podPortRepository.updateStatusByEpUid(epUid, "DELETED");
        } catch (Exception e) {
            log.error("Error: update {}'s status to 'DELETED' failed. epUid={}", PodPortPO.class.getName(), epUid, e);
            throw new K8sServiceException("Unable to delete " + PodPortPO.class.getSimpleName() + " by epUid", e);
        }
    }
}
