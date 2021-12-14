package com.dclingcloud.kubetopo.service;

import com.dclingcloud.kubetopo.entity.PathRulePO;
import com.dclingcloud.kubetopo.util.K8sServiceException;

import java.util.List;

public interface PathRuleService {
    void save(PathRulePO pathRule) throws K8sServiceException;

    /**
     * it's not a real delete
     *
     * @param pathRule
     * @throws K8sServiceException
     */
    void delete(PathRulePO pathRule) throws K8sServiceException;

    void saveAll(List<PathRulePO> pathRules) throws K8sServiceException;

    /**
     * it's not a real delete
     *
     * @param ingressUid
     */
    void deleteAllByIngressUid(String ingressUid);
}
