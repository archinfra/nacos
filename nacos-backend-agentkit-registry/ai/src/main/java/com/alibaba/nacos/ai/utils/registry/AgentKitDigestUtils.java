/*
 * Copyright 1999-2026 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.alibaba.nacos.ai.utils.registry;

import com.alibaba.nacos.common.utils.JacksonUtils;
import com.alibaba.nacos.common.utils.MD5Utils;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;

/**
 * Registry resource digest utilities.
 *
 * @author nacos
 */
public final class AgentKitDigestUtils {

    private AgentKitDigestUtils() {
    }

    public static String md5(Object object) {
        try {
            return MD5Utils.md5Hex(JacksonUtils.toJson(object).getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 algorithm not available", e);
        }
    }

    public static String quoted(String digest) {
        return digest == null ? null : '"' + digest + '"';
    }

    public static String unquote(String value) {
        if (value == null) {
            return null;
        }
        String result = value.trim();
        if (result.length() >= 2 && result.startsWith("\"") && result.endsWith("\"")) {
            return result.substring(1, result.length() - 1);
        }
        return result;
    }
}
