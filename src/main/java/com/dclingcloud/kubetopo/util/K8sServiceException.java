package com.dclingcloud.kubetopo.util;

public class K8sServiceException extends RuntimeException{
    public K8sServiceException(String message) {
        super(message);
    }

    public K8sServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public K8sServiceException(Throwable cause) {
        super(cause);
    }
}
