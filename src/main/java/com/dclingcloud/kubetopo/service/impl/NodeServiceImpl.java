package com.dclingcloud.kubetopo.service.impl;

import com.dclingcloud.kubetopo.entity.NodePO;
import com.dclingcloud.kubetopo.repository.NodeRepository;
import com.dclingcloud.kubetopo.service.NodeService;
import com.dclingcloud.kubetopo.util.K8sServiceException;
import io.kubernetes.client.openapi.models.V1Node;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.persistence.PersistenceException;
import javax.transaction.Transactional;

@Service
@Slf4j
public class NodeServiceImpl implements NodeService {

    @Resource
    private NodeRepository nodeRepository;

    @Transactional
    @Override
    public void save(V1Node node, String status) throws K8sServiceException {
        NodePO nodePO = NodePO.builder()
                .uid(node.getMetadata().getUid())
                .name(node.getMetadata().getName())
                .podCIDR(node.getSpec().getPodCIDR())
                .status(status)
                .gmtCreate(node.getMetadata().getCreationTimestamp().toLocalDateTime())
                .build();
        node.getStatus().getAddresses().forEach(addr -> {
            if ("Hostname".equals(addr.getType())) {
                nodePO.setHostname(addr.getAddress());
            } else if ("InternalIP".equals(addr.getType())) {
                nodePO.setInternalIP(addr.getAddress());
            }
        });
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

}
