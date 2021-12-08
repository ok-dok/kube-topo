package com.dclingcloud.kubetopo.service;

import io.kubernetes.client.openapi.models.V1Node;

public interface NodeService {
    void save(V1Node node, String status);

    /**
     * it's not a real delete
     *
     * @param node
     */
    void delete(V1Node node);
}
