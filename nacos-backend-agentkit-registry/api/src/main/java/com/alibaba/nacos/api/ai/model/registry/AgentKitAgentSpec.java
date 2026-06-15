/*
 * Copyright 1999-2026 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.alibaba.nacos.api.ai.model.registry;

import java.util.List;
import java.util.Map;

/**
 * Normalized Agent config spec for AgentKit runtime.
 *
 * @author nacos
 */
public class AgentKitAgentSpec {

    private String agentClass;

    private String description;

    private String modelRef;

    private String instruction;

    private Map<String, Object> generationConfig;

    private List<AgentKitSkillRef> skills;

    private List<AgentKitToolRef> tools;

    private List<AgentKitSubAgentRef> subAgents;

    private Map<String, Object> runtime;

    public String getAgentClass() {
        return agentClass;
    }

    public void setAgentClass(String agentClass) {
        this.agentClass = agentClass;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getModelRef() {
        return modelRef;
    }

    public void setModelRef(String modelRef) {
        this.modelRef = modelRef;
    }

    public String getInstruction() {
        return instruction;
    }

    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }

    public Map<String, Object> getGenerationConfig() {
        return generationConfig;
    }

    public void setGenerationConfig(Map<String, Object> generationConfig) {
        this.generationConfig = generationConfig;
    }

    public List<AgentKitSkillRef> getSkills() {
        return skills;
    }

    public void setSkills(List<AgentKitSkillRef> skills) {
        this.skills = skills;
    }

    public List<AgentKitToolRef> getTools() {
        return tools;
    }

    public void setTools(List<AgentKitToolRef> tools) {
        this.tools = tools;
    }

    public List<AgentKitSubAgentRef> getSubAgents() {
        return subAgents;
    }

    public void setSubAgents(List<AgentKitSubAgentRef> subAgents) {
        this.subAgents = subAgents;
    }

    public Map<String, Object> getRuntime() {
        return runtime;
    }

    public void setRuntime(Map<String, Object> runtime) {
        this.runtime = runtime;
    }
}
