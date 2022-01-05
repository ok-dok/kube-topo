package com.dclingcloud.kubetopo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import javax.persistence.*;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Accessors(chain = true)
@SuperBuilder
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
    @Column(name = "external_ips")
    private String externalIPs;
    @Column(name = "load_balancer_ip")
    private String loadBalancerIP;
    @Column
    private String externalName;

    @OneToMany(cascade = CascadeType.DETACH, mappedBy = "service")
    @NotFound(action = NotFoundAction.IGNORE)
    private Collection<ServicePortPO> ports;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ServicePO servicePO = (ServicePO) o;
        return uid.equals(servicePO.uid) && name.equals(servicePO.name) && namespace.equals(servicePO.namespace);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), uid, name, namespace);
    }
}
