package com.dclingcloud.kubetopo.controller;

import com.dclingcloud.kubetopo.model.ServiceInfo;
import com.dclingcloud.kubetopo.service.TopologyService;
import io.kubernetes.client.openapi.ApiException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
public class TopologyController {
    @Resource
    private TopologyService topologyService;

    @GetMapping("/svc")
    public List<ServiceInfo> getServices() {
        try {
            return topologyService.getServices();
        } catch (ApiException e) {
            System.out.println(e.getResponseBody());
            e.printStackTrace();
            return null;
        }
    }
}
