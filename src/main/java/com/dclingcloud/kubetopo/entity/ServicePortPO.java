package com.dclingcloud.kubetopo.entity;

import lombok.Builder;
import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

@Data
@Builder
@Entity
@Table(name = "service_port")
public class ServicePortPO implements Serializable {
    @Id
    @Column(name = "uid", nullable = false)
    private Long uid;
    @Column
    private String name;
    @Column
    private String protocol;
    @Column
    private String appProtocol;
    @Column
    private Integer port;
    @Column
    private Integer nodePort;
    @Column
    private Integer targetPort;

    @ManyToOne(cascade = CascadeType.DETACH)
    @JoinColumn(name = "service_uid")
    private ServicePO service;

    @OneToOne(cascade = CascadeType.DETACH)
    @JoinColumn(name = "path_rule_uid")
    private PathRulePO ingressPathRule;

    @OneToMany(cascade = CascadeType.DETACH, mappedBy = "servicePort")
    private List<PodPortPO> podPorts;
}
