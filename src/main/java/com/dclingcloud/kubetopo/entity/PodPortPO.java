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
import java.util.HashSet;
import java.util.Objects;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Accessors(chain = true)
@SuperBuilder
@Entity
@Table(name = "pod_port", schema = "k8s")
public class PodPortPO extends BasePO {

    @Id
    @Column(name = "uid", nullable = false)
    private String uid;
    @Column
    private String name;
    @Column
    private Integer port;
    @Column
    private String protocol;
    @Column
    private String appProtocol;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "podPort")
    @NotFound(action = NotFoundAction.IGNORE)
    private Collection<BackendEndpointRelationPO> backendEndpointRelations;

    @ManyToOne(cascade = CascadeType.DETACH)
    @NotFound(action = NotFoundAction.IGNORE)
    @JoinColumn(name = "podUid")
    private PodPO pod;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PodPortPO podPortPO = (PodPortPO) o;
        return uid.equals(podPortPO.uid) || Objects.equals(port, podPortPO.port) && Objects.equals(protocol, podPortPO.protocol) && pod.equals(podPortPO.pod);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), uid);
    }
}
