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
@Table(name = "ingress", schema = "k8s")
public class IngressPO extends BasePO {
    @Id
    @Column(name = "uid", nullable = false, length = 36)
    private String uid;
    @Column
    private String name;
    @Column
    private String namespace;
    @Column
    private String className;
    @Column
    private String loadBalancerHosts;
    @OneToMany(cascade = CascadeType.DETACH, mappedBy = "ingress")
    @NotFound(action = NotFoundAction.IGNORE)
    private Collection<PathRulePO> pathRules;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IngressPO ingressPO = (IngressPO) o;
        return uid.equals(ingressPO.uid) && name.equals(ingressPO.name) && namespace.equals(ingressPO.namespace);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), uid, name, namespace);
    }
}
