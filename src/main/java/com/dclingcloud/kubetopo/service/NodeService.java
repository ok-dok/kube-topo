package com.dclingcloud.kubetopo.service;

import com.dclingcloud.kubetopo.entity.NodePO;
import io.kubernetes.client.openapi.models.V1Node;

import java.util.Optional;

public interface NodeService {
    void saveOrUpdate(V1Node node, String status);

    /**
     * it's not a real delete
     *
     * @param node
     */
    void delete(V1Node node);

    Optional<NodePO> findByHostIP(String hostIP);
}
