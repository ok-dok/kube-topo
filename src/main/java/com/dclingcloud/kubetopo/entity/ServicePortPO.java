package com.dclingcloud.kubetopo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.persistence.*;
import java.util.List;
import java.util.Objects;

@NoArgsConstructor
@AllArgsConstructor
@Data
@SuperBuilder
@Entity
@Table(name = "service_port", schema = "k8s")
public class ServicePortPO extends BasePO {
    @Id
    @Column(name = "uid", nullable = false)
//    @GenericGenerator(name = "uuid-gen", strategy = "uuid2")
//    @GeneratedValue(generator = "uuid-gen")
    private String uid;
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

    @OneToMany(cascade = CascadeType.DETACH, mappedBy = "backend")
    private List<PathRulePO> ingressPathRules;

    @OneToMany(cascade = CascadeType.DETACH, mappedBy = "servicePort")
    private List<PodPortPO> podPorts;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServicePortPO that = (ServicePortPO) o;
        return this.uid.equals(that.uid) || port.equals(that.port) && service.equals(that.service);
    }

    @Override
    public int hashCode() {
        return Objects.hash(port, service);
    }
}
