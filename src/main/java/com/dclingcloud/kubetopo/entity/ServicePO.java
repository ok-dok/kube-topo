package com.dclingcloud.kubetopo.entity;

import lombok.Builder;
import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

@Data
@Builder
@Entity
@Table(name = "service")
public class ServicePO implements Serializable {
    @Id
    @Column(name = "uid", nullable = false)
    private String uid;
    @Column
    private String name;
    @Column
    private String namespace;
    @Column
    private String type;
    @Column
    private String clusterIP;
    @Column
    private String externalIPs;
    @Column(name = "load_balancer_ip")
    private String loadBalancerIP;
    @Column
    private String externalName;

    @OneToMany(cascade = CascadeType.DETACH, mappedBy = "service")
    private List<ServicePortPO> ports;
}
