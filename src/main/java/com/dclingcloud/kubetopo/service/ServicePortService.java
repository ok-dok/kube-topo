package com.dclingcloud.kubetopo.service;

import com.dclingcloud.kubetopo.entity.ServicePortPO;
import com.dclingcloud.kubetopo.util.K8sServiceException;

import java.util.Collection;

public interface ServicePortService {
    void saveOrUpdate(ServicePortPO servicePortPO) throws K8sServiceException;

    void saveAll(Collection<ServicePortPO> servicePorts) throws K8sServiceException;
}
