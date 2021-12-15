package com.dclingcloud.kubetopo.service;

import com.dclingcloud.kubetopo.util.K8sServiceException;
import com.dclingcloud.kubetopo.vo.TopologyVO;
import io.kubernetes.client.openapi.ApiException;

import javax.transaction.Transactional;

public interface TopologyService {
    TopologyVO getTopology();

    @Transactional
    void loadResourcesTopology() throws ApiException, K8sServiceException;

    void updateAllResourcesWithDeletedStatus() throws K8sServiceException;
}
