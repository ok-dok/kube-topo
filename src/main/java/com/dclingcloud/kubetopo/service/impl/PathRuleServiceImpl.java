package com.dclingcloud.kubetopo.service.impl;

import com.dclingcloud.kubetopo.entity.PathRulePO;
import com.dclingcloud.kubetopo.repository.PathRuleRepository;
import com.dclingcloud.kubetopo.service.PathRuleService;
import com.dclingcloud.kubetopo.util.K8sServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.transaction.Transactional;
import java.util.List;

@Service
@Slf4j
public class PathRuleServiceImpl implements PathRuleService {
    @Resource
    private PathRuleRepository pathRuleRepository;

    @Transactional
    @Override
    public void save(PathRulePO pathRule) throws K8sServiceException {
        try {
            pathRuleRepository.saveAndFlush(pathRule);
        } catch (Exception e) {
            log.error("Error: save {} failed. {}", PathRulePO.class.getName(), pathRule, e);
            throw new K8sServiceException("Unable to save " + PathRulePO.class.getSimpleName(), e);
        }
    }

    @Transactional
    @Override
    public void delete(PathRulePO pathRule) throws K8sServiceException {
        try {
            pathRuleRepository.updateStatusByUid(pathRule.getUid(), "DELETED");
        } catch (Exception e) {
            log.error("Error: save {} failed. {}", PathRulePO.class.getName(), pathRule, e);
            throw new K8sServiceException("Unable to save " + PathRulePO.class.getSimpleName(), e);
        }
    }

    @Transactional
    @Override
    public void saveAll(List<PathRulePO> pathRules) throws K8sServiceException {
        try {
            pathRuleRepository.saveAllAndFlush(pathRules);
        } catch (Exception e) {
            log.error("Error: save {} list failed.", PathRulePO.class.getName(), e);
            throw new K8sServiceException("Unable to save " + PathRulePO.class.getSimpleName() + " list", e);
        }
    }
}
