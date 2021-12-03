package com.dclingcloud.kubetopo.vo;

import lombok.*;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class PodVO implements Serializable {
    private String uid;
    private String name;
    private String namespace;
    private String nodeName;
    private String hostname;
    private String ip;
    private List<EndpointVO> endpoints;
}
