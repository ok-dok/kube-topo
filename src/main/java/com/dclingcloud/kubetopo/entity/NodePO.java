package com.dclingcloud.kubetopo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Objects;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Accessors(chain = true)
@SuperBuilder
@Entity
@Table(name = "node", schema = "k8s")
public class NodePO extends BasePO {
    @Id
    @Column(name = "uid", nullable = false, length = 36)
    private String uid;
    @Column
    private String name;
    @Column
    private String hostname;
    @Column
    private String internalIP;
    @Column
    private String podCIDR;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodePO nodePO = (NodePO) o;
        return Objects.equals(uid, nodePO.uid) && Objects.equals(name, nodePO.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), uid, name);
    }
}
