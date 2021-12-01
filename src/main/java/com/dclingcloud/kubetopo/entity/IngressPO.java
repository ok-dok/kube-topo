package com.dclingcloud.kubetopo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@Entity
@Table(name = "ingress", schema = "k8s")
public class IngressPO implements Serializable {
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
    private List<PathRulePO> pathRules;
}
