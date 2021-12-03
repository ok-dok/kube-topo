package com.dclingcloud.kubetopo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TopologyVO implements Serializable {
    private List<IngressVO> ingresses;
    private List<ServiceVO> services;
    private List<PodVO> pods;
    private List<NodeVO> nodes;
}
