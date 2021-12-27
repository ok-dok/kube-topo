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
import java.util.Objects;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Accessors(chain = true)
@SuperBuilder
@Entity
@Table(name = "pod", schema = "k8s")
public class PodPO extends BasePO {
    @Id
    @Column(name = "uid", nullable = false, length = 36)
    private String uid;
    @Column
    private String name;
    @Column
    private String namespace;

    @ManyToOne(cascade = CascadeType.DETACH)
    @JoinColumn(name = "nodeUid")
    private NodePO node;

    @Column
    private String hostname;
    @Column
    private String subdomain;
    @Column
    private String ip;
    @Column
    private String containerIds;
    @OneToMany(cascade = CascadeType.DETACH, mappedBy = "pod")
    @NotFound(action = NotFoundAction.IGNORE)
    private Collection<PodPortPO> ports;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PodPO podPO = (PodPO) o;
        return uid.equals(podPO.uid) && name.equals(podPO.name) && namespace.equals(podPO.namespace);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), uid, name, namespace);
    }
}
