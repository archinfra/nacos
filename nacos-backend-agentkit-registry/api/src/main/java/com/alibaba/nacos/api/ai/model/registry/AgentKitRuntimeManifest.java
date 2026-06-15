/*
 * Copyright 1999-2026 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.alibaba.nacos.api.ai.model.registry;

import java.util.List;

/**
 * Materialized runtime manifest returned to AgentKit before a run.
 *
 * @author nacos
 */
public class AgentKitRuntimeManifest {

    private AgentKitAgent agent;

    private List<AgentKitSkill> skills;

    private String resourceVersion;

    private String digest;

    public AgentKitAgent getAgent() {
        return agent;
    }

    public void setAgent(AgentKitAgent agent) {
        this.agent = agent;
    }

    public List<AgentKitSkill> getSkills() {
        return skills;
    }

    public void setSkills(List<AgentKitSkill> skills) {
        this.skills = skills;
    }

    public String getResourceVersion() {
        return resourceVersion;
    }

    public void setResourceVersion(String resourceVersion) {
        this.resourceVersion = resourceVersion;
    }

    public String getDigest() {
        return digest;
    }

    public void setDigest(String digest) {
        this.digest = digest;
    }
}
