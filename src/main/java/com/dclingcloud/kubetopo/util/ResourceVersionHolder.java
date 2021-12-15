package com.dclingcloud.kubetopo.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.concurrent.atomic.AtomicLong;

public class ResourceVersionHolder {
    private AtomicLong resourceVersion = new AtomicLong(-1L);

    /**
     * 更新保持最大的ResourceVersion
     *
     * @param newResourceVersion
     * @return 更新成功返回true，失败返回false
     */
    public boolean syncUpdateLatest(String newResourceVersion) {
        if (StringUtils.isBlank(newResourceVersion)) {
            return false;
        }
        Long newVersion = NumberUtils.createLong(newResourceVersion);
        if (newVersion == null) {
            return false;
        } else {
            long replaced = resourceVersion.updateAndGet(pre -> pre < newVersion ? newVersion : pre);
            return replaced == newVersion;
        }
    }

    @Override
    public String toString() {
        return resourceVersion.get() < 0L ? "" : resourceVersion.toString();
    }
}
