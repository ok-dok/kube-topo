package com.dclingcloud.kubetopo.watch;

import com.dclingcloud.kubetopo.entity.IngressPO;
import com.dclingcloud.kubetopo.entity.PathRulePO;
import com.dclingcloud.kubetopo.service.IngressService;
import com.dclingcloud.kubetopo.service.PathRuleService;
import com.dclingcloud.kubetopo.util.K8sApi;
import com.dclingcloud.kubetopo.util.K8sServiceException;
import com.google.gson.reflect.TypeToken;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1HTTPIngressPath;
import io.kubernetes.client.openapi.models.V1Ingress;
import io.kubernetes.client.openapi.models.V1IngressRule;
import io.kubernetes.client.util.Watch;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
@Async
public class IngressEventWatcher extends EventWatcher<V1Ingress> {
    @Resource
    private IngressService ingressService;
    @Resource
    private PathRuleService pathRuleService;

    @Override
    protected void processEventObject(String type, Object object, StringBuilder eventLog) {
        V1Ingress ingress = (V1Ingress) object;
        switch (type.toUpperCase()) {
            case ADDED:
                try {
                    ingressService.saveOrUpdate(ingress, ADDED);
                } catch (K8sServiceException e) {
                    // TODO 保存异常处理，重试？
                    throw e;
                }
                break;
            case DELETED:
                try {
                    ingressService.delete(ingress);
                } catch (K8sServiceException e) {
                    // TODO 保存异常处理，重试？
                    throw e;
                }
                break;
            case MODIFIED:
                try {
                    ingressService.saveOrUpdate(ingress, MODIFIED);
                } catch (K8sServiceException e) {
                    // TODO 保存异常处理，重试？
                    throw e;
                }
                break;
        }
    }

    @Override
    protected Watch<V1Ingress> createWatch() throws ApiException {
        Call call = K8sApi.createIngressesCall(this.resourceVersion);
        Watch<V1Ingress> watch = Watch.createWatch(apiClient, call, new TypeToken<Watch.Response<V1Ingress>>() {
        }.getType());
        return watch;
    }

    private void processIngressPathRules(V1Ingress ingress) {
        List<V1IngressRule> rules = ingress.getSpec().getRules();
        ArrayList<PathRulePO> pathRules = new ArrayList<>();
        for (V1IngressRule rule : rules) {
            // 七层路由
            List<V1HTTPIngressPath> paths = rule.getHttp().getPaths();
            for (V1HTTPIngressPath path : paths) {
                String id = ingress.getMetadata().getUid() + ":" + rule.getHost() + ":" + path.getPath() + ":" + path.getPathType();
                String hash = DigestUtils.md5DigestAsHex(Base64Utils.encode(id.getBytes(StandardCharsets.UTF_8)));
                String uid = new StringBuilder(hash).insert(20, '-').insert(16, '-').insert(12, '-').insert(8, '-').toString();
                PathRulePO pathRulePO = PathRulePO.builder()
                        .uid(uid)
                        .host(rule.getHost())
                        .path(path.getPath())
                        .pathType(path.getPathType())
                        .ingress(IngressPO.builder().uid(ingress.getMetadata().getUid()).build())
                        .build();
                pathRules.add(pathRulePO);
            }
        }
        pathRuleService.saveAll(pathRules);
    }

}
