/*
 * Copyright 1999-2026 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.alibaba.nacos.api.ai.model.registry;

/**
 * Skill reference in a normalized Agent spec.
 *
 * @author nacos
 */
public class AgentKitSkillRef {

    private String name;

    private String version;

    private String label;

    private String activation;

    private String refreshPolicy;

    private Boolean required;

    private Integer order;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getActivation() {
        return activation;
    }

    public void setActivation(String activation) {
        this.activation = activation;
    }

    public String getRefreshPolicy() {
        return refreshPolicy;
    }

    public void setRefreshPolicy(String refreshPolicy) {
        this.refreshPolicy = refreshPolicy;
    }

    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }
}
