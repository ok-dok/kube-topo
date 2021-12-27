package com.dclingcloud.kubetopo.util;

import org.springframework.util.Base64Utils;

import java.nio.charset.StandardCharsets;

public class CustomUidGenerateUtil {
    public static String getServicePortUid(String serviceUid, Integer port, String protocol) {
        String id = serviceUid + ":" + port + ":" + protocol;
        String uid = new String(Base64Utils.encode(id.getBytes(StandardCharsets.UTF_8)));
        return uid;
    }

    public static String getPodPortUid(String podUid, Integer port, String protocol) {
        String id = podUid + ":" + port + ":" + protocol;
        String uid = new String(Base64Utils.encode(id.getBytes(StandardCharsets.UTF_8)));
        return uid;
    }
}
