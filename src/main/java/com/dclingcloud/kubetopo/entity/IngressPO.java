package com.dclingcloud.kubetopo.entity;

import lombok.Builder;
import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

@Data
@Builder
@Entity
@Table(name = "ingress")
public class IngressPO implements Serializable {
    @Id
    @Column(name = "uid", nullable = false)
    private String uid;
    @Column
    private String name;
    @Column
    private String className;
    @Column
    private String hostname;
    @Column
    private String ips;
    @OneToMany(cascade = CascadeType.DETACH, mappedBy = "ingress")
    private List<PathRulePO> pathRules;
}
