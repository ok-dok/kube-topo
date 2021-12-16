package com.dclingcloud.kubetopo.job;

import com.dclingcloud.kubetopo.repository.*;
import com.dclingcloud.kubetopo.util.K8sServiceException;
import com.dclingcloud.kubetopo.watch.EventType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.concurrent.TimeUnit;

@Async
@Component
@Slf4j
public class ResourcesDeletionScheduler {
    @Resource
    private ServiceRepository serviceRepository;
    @Resource
    private ServicePortRepository servicePortRepository;
    @Resource
    private IngressRepository ingressRepository;
    @Resource
    private PathRuleRepository pathRuleRepository;
    @Resource
    private PodRepository podRepository;
    @Resource
    private PodPortRepository podPortRepository;
    private Integer historicalRemainTime = 1;
    private TemporalUnit historicalRemainTimeUnit = ChronoUnit.DAYS;

    @Transactional(rollbackOn = Exception.class)
//    @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.SECONDS)
    @Scheduled(cron = "0 0 0 * * ?")
    public void dailyCheck() {
        try {
            log.info("Deleting the expired resources");
            LocalDateTime beforeDateTime = LocalDateTime.now().minus(historicalRemainTime, historicalRemainTimeUnit);
            serviceRepository.deleteAllByStatusAndGmtModifiedBefore(EventType.DELETED, beforeDateTime);
            servicePortRepository.deleteAllByStatusAndGmtModifiedBefore(EventType.DELETED, beforeDateTime);
            ingressRepository.deleteAllByStatusAndGmtModifiedBefore(EventType.DELETED, beforeDateTime);
            pathRuleRepository.deleteAllByStatusAndGmtModifiedBefore(EventType.DELETED, beforeDateTime);
            podRepository.deleteAllByStatusAndGmtModifiedBefore(EventType.DELETED, beforeDateTime);
            podPortRepository.deleteAllByStatusAndGmtModifiedBefore(EventType.DELETED, beforeDateTime);
        } catch (Exception e) {
            log.error("Delete expired resources failed", e);
            throw new K8sServiceException(e);
        }
    }
}
