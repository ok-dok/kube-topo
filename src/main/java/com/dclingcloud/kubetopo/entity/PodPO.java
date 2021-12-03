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
    @OneToMany(cascade = CascadeType.DETACH, mappedBy = "pod")
    private List<PodPortPO> ports;
}
