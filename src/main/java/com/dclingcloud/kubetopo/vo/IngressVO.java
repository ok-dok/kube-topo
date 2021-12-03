package com.dclingcloud.kubetopo.vo;

import lombok.*;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class IngressVO implements Serializable {
    private String uid;
    private String name;
    private String namespace;
    private String className;
    private String loadBalancerHosts;
    private List<IngressPathRuleVO> pathRules;
}
