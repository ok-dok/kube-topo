package com.dclingcloud.kubetopo.service;

import com.dclingcloud.kubetopo.vo.TopologyVO;
import io.kubernetes.client.openapi.ApiException;

import javax.transaction.Transactional;

public interface TopologyService {
    TopologyVO getTopology();

    @Transactional
    void loadResources() throws ApiException;
}
