package com.dclingcloud.kubetopo.model;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class IngressInfo {
    private String hostname;
    private List<String> ips;
    private String path;
}
