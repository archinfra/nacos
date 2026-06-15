/*
 * Copyright 1999-2026 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.alibaba.nacos.api.ai.model.registry;

import java.util.Map;

/**
 * Sub-agent reference and invocation policy.
 *
 * @author nacos
 */
public class AgentKitSubAgentRef {

    private String id;

    private AgentKitSkillRef ref;

    private Map<String, Object> invocation;

    private Map<String, Object> context;

    private Map<String, Object> workspace;

    private Map<String, Object> output;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public AgentKitSkillRef getRef() {
        return ref;
    }

    public void setRef(AgentKitSkillRef ref) {
        this.ref = ref;
    }

    public Map<String, Object> getInvocation() {
        return invocation;
    }

    public void setInvocation(Map<String, Object> invocation) {
        this.invocation = invocation;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public void setContext(Map<String, Object> context) {
        this.context = context;
    }

    public Map<String, Object> getWorkspace() {
        return workspace;
    }

    public void setWorkspace(Map<String, Object> workspace) {
        this.workspace = workspace;
    }

    public Map<String, Object> getOutput() {
        return output;
    }

    public void setOutput(Map<String, Object> output) {
        this.output = output;
    }
}
