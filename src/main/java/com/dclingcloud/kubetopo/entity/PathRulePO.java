package com.dclingcloud.kubetopo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import javax.persistence.*;
import java.util.Objects;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Accessors(chain = true)
@SuperBuilder
@Entity
@Table(name = "path_rule", schema = "k8s")
public class PathRulePO extends BasePO {
    @Id
    @Column(name = "uid", nullable = false)
    private String uid;
    @ManyToOne(cascade = CascadeType.DETACH)
    @JoinColumn(name = "ingress_uid")
    @NotFound(action = NotFoundAction.IGNORE)
    private IngressPO ingress;
    @Column
    private String host;
    @Column
    private String path;
    @Column
    private String pathType;

    @ManyToOne(cascade = CascadeType.DETACH)
    @JoinColumn(name = "service_port_uid")
    @NotFound(action = NotFoundAction.IGNORE)
    private ServicePortPO backend;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PathRulePO that = (PathRulePO) o;
        return this.uid.equals(that.uid) || ingress.equals(that.ingress) && host.equals(that.host) && path.equals(that.path) && pathType.equals(that.pathType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uid, ingress, host, path, pathType);
    }
}
