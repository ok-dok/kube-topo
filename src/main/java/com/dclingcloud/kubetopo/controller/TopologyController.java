package com.dclingcloud.kubetopo.controller;

import com.dclingcloud.kubetopo.model.ServiceInfo;
import com.dclingcloud.kubetopo.service.TopologyService;
import io.kubernetes.client.openapi.ApiException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/v1/k8s/topo")
public class TopologyController {
    @Resource
    private TopologyService topologyService;

    @GetMapping("/all")
    public List<ServiceInfo> getServiceTopology() {
        try {
            topologyService.loadResources();
            return null;
        } catch (ApiException e) {
            System.out.println(e.getResponseBody());
            e.printStackTrace();
            return null;
        }
//
//        return null;
    }

    @GetMapping("mapping/endpoint/{ep}")
    public ServiceInfo getMappingByPodEndpoint(@PathVariable("ep") String ep) {
        if (!StringUtils.contains(ep, ":")) {
            return null;
        } else {
            String[] split = ep.split(":");
        }
        return null;
    }

}
