package com.dclingcloud.kubetopo.model;

import lombok.Builder;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.util.List;

@Data
@Builder
public class PodEndpoint {
    @Id
    private String uid;
    // Pod名称
    @Column
    private String name;
    // Pod所在节点名称
    @Column
    private String nodeName;
    // Pod的hostname，在使用dns的情况下，可通过“hostname.服务名.命名空间.svc.集群名称”的方式来访问
    // 比如无头服务（Headless Service）会为每个Pod（通常是StatefulSet）保持一个不变的Pod Name作为Hostname，
    // Pod之间就可以通过dnsName来直接相互访问，而不必通过ClusterIP
    @Column
    private String hostname;
    // Pod IP
    @Column
    private String ip;
    @Transient
    private List<PodPort> ports;
}
