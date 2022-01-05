package com.dclingcloud.kubetopo.watch;

import com.dclingcloud.kubetopo.service.TopologyService;
import com.dclingcloud.kubetopo.util.K8sServiceException;
import io.kubernetes.client.openapi.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.net.ssl.SSLException;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Slf4j
public class FullTopologyLoadListener implements InitializingBean {
    @Resource
    private TopologyService topologyService;
    @Resource
    private ApplicationContext applicationContext;
    private ReentrantLock lock = new ReentrantLock();
    private boolean loaded = false;

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("Loading full topology of K8s resources");
        try {
            reload();
        } catch (ApiException e) {
            if (e.getCause() instanceof SSLException) {
                log.error("Could not connect to K8s ApiServer.", e);
                SpringApplication.exit(applicationContext);
            } else {
                throw e;
            }
        }
        log.info("Loaded full topology of K8s resources, start watching events");
    }

    public boolean reload() throws ApiException, K8sServiceException {
        boolean locked = lock.tryLock();
        if (locked) {
            topologyService.updateAllResourcesWithDeletedStatus();
            topologyService.loadResourcesTopology();
            loaded = true;
            lock.unlock();
            return true;
        }
        return false;
    }

    public boolean isLoading() {
        return lock.isLocked();
    }

    public boolean hasLoaded() {
        return loaded;
    }
}
