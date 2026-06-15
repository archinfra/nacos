/*
 * Copyright 1999-2026 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.alibaba.nacos.ai.service.registry;

import com.alibaba.nacos.ai.constant.Constants;
import com.alibaba.nacos.ai.service.agentspecs.AgentSpecOperationService;
import com.alibaba.nacos.ai.service.skills.SkillOperationService;
import com.alibaba.nacos.ai.utils.registry.AgentKitAgentCodec;
import com.alibaba.nacos.ai.utils.registry.AgentKitDigestUtils;
import com.alibaba.nacos.ai.utils.registry.AgentKitSkillCodec;
import com.alibaba.nacos.api.ai.model.agentspecs.AgentSpec;
import com.alibaba.nacos.api.ai.model.agentspecs.AgentSpecMeta;
import com.alibaba.nacos.api.ai.model.registry.AgentKitAgent;
import com.alibaba.nacos.api.ai.model.registry.AgentKitRuntimeManifest;
import com.alibaba.nacos.api.ai.model.registry.AgentKitSkill;
import com.alibaba.nacos.api.ai.model.registry.AgentKitSkillRef;
import com.alibaba.nacos.api.ai.model.skills.Skill;
import com.alibaba.nacos.api.ai.model.skills.SkillMeta;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.exception.api.NacosApiException;
import com.alibaba.nacos.api.model.v2.ErrorCode;
import com.alibaba.nacos.common.utils.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Default AgentKit registry service implemented as an adapter over existing Nacos Skill and AgentSpec services.
 *
 * @author nacos
 */
@Service
public class AgentKitRegistryServiceImpl implements AgentKitRegistryService {

    private final SkillOperationService skillOperationService;

    private final AgentSpecOperationService agentSpecOperationService;

    public AgentKitRegistryServiceImpl(SkillOperationService skillOperationService,
        AgentSpecOperationService agentSpecOperationService) {
        this.skillOperationService = skillOperationService;
        this.agentSpecOperationService = agentSpecOperationService;
    }

    @Override
    public AgentKitSkill getSkill(String namespaceId, String name, String version, String label)
        throws NacosException {
        Skill skill;
        String resolvedVersion = version;
        if (StringUtils.isNotBlank(version)) {
            skill = skillOperationService.getSkillVersionDetail(namespaceId, name, version);
        } else {
            skill = skillOperationService.querySkill(namespaceId, name, null,
                firstNotBlank(label, Constants.Registry.LABEL_LATEST));
            SkillMeta meta = skillOperationService.getSkillDetail(namespaceId, name);
            if (meta.getLabels() != null) {
                resolvedVersion = meta.getLabels().get(firstNotBlank(label, Constants.Registry.LABEL_LATEST));
            }
        }
        AgentKitSkill result = AgentKitSkillCodec.fromNacosSkill(skill, resolvedVersion, null);
        if (result.getMetadata() != null) {
            result.getMetadata().setNamespace(namespaceId);
        }
        return result;
    }

    @Override
    public String upsertSkillDraft(String namespaceId, AgentKitSkill skill, String ifMatch)
        throws NacosException {
        validateSkill(skill);
        String name = skill.getMetadata().getName();
        verifySkillIfMatch(namespaceId, name, ifMatch);
        Skill nacosSkill = AgentKitSkillCodec.toNacosSkill(skill, namespaceId);
        String targetVersion = skill.getMetadata().getVersion();
        SkillMeta meta = null;
        try {
            meta = skillOperationService.getSkillDetail(namespaceId, name);
        } catch (NacosException e) {
            if (e.getErrCode() != NacosException.NOT_FOUND) {
                throw e;
            }
        }
        if (meta == null) {
            return skillOperationService.createDraft(namespaceId, name, null, targetVersion,
                nacosSkill, "registry create skill draft");
        }
        if (StringUtils.isBlank(meta.getEditingVersion())) {
            skillOperationService.createDraft(namespaceId, name, null, targetVersion, null,
                "registry fork skill draft");
        }
        skillOperationService.updateDraft(namespaceId, nacosSkill, "registry update skill draft");
        SkillMeta updated = skillOperationService.getSkillDetail(namespaceId, name);
        return updated.getEditingVersion();
    }

    @Override
    public String publishSkill(String namespaceId, String name, String version, boolean force)
        throws NacosException {
        String target = resolveSkillWorkingVersion(namespaceId, name, version);
        if (force) {
            skillOperationService.forcePublish(namespaceId, name, target, true);
            return target;
        }
        return skillOperationService.submit(namespaceId, name, target);
    }

    @Override
    public void deleteSkill(String namespaceId, String name) throws NacosException {
        skillOperationService.deleteSkill(namespaceId, name);
    }

    @Override
    public AgentKitAgent getAgent(String namespaceId, String name, String version, String label)
        throws NacosException {
        AgentSpec agentSpec;
        String resolvedVersion = version;
        if (StringUtils.isNotBlank(version)) {
            agentSpec = agentSpecOperationService.getAgentSpecVersionDetail(namespaceId, name, version);
        } else {
            agentSpec = agentSpecOperationService.queryAgentSpec(namespaceId, name, null,
                firstNotBlank(label, Constants.Registry.LABEL_LATEST));
            AgentSpecMeta meta = agentSpecOperationService.getAgentSpecDetail(namespaceId, name);
            if (meta.getLabels() != null) {
                resolvedVersion = meta.getLabels().get(firstNotBlank(label, Constants.Registry.LABEL_LATEST));
            }
        }
        AgentKitAgent result = AgentKitAgentCodec.fromNacosAgentSpec(agentSpec, resolvedVersion, null);
        if (result.getMetadata() != null) {
            result.getMetadata().setNamespace(namespaceId);
        }
        return result;
    }

    @Override
    public String upsertAgentDraft(String namespaceId, AgentKitAgent agent, String ifMatch)
        throws NacosException {
        validateAgent(agent);
        String name = agent.getMetadata().getName();
        verifyAgentIfMatch(namespaceId, name, ifMatch);
        AgentSpec nacosAgentSpec = AgentKitAgentCodec.toNacosAgentSpec(agent, namespaceId);
        String targetVersion = agent.getMetadata().getVersion();
        AgentSpecMeta meta = null;
        try {
            meta = agentSpecOperationService.getAgentSpecDetail(namespaceId, name);
        } catch (NacosException e) {
            if (e.getErrCode() != NacosException.NOT_FOUND) {
                throw e;
            }
        }
        if (meta != null && StringUtils.isBlank(meta.getEditingVersion())) {
            agentSpecOperationService.createDraft(namespaceId, name, null, targetVersion);
        }
        agentSpecOperationService.updateDraft(namespaceId, nacosAgentSpec);
        AgentSpecMeta updated = agentSpecOperationService.getAgentSpecDetail(namespaceId, name);
        return updated.getEditingVersion();
    }

    @Override
    public String publishAgent(String namespaceId, String name, String version, boolean force)
        throws NacosException {
        String target = resolveAgentWorkingVersion(namespaceId, name, version);
        if (force) {
            agentSpecOperationService.forcePublish(namespaceId, name, target, true);
            return target;
        }
        return agentSpecOperationService.submit(namespaceId, name, target);
    }

    @Override
    public void deleteAgent(String namespaceId, String name) throws NacosException {
        agentSpecOperationService.deleteAgentSpec(namespaceId, name);
    }

    @Override
    public AgentKitRuntimeManifest getRuntimeManifest(String namespaceId, String name,
        String version, String label) throws NacosException {
        AgentKitAgent agent = getAgent(namespaceId, name, version, label);
        List<AgentKitSkill> skills = new ArrayList<>();
        if (agent.getSpec() != null && agent.getSpec().getSkills() != null) {
            for (AgentKitSkillRef ref : agent.getSpec().getSkills()) {
                if (ref == null || StringUtils.isBlank(ref.getName())) {
                    continue;
                }
                skills.add(getSkill(namespaceId, ref.getName(), ref.getVersion(),
                    firstNotBlank(ref.getLabel(), Constants.Registry.LABEL_LATEST)));
            }
        }
        AgentKitRuntimeManifest manifest = new AgentKitRuntimeManifest();
        manifest.setAgent(agent);
        manifest.setSkills(skills);
        manifest.setDigest(AgentKitDigestUtils.md5(manifest));
        manifest.setResourceVersion(manifest.getDigest());
        return manifest;
    }

    @Override
    public Map<String, Object> getAgentCard(String namespaceId, String name, String version,
        String label, String endpointUrl) throws NacosException {
        return AgentKitAgentCodec.toA2aAgentCard(getAgent(namespaceId, name, version, label), endpointUrl);
    }

    private void verifySkillIfMatch(String namespaceId, String name, String ifMatch)
        throws NacosException {
        if (StringUtils.isBlank(ifMatch)) {
            return;
        }
        try {
            AgentKitSkill current = getSkill(namespaceId, name, null, Constants.Registry.LABEL_LATEST);
            verifyMatch(current.getMetadata().getDigest(), ifMatch);
        } catch (NacosException e) {
            if (e.getErrCode() != NacosException.NOT_FOUND) {
                throw e;
            }
        }
    }

    private void verifyAgentIfMatch(String namespaceId, String name, String ifMatch)
        throws NacosException {
        if (StringUtils.isBlank(ifMatch)) {
            return;
        }
        try {
            AgentKitAgent current = getAgent(namespaceId, name, null, Constants.Registry.LABEL_LATEST);
            verifyMatch(current.getMetadata().getDigest(), ifMatch);
        } catch (NacosException e) {
            if (e.getErrCode() != NacosException.NOT_FOUND) {
                throw e;
            }
        }
    }

    private void verifyMatch(String currentDigest, String ifMatch) throws NacosException {
        String expected = AgentKitDigestUtils.unquote(ifMatch);
        if (StringUtils.isNotBlank(expected) && !expected.equals(currentDigest)) {
            throw new NacosApiException(NacosException.CONFLICT, ErrorCode.RESOURCE_CONFLICT,
                "Registry resource changed, please reload and retry");
        }
    }

    private String resolveSkillWorkingVersion(String namespaceId, String name, String version)
        throws NacosException {
        if (StringUtils.isNotBlank(version)) {
            return version;
        }
        SkillMeta meta = skillOperationService.getSkillDetail(namespaceId, name);
        return firstNotBlank(meta.getEditingVersion(), meta.getReviewingVersion());
    }

    private String resolveAgentWorkingVersion(String namespaceId, String name, String version)
        throws NacosException {
        if (StringUtils.isNotBlank(version)) {
            return version;
        }
        AgentSpecMeta meta = agentSpecOperationService.getAgentSpecDetail(namespaceId, name);
        return firstNotBlank(meta.getEditingVersion(), meta.getReviewingVersion());
    }

    private void validateSkill(AgentKitSkill skill) throws NacosException {
        if (skill == null || skill.getMetadata() == null || StringUtils.isBlank(skill.getMetadata().getName())) {
            throw new NacosApiException(NacosException.INVALID_PARAM, ErrorCode.PARAMETER_MISSING,
                "metadata.name is required");
        }
        if (skill.getSpec() == null || StringUtils.isBlank(skill.getSpec().getDescription())) {
            throw new NacosApiException(NacosException.INVALID_PARAM, ErrorCode.PARAMETER_MISSING,
                "spec.description is required");
        }
    }

    private void validateAgent(AgentKitAgent agent) throws NacosException {
        if (agent == null || agent.getMetadata() == null || StringUtils.isBlank(agent.getMetadata().getName())) {
            throw new NacosApiException(NacosException.INVALID_PARAM, ErrorCode.PARAMETER_MISSING,
                "metadata.name is required");
        }
        if (agent.getSpec() == null) {
            throw new NacosApiException(NacosException.INVALID_PARAM, ErrorCode.PARAMETER_MISSING,
                "spec is required");
        }
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }
}
