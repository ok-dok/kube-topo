package com.dclingcloud.kubetopo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.persistence.*;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
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
    @Column
    private String nodeName;
    @Column
    private String hostname;
    @Column
    private String ip;
    @Column
    private String containerId;
    @OneToMany(cascade = CascadeType.DETACH, mappedBy = "pod")
    private List<PodPortPO> ports;
}
