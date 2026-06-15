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
import com.alibaba.nacos.api.ai.model.registry.AgentKitSkill;
import com.alibaba.nacos.api.annotation.NacosApi;
import com.alibaba.nacos.api.annotation.Since;
import com.alibaba.nacos.api.common.ApiType;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.model.v2.Result;
import com.alibaba.nacos.auth.annotation.Secured;
import com.alibaba.nacos.common.utils.StringUtils;
import com.alibaba.nacos.core.paramcheck.ExtractorManager;
import com.alibaba.nacos.plugin.auth.constant.ActionTypes;
import com.alibaba.nacos.plugin.auth.constant.SignType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * AgentKit normalized registry admin API.
 *
 * <p>This resource-oriented API exposes Agent and Skill CRUD as normalized JSON while reusing existing
 * Nacos Skill/AgentSpec draft, publish, label, visibility and storage services.</p>
 *
 * @author nacos
 */
@NacosApi
@RestController
@RequestMapping(Constants.Registry.ADMIN_PATH)
@ExtractorManager.Extractor(httpExtractor = ExtractorManager.DefaultHttpExtractor.class)
public class AgentKitRegistryAdminController {

    private final AgentKitRegistryService registryService;

    public AgentKitRegistryAdminController(AgentKitRegistryService registryService) {
        this.registryService = registryService;
    }

    @Since("3.2.3")
    @GetMapping("/skills/{name}")
    @Secured(action = ActionTypes.READ, signType = SignType.AI, apiType = ApiType.ADMIN_API)
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
    @PostMapping("/skills")
    @Secured(action = ActionTypes.WRITE, signType = SignType.AI, apiType = ApiType.ADMIN_API)
    public Result<String> createSkill(
        @RequestParam(value = "namespaceId", required = false, defaultValue = "public") String namespaceId,
        @RequestParam(value = "publish", required = false, defaultValue = "false") boolean publish,
        @RequestParam(value = "force", required = false, defaultValue = "false") boolean force,
        @RequestBody AgentKitSkill skill) throws NacosException {
        String version = registryService.upsertSkillDraft(namespaceId, skill, null);
        if (publish) {
            version = registryService.publishSkill(namespaceId, skill.getMetadata().getName(), version, force);
        }
        return Result.success(version);
    }

    @Since("3.2.3")
    @PutMapping("/skills/{name}")
    @Secured(action = ActionTypes.WRITE, signType = SignType.AI, apiType = ApiType.ADMIN_API)
    public Result<String> updateSkill(
        @RequestParam(value = "namespaceId", required = false, defaultValue = "public") String namespaceId,
        @PathVariable("name") String name,
        @RequestParam(value = "publish", required = false, defaultValue = "false") boolean publish,
        @RequestParam(value = "force", required = false, defaultValue = "false") boolean force,
        @RequestHeader(value = "If-Match", required = false) String ifMatch,
        @RequestBody AgentKitSkill skill) throws NacosException {
        if (skill.getMetadata() != null && StringUtils.isBlank(skill.getMetadata().getName())) {
            skill.getMetadata().setName(name);
        }
        String version = registryService.upsertSkillDraft(namespaceId, skill, ifMatch);
        if (publish) {
            version = registryService.publishSkill(namespaceId, name, version, force);
        }
        return Result.success(version);
    }

    @Since("3.2.3")
    @PostMapping("/skills/{name}/publish")
    @Secured(action = ActionTypes.WRITE, signType = SignType.AI, apiType = ApiType.ADMIN_API)
    public Result<String> publishSkill(
        @RequestParam(value = "namespaceId", required = false, defaultValue = "public") String namespaceId,
        @PathVariable("name") String name,
        @RequestParam(value = "version", required = false) String version,
        @RequestParam(value = "force", required = false, defaultValue = "false") boolean force)
        throws NacosException {
        return Result.success(registryService.publishSkill(namespaceId, name, version, force));
    }

    @Since("3.2.3")
    @DeleteMapping("/skills/{name}")
    @Secured(action = ActionTypes.WRITE, signType = SignType.AI, apiType = ApiType.ADMIN_API)
    public Result<String> deleteSkill(
        @RequestParam(value = "namespaceId", required = false, defaultValue = "public") String namespaceId,
        @PathVariable("name") String name) throws NacosException {
        registryService.deleteSkill(namespaceId, name);
        return Result.success("ok");
    }

    @Since("3.2.3")
    @GetMapping("/agents/{name}")
    @Secured(action = ActionTypes.READ, signType = SignType.AI, apiType = ApiType.ADMIN_API)
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
    @PostMapping("/agents")
    @Secured(action = ActionTypes.WRITE, signType = SignType.AI, apiType = ApiType.ADMIN_API)
    public Result<String> createAgent(
        @RequestParam(value = "namespaceId", required = false, defaultValue = "public") String namespaceId,
        @RequestParam(value = "publish", required = false, defaultValue = "false") boolean publish,
        @RequestParam(value = "force", required = false, defaultValue = "false") boolean force,
        @RequestBody AgentKitAgent agent) throws NacosException {
        String version = registryService.upsertAgentDraft(namespaceId, agent, null);
        if (publish) {
            version = registryService.publishAgent(namespaceId, agent.getMetadata().getName(), version, force);
        }
        return Result.success(version);
    }

    @Since("3.2.3")
    @PutMapping("/agents/{name}")
    @Secured(action = ActionTypes.WRITE, signType = SignType.AI, apiType = ApiType.ADMIN_API)
    public Result<String> updateAgent(
        @RequestParam(value = "namespaceId", required = false, defaultValue = "public") String namespaceId,
        @PathVariable("name") String name,
        @RequestParam(value = "publish", required = false, defaultValue = "false") boolean publish,
        @RequestParam(value = "force", required = false, defaultValue = "false") boolean force,
        @RequestHeader(value = "If-Match", required = false) String ifMatch,
        @RequestBody AgentKitAgent agent) throws NacosException {
        if (agent.getMetadata() != null && StringUtils.isBlank(agent.getMetadata().getName())) {
            agent.getMetadata().setName(name);
        }
        String version = registryService.upsertAgentDraft(namespaceId, agent, ifMatch);
        if (publish) {
            version = registryService.publishAgent(namespaceId, name, version, force);
        }
        return Result.success(version);
    }

    @Since("3.2.3")
    @PostMapping("/agents/{name}/publish")
    @Secured(action = ActionTypes.WRITE, signType = SignType.AI, apiType = ApiType.ADMIN_API)
    public Result<String> publishAgent(
        @RequestParam(value = "namespaceId", required = false, defaultValue = "public") String namespaceId,
        @PathVariable("name") String name,
        @RequestParam(value = "version", required = false) String version,
        @RequestParam(value = "force", required = false, defaultValue = "false") boolean force)
        throws NacosException {
        return Result.success(registryService.publishAgent(namespaceId, name, version, force));
    }

    @Since("3.2.3")
    @DeleteMapping("/agents/{name}")
    @Secured(action = ActionTypes.WRITE, signType = SignType.AI, apiType = ApiType.ADMIN_API)
    public Result<String> deleteAgent(
        @RequestParam(value = "namespaceId", required = false, defaultValue = "public") String namespaceId,
        @PathVariable("name") String name) throws NacosException {
        registryService.deleteAgent(namespaceId, name);
        return Result.success("ok");
    }

    @Since("3.2.3")
    @GetMapping("/agents/{name}/card")
    @Secured(action = ActionTypes.READ, signType = SignType.AI, apiType = ApiType.ADMIN_API)
    public Result<Map<String, Object>> getAgentCard(
        @RequestParam(value = "namespaceId", required = false, defaultValue = "public") String namespaceId,
        @PathVariable("name") String name,
        @RequestParam(value = "version", required = false) String version,
        @RequestParam(value = "label", required = false) String label,
        @RequestParam(value = "endpointUrl", required = false) String endpointUrl) throws NacosException {
        return Result.success(registryService.getAgentCard(namespaceId, name, version, label, endpointUrl));
    }
}
