package com.dclingcloud.kubetopo.vo;

import lombok.*;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class IngressPathRuleVO implements Serializable {
    private String uid;
    private String host;
    private String path;
    private String pathType;
    private String targetBackendUid;
    private String ingressUid;
}
