package com.dclingcloud.kubetopo.entity;

import lombok.Builder;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Data
@Builder
@Entity
@Table(name = "node")
public class NodePO implements Serializable {
    @Id
    @Column(name = "uid", nullable = false)
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
