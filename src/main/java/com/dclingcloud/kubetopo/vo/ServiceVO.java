package com.dclingcloud.kubetopo.vo;


import lombok.*;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ServiceVO {
    private String uid;
    private String name;
    private String namespace;
    private String type;
    private String clusterIP;
    private String externalIPs;
    private String loadBalancerIP;
    private String externalName;
    private List<BackendVO> backends;
}
