/*
 * Copyright 1999-2026 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.alibaba.nacos.ai.utils.registry;

import com.alibaba.nacos.ai.constant.Constants;
import com.alibaba.nacos.api.ai.model.agentspecs.AgentSpec;
import com.alibaba.nacos.api.ai.model.registry.AgentKitAgent;
import com.alibaba.nacos.api.ai.model.registry.AgentKitAgentSpec;
import com.alibaba.nacos.api.ai.model.registry.AgentKitMetadata;
import com.alibaba.nacos.api.ai.model.registry.AgentKitSkillRef;
import com.alibaba.nacos.api.ai.model.registry.AgentKitSubAgentRef;
import com.alibaba.nacos.api.ai.model.registry.AgentKitToolRef;
import com.alibaba.nacos.common.utils.JacksonUtils;
import com.alibaba.nacos.common.utils.StringUtils;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Codec between normalized AgentKit Agent JSON and existing Nacos AgentSpec storage.
 *
 * @author nacos
 */
public final class AgentKitAgentCodec {

    private AgentKitAgentCodec() {
    }

    public static AgentKitAgent fromNacosAgentSpec(AgentSpec agentSpec, String version, String status) {
        AgentKitAgent parsed = tryParseNormalized(agentSpec.getContent());
        if (parsed != null) {
            enrichMetadata(parsed, agentSpec, version, status);
            return parsed;
        }
        AgentKitAgent result = new AgentKitAgent();
        result.setApiVersion(Constants.Registry.API_VERSION);
        result.setKind(Constants.Registry.KIND_AGENT);
        AgentKitMetadata metadata = new AgentKitMetadata();
        metadata.setNamespace(agentSpec.getNamespaceId());
        metadata.setName(agentSpec.getName());
        metadata.setDisplayName(agentSpec.getName());
        metadata.setVersion(version);
        metadata.setStatus(status);
        result.setMetadata(metadata);
        AgentKitAgentSpec spec = parseLegacySpec(agentSpec.getContent());
        spec.setDescription(firstNotBlank(spec.getDescription(), agentSpec.getDescription()));
        result.setSpec(spec);
        metadata.setDigest(AgentKitDigestUtils.md5(result));
        metadata.setResourceVersion(metadata.getDigest());
        return result;
    }

    public static AgentSpec toNacosAgentSpec(AgentKitAgent resource, String namespaceId) {
        AgentKitMetadata metadata = resource.getMetadata() == null ? new AgentKitMetadata() : resource.getMetadata();
        AgentKitAgentSpec spec = resource.getSpec() == null ? new AgentKitAgentSpec() : resource.getSpec();
        AgentSpec result = new AgentSpec();
        result.setNamespaceId(firstNotBlank(namespaceId, metadata.getNamespace()));
        result.setName(metadata.getName());
        result.setDescription(spec.getDescription());
        result.setBizTags(toBizTags(metadata, spec));
        normalize(resource);
        result.setContent(JacksonUtils.toJson(resource));
        return result;
    }

    public static Map<String, Object> toA2aAgentCard(AgentKitAgent agent, String endpointUrl) {
        Map<String, Object> card = new LinkedHashMap<>();
        AgentKitMetadata metadata = agent.getMetadata();
        AgentKitAgentSpec spec = agent.getSpec();
        card.put("name", metadata == null ? null : metadata.getName());
        card.put("description", spec == null ? null : spec.getDescription());
        card.put("version", metadata == null ? null : metadata.getVersion());
        Map<String, Object> capabilities = new LinkedHashMap<>();
        capabilities.put("streaming", true);
        capabilities.put("extendedAgentCard", true);
        card.put("capabilities", capabilities);
        List<Map<String, Object>> skills = new ArrayList<>();
        if (spec != null && spec.getSkills() != null) {
            for (AgentKitSkillRef ref : spec.getSkills()) {
                if (ref == null || StringUtils.isBlank(ref.getName())) {
                    continue;
                }
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", ref.getName());
                item.put("name", ref.getName());
                item.put("version", firstNotBlank(ref.getVersion(), ref.getLabel(), Constants.Registry.LABEL_LATEST));
                skills.add(item);
            }
        }
        card.put("skills", skills);
        if (StringUtils.isNotBlank(endpointUrl)) {
            List<Map<String, Object>> interfaces = new ArrayList<>();
            Map<String, Object> iface = new LinkedHashMap<>();
            iface.put("transport", "http+json");
            iface.put("url", endpointUrl);
            interfaces.add(iface);
            card.put("supportedInterfaces", interfaces);
        }
        return card;
    }

    private static void normalize(AgentKitAgent resource) {
        if (StringUtils.isBlank(resource.getApiVersion())) {
            resource.setApiVersion(Constants.Registry.API_VERSION);
        }
        if (StringUtils.isBlank(resource.getKind())) {
            resource.setKind(Constants.Registry.KIND_AGENT);
        }
        if (resource.getMetadata() == null) {
            resource.setMetadata(new AgentKitMetadata());
        }
    }

    private static AgentKitAgent tryParseNormalized(String content) {
        if (StringUtils.isBlank(content)) {
            return null;
        }
        try {
            AgentKitAgent parsed = JacksonUtils.toObj(content, AgentKitAgent.class);
            if (parsed != null && Constants.Registry.KIND_AGENT.equals(parsed.getKind())
                && parsed.getSpec() != null) {
                return parsed;
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private static AgentKitAgentSpec parseLegacySpec(String content) {
        AgentKitAgentSpec spec = new AgentKitAgentSpec();
        if (StringUtils.isBlank(content)) {
            return spec;
        }
        try {
            Map<String, Object> map = JacksonUtils.toObj(content,
                new TypeReference<Map<String, Object>>() {
                });
            if (map == null || map.isEmpty()) {
                return spec;
            }
            spec.setAgentClass(asString(first(map, "agentClass", "agent_class")));
            spec.setDescription(asString(map.get("description")));
            spec.setModelRef(asString(first(map, "modelRef", "model")));
            spec.setInstruction(asString(map.get("instruction")));
            spec.setGenerationConfig(asMap(first(map, "generationConfig", "generate_content_config")));
            spec.setSkills(parseSkills(first(map, "skills", "skillRefs")));
            spec.setTools(parseTools(map.get("tools")));
            spec.setSubAgents(parseSubAgents(first(map, "subAgents", "sub_agents")));
            spec.setRuntime(asMap(map.get("runtime")));
        } catch (Exception ignored) {
            spec.setRuntime(new LinkedHashMap<>());
            spec.getRuntime().put("legacyContent", content);
        }
        return spec;
    }

    private static void enrichMetadata(AgentKitAgent agent, AgentSpec agentSpec, String version, String status) {
        normalize(agent);
        AgentKitMetadata metadata = agent.getMetadata();
        if (StringUtils.isBlank(metadata.getNamespace())) {
            metadata.setNamespace(agentSpec.getNamespaceId());
        }
        if (StringUtils.isBlank(metadata.getName())) {
            metadata.setName(agentSpec.getName());
        }
        if (StringUtils.isBlank(metadata.getVersion())) {
            metadata.setVersion(version);
        }
        if (StringUtils.isBlank(metadata.getStatus())) {
            metadata.setStatus(status);
        }
        if (agent.getSpec() != null && StringUtils.isBlank(agent.getSpec().getDescription())) {
            agent.getSpec().setDescription(agentSpec.getDescription());
        }
        metadata.setDigest(AgentKitDigestUtils.md5(agent));
        metadata.setResourceVersion(metadata.getDigest());
    }

    private static List<AgentKitSkillRef> parseSkills(Object value) {
        if (!(value instanceof List)) {
            return null;
        }
        List<?> items = (List<?>) value;
        List<AgentKitSkillRef> result = new ArrayList<>();
        for (Object item : items) {
            AgentKitSkillRef ref = new AgentKitSkillRef();
            if (item instanceof String) {
                ref.setName((String) item);
                ref.setLabel(Constants.Registry.LABEL_LATEST);
            } else if (item instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) item;
                ref.setName(asString(first(map, "name", "id")));
                ref.setVersion(asString(map.get("version")));
                ref.setLabel(asString(map.get("label")));
                ref.setActivation(asString(map.get("activation")));
                ref.setRefreshPolicy(asString(map.get("refreshPolicy")));
                ref.setRequired(asBoolean(map.get("required")));
                ref.setOrder(asInteger(map.get("order")));
            }
            if (StringUtils.isNotBlank(ref.getName())) {
                result.add(ref);
            }
        }
        return result.isEmpty() ? null : result;
    }

    private static List<AgentKitToolRef> parseTools(Object value) {
        if (!(value instanceof List)) {
            return null;
        }
        List<?> items = (List<?>) value;
        List<AgentKitToolRef> result = new ArrayList<>();
        for (Object item : items) {
            if (!(item instanceof Map)) {
                continue;
            }
            Map<?, ?> map = (Map<?, ?>) item;
            AgentKitToolRef ref = new AgentKitToolRef();
            ref.setType(firstNotBlank(asString(map.get("type")), inferToolType(map)));
            ref.setName(asString(map.get("name")));
            ref.setServer(asString(map.get("server")));
            ref.setToolFilter(asStringList(first(map, "toolFilter", "tool_filter")));
            ref.setArgs(asMap(map.get("args")));
            result.add(ref);
        }
        return result.isEmpty() ? null : result;
    }

    private static List<AgentKitSubAgentRef> parseSubAgents(Object value) {
        if (!(value instanceof List)) {
            return null;
        }
        List<?> items = (List<?>) value;
        List<AgentKitSubAgentRef> result = new ArrayList<>();
        for (Object item : items) {
            if (!(item instanceof Map)) {
                continue;
            }
            Map<?, ?> map = (Map<?, ?>) item;
            AgentKitSubAgentRef sub = new AgentKitSubAgentRef();
            sub.setId(asString(map.get("id")));
            Object refObj = map.get("ref");
            AgentKitSkillRef ref = new AgentKitSkillRef();
            if (refObj instanceof String) {
                ref.setName((String) refObj);
                ref.setLabel(Constants.Registry.LABEL_LATEST);
            } else if (refObj instanceof Map) {
                Map<?, ?> refMap = (Map<?, ?>) refObj;
                ref.setName(asString(refMap.get("name")));
                ref.setVersion(asString(refMap.get("version")));
                ref.setLabel(asString(refMap.get("label")));
            }
            sub.setRef(ref);
            sub.setInvocation(asMap(map.get("invocation")));
            sub.setContext(asMap(map.get("context")));
            sub.setWorkspace(asMap(map.get("workspace")));
            sub.setOutput(asMap(map.get("output")));
            result.add(sub);
        }
        return result.isEmpty() ? null : result;
    }

    private static String toBizTags(AgentKitMetadata metadata, AgentKitAgentSpec spec) {
        List<String> tags = new ArrayList<>();
        if (metadata.getLabels() != null) {
            tags.addAll(metadata.getLabels().values());
        }
        if (spec != null && spec.getSkills() != null) {
            for (AgentKitSkillRef skill : spec.getSkills()) {
                if (skill != null && StringUtils.isNotBlank(skill.getName())) {
                    tags.add("skill:" + skill.getName());
                }
            }
        }
        return tags.isEmpty() ? null : JacksonUtils.toJson(tags);
    }

    private static Object first(Map<?, ?> map, String... keys) {
        if (map == null) {
            return null;
        }
        for (String key : keys) {
            if (map.containsKey(key)) {
                return map.get(key);
            }
        }
        return null;
    }

    private static String inferToolType(Map<?, ?> map) {
        return map.containsKey("server") ? "mcp" : "builtin";
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : null;
    }

    private static List<String> asStringList(Object value) {
        if (!(value instanceof List)) {
            return null;
        }
        List<String> result = new ArrayList<>();
        for (Object item : (List<?>) value) {
            String v = asString(item);
            if (StringUtils.isNotBlank(v)) {
                result.add(v);
            }
        }
        return result;
    }

    private static Boolean asBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return value == null ? null : Boolean.valueOf(String.valueOf(value));
    }

    private static Integer asInteger(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String firstNotBlank(String... values) {
        return Arrays.stream(values).filter(StringUtils::isNotBlank).findFirst().orElse(null);
    }
}
