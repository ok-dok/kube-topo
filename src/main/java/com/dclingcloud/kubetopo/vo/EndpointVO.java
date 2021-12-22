package com.dclingcloud.kubetopo.vo;

import lombok.*;

import java.io.Serializable;
import java.util.Collection;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class EndpointVO implements Serializable {
    private String uid;
    private String name;
    private Integer port;
    private String protocol;
    private String appProtocol;
    private Collection<String> backendUids;
    private String podUid;
}
