package com.dclingcloud.kubetopo.model;

import lombok.Builder;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.io.Serializable;
import java.util.Objects;

@Data
@Builder
public class PodPort implements Serializable {
    private Long id;
    private String podUID;
    private String name;
    private Integer port;
    private String protocol;

}
