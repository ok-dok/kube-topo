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
@Table(name = "pod_port", schema = "k8s")
public class PodPortPO implements Serializable {
    @Id
    @Column(name = "uid", nullable = false, length = 36)
    @GenericGenerator(name = "uuid-gen", strategy = "uuid2")
    @GeneratedValue(generator = "uuid-gen")
    private String uid;

    @Column
    private String name;
    @Column
    private Integer port;
    @Column
    private String protocol;
    @Column
    private String appProtocol;

    @ManyToOne(cascade = CascadeType.DETACH)
    @JoinColumn(name = "service_port_uid")
    private ServicePortPO servicePort;

    @ManyToOne(cascade = CascadeType.DETACH)
    @JoinColumn(name = "podUid")
    private PodPO pod;

}
