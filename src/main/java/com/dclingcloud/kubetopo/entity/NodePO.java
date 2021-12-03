package com.dclingcloud.kubetopo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@Entity
@Table(name = "node", schema = "k8s")
public class NodePO extends BasePO {
    @Id
    @Column(name = "uid", nullable = false, length = 36)
    private String uid;
    @Column
    private String name;
    @Column
    private String hostname;
    @Column
    private String internalIP;
    @Column
    private String podCIDR;
}
