package com.dclingcloud.kubetopo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@Entity
@Table(name = "path_rule", schema = "k8s")
public class PathRulePO implements Serializable {
    @Id
    @Column(name = "uid", nullable = false, length = 36)
    @GenericGenerator(name = "uuid-gen", strategy = "uuid2")
    @GeneratedValue(generator = "uuid-gen")
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
