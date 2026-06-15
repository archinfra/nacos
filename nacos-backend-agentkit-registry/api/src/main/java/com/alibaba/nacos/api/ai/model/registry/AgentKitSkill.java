/*
 * Copyright 1999-2026 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.alibaba.nacos.api.ai.model.registry;

/**
 * Normalized AgentKit Skill resource.
 *
 * @author nacos
 */
public class AgentKitSkill {

    private String apiVersion;

    private String kind;

    private AgentKitMetadata metadata;

    private AgentKitSkillSpec spec;

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

    public AgentKitSkillSpec getSpec() {
        return spec;
    }

    public void setSpec(AgentKitSkillSpec spec) {
        this.spec = spec;
    }
}
