package com.dclingcloud.kubetopo.constants;

import java.util.Arrays;
import java.util.Optional;

public enum K8sResources {
    Service("Service"), Services("Services"),
    Ingress("Ingress"), Ingresses("Ingresses"),
    Endpoint("Endpoint"), Endpoints("Endpoints"),
    EndpointSlice("EndpointSlice"), EndpointSlices("EndpointSlices"),
    Pod("Pod"), Pods("Pods");

    String value;

    K8sResources(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    public static K8sResources parse(String name) {
        Optional<K8sResources> first = Arrays.stream(K8sResources.values()).filter(res -> res.value.equalsIgnoreCase(name)).findFirst();
        return first.get();
    }
}
