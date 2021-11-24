package com.dclingcloud.kubetopo.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ServiceEndpointMeta {
    private String name;
    private String protocol;
    private Integer port;
    private Integer nodePort;
    private IngressInfo ingress;
    private List<PodEndpointMeta> endpoints;
}
