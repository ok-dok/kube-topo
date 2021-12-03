package com.dclingcloud.kubetopo.vo;

import lombok.*;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class JsonResult<T> implements Serializable {
    private T data;
    private String code;
    private JsonResultStatus status;

    public static enum JsonResultStatus {
        SUCCESS, ERROR
    }
}
