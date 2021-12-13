package com.dclingcloud.kubetopo.service;

import com.dclingcloud.kubetopo.entity.PodPO;
import com.dclingcloud.kubetopo.util.K8sServiceException;

public interface PodPortService {
    void save(PodPO podPO) throws K8sServiceException;
}
