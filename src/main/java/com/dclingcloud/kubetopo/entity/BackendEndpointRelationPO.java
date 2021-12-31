package com.dclingcloud.kubetopo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import javax.persistence.*;
import java.util.Objects;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Accessors(chain = true)
@SuperBuilder
@Entity
@Table(name = "backend_endpoint_relation", schema = "k8s", uniqueConstraints = {
        @UniqueConstraint(name = "uc_unique_key_index", columnNames = {"service_port_uid", "pod_port_uid"})
})
public class BackendEndpointRelationPO extends BasePO {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(cascade = CascadeType.DETACH)
    @JoinColumn(name = "service_port_uid")
    private ServicePortPO servicePort;

    @ManyToOne(cascade = CascadeType.DETACH)
    @JoinColumn(name = "pod_port_uid")
    private PodPortPO podPort;
    /**
     * Ready or Terminating
     */
    @Column
    private String state;

    @Column
    private String addresses;

    @Column
    private Integer port;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        BackendEndpointRelationPO that = (BackendEndpointRelationPO) o;
        return servicePort.equals(that.servicePort) && podPort.equals(that.podPort);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), servicePort, podPort);
    }
}
