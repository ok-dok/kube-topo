package com.dclingcloud.kubetopo.service;

import com.dclingcloud.kubetopo.entity.ServicePortPO;
import com.dclingcloud.kubetopo.util.K8sServiceException;
import io.kubernetes.client.custom.IntOrString;
import io.kubernetes.client.openapi.models.V1ServiceBackendPort;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ServicePortService {
    void saveOrUpdate(ServicePortPO servicePortPO) throws K8sServiceException;

    void saveAll(Collection<ServicePortPO> servicePorts) throws K8sServiceException;

    Optional<ServicePortPO> findByNamespacedServiceNameAndPort(String namespace, String serviceName, V1ServiceBackendPort port);


    /**
     * it's not a real delete action
     *
     * @param serviceUid
     */
    void deleteAllByServiceUid(String serviceUid);

    Optional<IntOrString> getTargetPort(@NonNull String servicePortUid);

    Optional<ServicePortPO> findByServiceUidAndTargetPortAndProtocol(String serviceUid, IntOrString targetPort, String protocol);

    @Query("select uid from ServicePortPO where service = :serviceUid")
    List<String> findAllUidByServiceUid(String serviceUid);
}
