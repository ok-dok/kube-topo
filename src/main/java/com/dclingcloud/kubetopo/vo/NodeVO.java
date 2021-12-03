package com.dclingcloud.kubetopo.vo;

import lombok.*;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
public class NodeVO implements Serializable {
    private String uid;
    private String name;
    private String hostname;
    private String internalIP;
    private String podCIDR;
}
