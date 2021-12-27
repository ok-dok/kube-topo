package com.dclingcloud.kubetopo.service.impl;

import com.dclingcloud.kubetopo.entity.NodePO;
import com.dclingcloud.kubetopo.repository.NodeRepository;
import com.dclingcloud.kubetopo.service.NodeService;
import com.dclingcloud.kubetopo.util.K8sServiceException;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1NodeAddress;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.persistence.PersistenceException;
import javax.transaction.Transactional;
import java.util.Optional;

@Service
@Slf4j
public class NodeServiceImpl implements NodeService {

    @Resource
    private NodeRepository nodeRepository;

    @Transactional
    @Override
    public void saveOrUpdate(V1Node node, String status) throws K8sServiceException {
        NodePO nodePO = nodeRepository.findById(node.getMetadata().getUid())
                .orElse(NodePO.builder()
                        .uid(node.getMetadata().getUid())
                        .build());
        nodePO.setName(node.getMetadata().getName())
                .setPodCIDR(node.getSpec().getPodCIDR())
                .setStatus(status)
                .setGmtCreate(node.getMetadata().getCreationTimestamp().toLocalDateTime());
        if (CollectionUtils.isNotEmpty(node.getStatus().getAddresses())) {
            for (V1NodeAddress addr : node.getStatus().getAddresses()) {
                if ("Hostname".equals(addr.getType())) {
                    nodePO.setHostname(addr.getAddress());
                } else if ("InternalIP".equals(addr.getType())) {
                    nodePO.setInternalIP(addr.getAddress());
                }
            }
        }
        try {
            nodeRepository.save(nodePO);
        } catch (PersistenceException e) {
            log.error("Error: save {} failed. {}", NodePO.class.getName(), nodePO, e);
            throw new K8sServiceException("Unable to save node", e);
        }

    }

    @Transactional
    @Override
    public void delete(V1Node node) throws K8sServiceException {
        String uid = node.getMetadata().getUid();
        try {
            nodeRepository.updateStatusByUid(uid, "DELETED");
        } catch (PersistenceException e) {
            log.error("Error: update {}'s status to 'DELETED' failed. uid={}", NodePO.class.getName(), uid, e);
            throw new K8sServiceException("Unable to delete node", e);
        }
    }

    @Override
    public Optional<NodePO> findByHostIP(String hostIP) {
        try {
            return nodeRepository.getByInternalIP(hostIP);
        } catch (Exception e) {
            log.error("Error: get {} by hostIP '{}' failed.", NodePO.class.getName(), hostIP, e);
            throw new K8sServiceException("Unable to get node", e);
        }
    }

}
