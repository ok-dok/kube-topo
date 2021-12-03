package com.dclingcloud.kubetopo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@Entity
@Table(name = "service", schema = "k8s")
public class ServicePO extends BasePO {
    @Id
    @Column(name = "uid", nullable = false, length = 36)
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

    @OneToMany(cascade = CascadeType.DETACH, mappedBy = "service", fetch = FetchType.EAGER)
    private List<ServicePortPO> ports;
}
