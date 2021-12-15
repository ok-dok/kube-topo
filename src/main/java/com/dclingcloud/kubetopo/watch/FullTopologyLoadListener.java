package com.dclingcloud.kubetopo.watch;

import com.dclingcloud.kubetopo.service.TopologyService;
import com.dclingcloud.kubetopo.util.K8sServiceException;
import io.kubernetes.client.openapi.ApiException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class FullTopologyLoadListener implements InitializingBean {
    @Resource
    private TopologyService topologyService;
    private ReentrantLock lock = new ReentrantLock();
    private boolean loaded = false;

    @Override
    public void afterPropertiesSet() throws Exception {
        reload();
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
