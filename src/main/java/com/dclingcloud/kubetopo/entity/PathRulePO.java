package com.dclingcloud.kubetopo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Objects;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@Entity
@Table(name = "path_rule", schema = "k8s")
public class PathRulePO extends BasePO {
    @Id
    @Column(name = "uid", nullable = false)
//    @GenericGenerator(name = "uuid-gen", strategy = "uuid2")
//    @GeneratedValue(generator = "uuid-gen")
    private String uid;
    @ManyToOne(cascade = CascadeType.DETACH)
    @JoinColumn(name = "ingress_uid")
    private IngressPO ingress;
    @Column
    private String host;
    @Column
    private String path;
    @Column
    private String pathType;

    @OneToOne(cascade = CascadeType.DETACH, mappedBy = "ingressPathRule")
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
        return Objects.hash(ingress, host, path, pathType);
    }
}
