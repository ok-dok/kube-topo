package com.dclingcloud.kubetopo.entity;

import com.dclingcloud.kubetopo.util.IntOrStringConverter;
import io.kubernetes.client.custom.IntOrString;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import javax.persistence.*;
import java.util.Collection;
import java.util.Objects;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Accessors(chain = true)
@SuperBuilder
@Entity
@Table(name = "service_port", schema = "k8s")
public class ServicePortPO extends BasePO {
    @Id
    @Column(name = "uid", nullable = false)
//    @GenericGenerator(name = "uuid-gen", strategy = "uuid2")
//    @GeneratedValue(generator = "uuid-gen")
    private String uid;
    @Column
    private String name;
    @Column
    private String protocol;
    @Column
    private String appProtocol;
    @Column
    private Integer port;
    @Column
    private Integer nodePort;
    @Column
    @Convert(converter = IntOrStringConverter.class)
    private IntOrString targetPort;

    @ManyToOne(cascade = CascadeType.DETACH)
    @NotFound(action = NotFoundAction.IGNORE)
    @JoinColumn(name = "service_uid")
    private ServicePO service;

    @OneToMany(cascade = CascadeType.DETACH, mappedBy = "backend")
    @NotFound(action = NotFoundAction.IGNORE)
    private Collection<PathRulePO> ingressPathRules;

    @OneToMany(cascade = CascadeType.DETACH, mappedBy = "servicePort")
    private Collection<BackendEndpointRelationPO> backendEndpointRelations;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ServicePortPO that = (ServicePortPO) o;
        return uid.equals(that.uid) && Objects.equals(protocol, that.protocol) && port.equals(that.port) && targetPort.equals(that.targetPort) && service.equals(that.service);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), uid, protocol, port, targetPort, service);
    }
}
