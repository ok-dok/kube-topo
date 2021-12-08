package com.dclingcloud.kubetopo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.persistence.*;

@NoArgsConstructor
@AllArgsConstructor
@Data
@SuperBuilder
@Entity
@Table(name = "pod_port", schema = "k8s")
public class PodPortPO extends BasePO {

    @Id
    @Column(name = "uid", nullable = false)
    private String uid;
    //    @Id
//    @Column(name = "uid", nullable = false, length = 36)
//    @GenericGenerator(name = "uuid-gen", strategy = "uuid2")
//    @GeneratedValue(generator = "uuid-gen")
//    private String uid;
//    @Data
//    @NoArgsConstructor
//    @AllArgsConstructor
//    @EqualsAndHashCode
//    @Embeddable
//    public static class PodPortUPK implements Serializable {
//        /**
//         * endpoint uid, 当podUid发生变化时，epUid不会发生变化
//         */
//        @Column
//        private String epUid;
//        @Column
//        private Integer port;
//    }
//
//    @EmbeddedId
//    private PodPortUPK id;
        /**
         * endpoint uid, 当podUid发生变化时，epUid不会发生变化
         */
    @Column
    private String epUid;
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
