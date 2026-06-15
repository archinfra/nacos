/*
 * Copyright 1999-2026 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.alibaba.nacos.ai.controller;

import com.alibaba.nacos.ai.constant.Constants;
import com.alibaba.nacos.ai.service.registry.AgentKitRegistryService;
import com.alibaba.nacos.ai.utils.registry.AgentKitDigestUtils;
import com.alibaba.nacos.api.ai.model.registry.AgentKitAgent;
import com.alibaba.nacos.api.ai.model.registry.AgentKitRuntimeManifest;
import com.alibaba.nacos.api.ai.model.registry.AgentKitSkill;
import com.alibaba.nacos.api.annotation.NacosApi;
import com.alibaba.nacos.api.annotation.Since;
import com.alibaba.nacos.api.common.ApiType;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.model.v2.Result;
import com.alibaba.nacos.auth.annotation.Secured;
import com.alibaba.nacos.plugin.auth.constant.ActionTypes;
import com.alibaba.nacos.plugin.auth.constant.SignType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static com.alibaba.nacos.plugin.auth.constant.Constants.Tag.ALLOW_ANONYMOUS;

/**
 * AgentKit normalized registry client API for dynamic runtime loading.
 *
 * @author nacos
 */
@NacosApi
@RestController
@RequestMapping(Constants.Registry.CLIENT_PATH)
public class AgentKitRegistryClientController {

    private final AgentKitRegistryService registryService;

    public AgentKitRegistryClientController(AgentKitRegistryService registryService) {
        this.registryService = registryService;
    }

    @Since("3.2.3")
    @GetMapping("/skills/{name}")
    @Secured(action = ActionTypes.READ, signType = SignType.AI, apiType = ApiType.OPEN_API,
        tags = {ALLOW_ANONYMOUS})
    public ResponseEntity<Result<AgentKitSkill>> getSkill(
        @RequestParam(value = "namespaceId", required = false, defaultValue = "public") String namespaceId,
        @PathVariable("name") String name,
        @RequestParam(value = "version", required = false) String version,
        @RequestParam(value = "label", required = false) String label) throws NacosException {
        AgentKitSkill skill = registryService.getSkill(namespaceId, name, version, label);
        return ResponseEntity.ok()
            .eTag(AgentKitDigestUtils.quoted(skill.getMetadata().getDigest()))
            .header(Constants.Registry.HEADER_RESOURCE_VERSION, skill.getMetadata().getResourceVersion())
            .header(Constants.Registry.HEADER_RESOURCE_DIGEST, skill.getMetadata().getDigest())
            .body(Result.success(skill));
    }

    @Since("3.2.3")
    @GetMapping("/agents/{name}")
    @Secured(action = ActionTypes.READ, signType = SignType.AI, apiType = ApiType.OPEN_API,
        tags = {ALLOW_ANONYMOUS})
    public ResponseEntity<Result<AgentKitAgent>> getAgent(
        @RequestParam(value = "namespaceId", required = false, defaultValue = "public") String namespaceId,
        @PathVariable("name") String name,
        @RequestParam(value = "version", required = false) String version,
        @RequestParam(value = "label", required = false) String label) throws NacosException {
        AgentKitAgent agent = registryService.getAgent(namespaceId, name, version, label);
        return ResponseEntity.ok()
            .eTag(AgentKitDigestUtils.quoted(agent.getMetadata().getDigest()))
            .header(Constants.Registry.HEADER_RESOURCE_VERSION, agent.getMetadata().getResourceVersion())
            .header(Constants.Registry.HEADER_RESOURCE_DIGEST, agent.getMetadata().getDigest())
            .body(Result.success(agent));
    }

    @Since("3.2.3")
    @GetMapping("/agents/{name}/manifest")
    @Secured(action = ActionTypes.READ, signType = SignType.AI, apiType = ApiType.OPEN_API,
        tags = {ALLOW_ANONYMOUS})
    public ResponseEntity<Result<AgentKitRuntimeManifest>> getRuntimeManifest(
        @RequestParam(value = "namespaceId", required = false, defaultValue = "public") String namespaceId,
        @PathVariable("name") String name,
        @RequestParam(value = "version", required = false) String version,
        @RequestParam(value = "label", required = false) String label) throws NacosException {
        AgentKitRuntimeManifest manifest = registryService.getRuntimeManifest(namespaceId, name, version, label);
        return ResponseEntity.ok()
            .eTag(AgentKitDigestUtils.quoted(manifest.getDigest()))
            .header(Constants.Registry.HEADER_RESOURCE_VERSION, manifest.getResourceVersion())
            .header(Constants.Registry.HEADER_RESOURCE_DIGEST, manifest.getDigest())
            .body(Result.success(manifest));
    }

    @Since("3.2.3")
    @GetMapping("/agents/{name}/card")
    @Secured(action = ActionTypes.READ, signType = SignType.AI, apiType = ApiType.OPEN_API,
        tags = {ALLOW_ANONYMOUS})
    public Result<Map<String, Object>> getAgentCard(
        @RequestParam(value = "namespaceId", required = false, defaultValue = "public") String namespaceId,
        @PathVariable("name") String name,
        @RequestParam(value = "version", required = false) String version,
        @RequestParam(value = "label", required = false) String label,
        @RequestParam(value = "endpointUrl", required = false) String endpointUrl) throws NacosException {
        return Result.success(registryService.getAgentCard(namespaceId, name, version, label, endpointUrl));
    }
}
