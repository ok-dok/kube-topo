package com.dclingcloud.kubetopo.vo;

import lombok.*;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class BackendVO implements Serializable {
    private String uid;
    private String name;
    private String protocol;
    private String appProtocol;
    private Integer port;
    private Integer nodePort;
    private Integer targetPort;
    private String serviceUid;
    private String ingressPathRuleUid;
    private List<String> endpointUids;
}
