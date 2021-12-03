package com.dclingcloud.kubetopo.entity;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.EntityListeners;
import javax.persistence.MappedSuperclass;
import java.io.Serializable;
import java.util.Date;

@Data
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public class BasePO implements Serializable {
    @Column
    @CreatedDate
    protected Date gmtCreate = new Date();
    @Column
    @LastModifiedDate
    protected Date gmtModified;
}
