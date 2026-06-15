/*
 * Copyright 1999-2026 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.alibaba.nacos.api.ai.model.registry;

/**
 * Normalized Agent resource.
 *
 * @author nacos
 */
public class AgentKitAgent {

    private String apiVersion;

    private String kind;

    private AgentKitMetadata metadata;

    private AgentKitAgentSpec spec;

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public AgentKitMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(AgentKitMetadata metadata) {
        this.metadata = metadata;
    }

    public AgentKitAgentSpec getSpec() {
        return spec;
    }

    public void setSpec(AgentKitAgentSpec spec) {
        this.spec = spec;
    }
}
