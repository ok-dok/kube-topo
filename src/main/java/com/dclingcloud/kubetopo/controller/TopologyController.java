package com.dclingcloud.kubetopo.controller;

import com.dclingcloud.kubetopo.entity.PodPortPO;
import com.dclingcloud.kubetopo.service.impl.TopologyServiceImpl;
import com.dclingcloud.kubetopo.vo.JsonResult;
import com.dclingcloud.kubetopo.vo.TopologyVO;
import io.kubernetes.client.openapi.ApiException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/v1/k8s/topo")
public class TopologyController {
    @Resource
    private TopologyServiceImpl topologyService;

    @GetMapping("/all")
    public JsonResult<TopologyVO> getServiceTopology() {
        try {
            topologyService.loadResources();
            TopologyVO topo = topologyService.getTopology();
            return JsonResult.<TopologyVO>builder().code("1").status(JsonResult.JsonResultStatus.SUCCESS).data(topo).build();
        } catch (ApiException e) {
            System.out.println(e.getResponseBody());
            e.printStackTrace();
            return null;
        }
//
//        return null;
    }

    @GetMapping("mapping/endpoints/{ep}")
    public PodPortPO getMappingByPodEndpoint(@PathVariable("ep") String ep) {
        if (!StringUtils.contains(ep, ":")) {
            return null;
        } else {
            String[] split = ep.split(":");
        }
        return null;
    }

}
