package com.dclingcloud.kubetopo.entity;

import lombok.Builder;
import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

@Data
@Builder
@Entity
@Table(name = "pod_port")
public class PodPortPO implements Serializable {
    @Id
    @Column(name = "uid", nullable = false)
    private String uid;

    @Column
    private String name;
    @Column
    private String port;
    @Column
    private String protocol;
    @Column
    private String appProtocol;
    @ManyToOne(cascade = CascadeType.DETACH)
    @JoinColumn(name = "service_port_uid")
    private ServicePortPO servicePort;

    @ManyToMany(cascade = CascadeType.DETACH)
    @JoinTable(name = "pod_port_rules", joinColumns = {
            @JoinColumn(name = "pod_uid", referencedColumnName = "uid")
    }, inverseJoinColumns = {
            @JoinColumn(name = "pod_port_uid", referencedColumnName = "uid")
    })
    private List<PodPO> pods;
}
