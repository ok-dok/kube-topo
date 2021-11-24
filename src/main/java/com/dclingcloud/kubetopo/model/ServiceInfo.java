package com.dclingcloud.kubetopo.model;

import io.kubernetes.client.proto.V1;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ServiceInfo {
    private String name;
    private String namespace;
    private String type;
    private String clusterIP;
    private List<String> externalIPs;
    private String loadbalancerIP;
    private String externalName;
    private List<ServiceEndpointMeta> endpoints;
    private V1.LoadBalancerIngress loadBalancerIngress;
}
