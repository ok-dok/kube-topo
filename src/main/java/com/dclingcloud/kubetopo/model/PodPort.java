package com.dclingcloud.kubetopo.model;

import lombok.Builder;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Data
@Builder
public class PodPort {
    private Long id;
    private String podUID;
    private String name;
    private Integer port;
    private String protocol;
}
