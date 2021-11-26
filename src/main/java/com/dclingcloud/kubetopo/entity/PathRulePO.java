package com.dclingcloud.kubetopo.entity;

import lombok.Builder;
import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;

@Data
@Builder
@Entity
@Table(name = "path_rule")
public class PathRulePO implements Serializable {
    @Id
    @Column(name = "uid", nullable = false)
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
}
