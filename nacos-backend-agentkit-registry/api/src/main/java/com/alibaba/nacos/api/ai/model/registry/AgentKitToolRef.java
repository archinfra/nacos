/*
 * Copyright 1999-2026 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.alibaba.nacos.api.ai.model.registry;

import java.util.List;
import java.util.Map;

/**
 * Tool reference aligned with MCP's tools/resources/prompts separation.
 *
 * @author nacos
 */
public class AgentKitToolRef {

    private String type;

    private String name;

    private String server;

    private List<String> toolFilter;

    private Map<String, Object> args;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public List<String> getToolFilter() {
        return toolFilter;
    }

    public void setToolFilter(List<String> toolFilter) {
        this.toolFilter = toolFilter;
    }

    public Map<String, Object> getArgs() {
        return args;
    }

    public void setArgs(Map<String, Object> args) {
        this.args = args;
    }
}
