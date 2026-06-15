/*
 * Copyright 1999-2026 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.alibaba.nacos.ai.service.registry;

import com.alibaba.nacos.api.ai.model.registry.AgentKitAgent;
import com.alibaba.nacos.api.ai.model.registry.AgentKitRuntimeManifest;
import com.alibaba.nacos.api.ai.model.registry.AgentKitSkill;
import com.alibaba.nacos.api.exception.NacosException;

import java.util.Map;

/**
 * AgentKit normalized registry service.
 *
 * @author nacos
 */
public interface AgentKitRegistryService {

    AgentKitSkill getSkill(String namespaceId, String name, String version, String label)
        throws NacosException;

    String upsertSkillDraft(String namespaceId, AgentKitSkill skill, String ifMatch)
        throws NacosException;

    String publishSkill(String namespaceId, String name, String version, boolean force)
        throws NacosException;

    void deleteSkill(String namespaceId, String name) throws NacosException;

    AgentKitAgent getAgent(String namespaceId, String name, String version, String label)
        throws NacosException;

    String upsertAgentDraft(String namespaceId, AgentKitAgent agent, String ifMatch)
        throws NacosException;

    String publishAgent(String namespaceId, String name, String version, boolean force)
        throws NacosException;

    void deleteAgent(String namespaceId, String name) throws NacosException;

    AgentKitRuntimeManifest getRuntimeManifest(String namespaceId, String name, String version,
        String label) throws NacosException;

    Map<String, Object> getAgentCard(String namespaceId, String name, String version,
        String label, String endpointUrl) throws NacosException;
}
