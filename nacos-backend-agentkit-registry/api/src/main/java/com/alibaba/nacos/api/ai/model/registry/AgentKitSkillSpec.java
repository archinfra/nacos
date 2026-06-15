/*
 * Copyright 1999-2026 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.alibaba.nacos.api.ai.model.registry;

import java.util.List;

/**
 * Normalized AgentKit Skill spec. This is the registry JSON view of SKILL.md + resources.
 *
 * @author nacos
 */
public class AgentKitSkillSpec {

    private String description;

    private String license;

    private String compatibility;

    private List<String> allowedTools;

    private String body;

    private List<AgentKitResourceRef> resources;

    private String skillSet;

    private List<String> groups;

    private List<String> keywords;

    private String modelName;

    private String modelDescription;

    private String matchHint;

    private String activation;

    private Integer priority;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public String getCompatibility() {
        return compatibility;
    }

    public void setCompatibility(String compatibility) {
        this.compatibility = compatibility;
    }

    public List<String> getAllowedTools() {
        return allowedTools;
    }

    public void setAllowedTools(List<String> allowedTools) {
        this.allowedTools = allowedTools;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public List<AgentKitResourceRef> getResources() {
        return resources;
    }

    public void setResources(List<AgentKitResourceRef> resources) {
        this.resources = resources;
    }

    public String getSkillSet() {
        return skillSet;
    }

    public void setSkillSet(String skillSet) {
        this.skillSet = skillSet;
    }

    public List<String> getGroups() {
        return groups;
    }

    public void setGroups(List<String> groups) {
        this.groups = groups;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getModelDescription() {
        return modelDescription;
    }

    public void setModelDescription(String modelDescription) {
        this.modelDescription = modelDescription;
    }

    public String getMatchHint() {
        return matchHint;
    }

    public void setMatchHint(String matchHint) {
        this.matchHint = matchHint;
    }

    public String getActivation() {
        return activation;
    }

    public void setActivation(String activation) {
        this.activation = activation;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }
}
