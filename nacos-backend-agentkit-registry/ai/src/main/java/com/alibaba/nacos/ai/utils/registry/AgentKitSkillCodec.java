/*
 * Copyright 1999-2026 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.alibaba.nacos.ai.utils.registry;

import com.alibaba.nacos.ai.constant.Constants;
import com.alibaba.nacos.ai.utils.SkillZipParser;
import com.alibaba.nacos.api.ai.model.registry.AgentKitMetadata;
import com.alibaba.nacos.api.ai.model.registry.AgentKitResourceRef;
import com.alibaba.nacos.api.ai.model.registry.AgentKitSkill;
import com.alibaba.nacos.api.ai.model.registry.AgentKitSkillSpec;
import com.alibaba.nacos.api.ai.model.skills.Skill;
import com.alibaba.nacos.api.ai.model.skills.SkillResource;
import com.alibaba.nacos.common.utils.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Codec between portable AgentKit Skill JSON and existing Nacos Skill/SKILL.md storage.
 *
 * @author nacos
 */
public final class AgentKitSkillCodec {

    private static final Pattern YAML_FRONT_MATTER = Pattern.compile(
        "^---\\s*\\n(.*?)\\n---\\s*\\n(.*)$", Pattern.DOTALL);

    private AgentKitSkillCodec() {
    }

    public static AgentKitSkill fromNacosSkill(Skill skill, String version, String status) {
        AgentKitSkill result = new AgentKitSkill();
        result.setApiVersion(Constants.Registry.API_VERSION);
        result.setKind(Constants.Registry.KIND_SKILL);
        AgentKitMetadata metadata = new AgentKitMetadata();
        metadata.setNamespace(skill.getNamespaceId());
        metadata.setName(skill.getName());
        metadata.setDisplayName(firstNotBlank(skill.getModelName(), skill.getName()));
        metadata.setVersion(version);
        metadata.setStatus(status);
        result.setMetadata(metadata);
        AgentKitSkillSpec spec = new AgentKitSkillSpec();
        spec.setDescription(skill.getDescription());
        spec.setSkillSet(skill.getSkillSet());
        spec.setGroups(skill.getGroups());
        spec.setKeywords(skill.getKeywords());
        spec.setModelName(skill.getModelName());
        spec.setModelDescription(skill.getModelDescription());
        spec.setMatchHint(skill.getMatchHint());
        spec.setActivation(skill.getActivation());
        spec.setPriority(skill.getPriority());
        Map<String, String> frontMatter = SkillZipParser.parseYamlFrontMatterFromMarkdown(skill.getSkillMd());
        spec.setLicense(frontMatter.get("license"));
        spec.setCompatibility(frontMatter.get("compatibility"));
        spec.setAllowedTools(parseStringList(frontMatter.get("allowed-tools")));
        spec.setBody(extractBody(skill.getSkillMd()));
        spec.setResources(toRefs(skill.getResource()));
        result.setSpec(spec);
        metadata.setDigest(AgentKitDigestUtils.md5(result));
        metadata.setResourceVersion(metadata.getDigest());
        return result;
    }

    public static Skill toNacosSkill(AgentKitSkill resource, String namespaceId) {
        AgentKitMetadata metadata = resource.getMetadata() == null ? new AgentKitMetadata() : resource.getMetadata();
        AgentKitSkillSpec spec = resource.getSpec() == null ? new AgentKitSkillSpec() : resource.getSpec();
        Skill skill = new Skill();
        skill.setNamespaceId(firstNotBlank(namespaceId, metadata.getNamespace()));
        skill.setName(metadata.getName());
        skill.setDescription(spec.getDescription());
        skill.setSkillSet(spec.getSkillSet());
        skill.setGroups(spec.getGroups());
        skill.setKeywords(spec.getKeywords());
        skill.setModelName(spec.getModelName());
        skill.setModelDescription(spec.getModelDescription());
        skill.setMatchHint(spec.getMatchHint());
        skill.setActivation(spec.getActivation());
        skill.setPriority(spec.getPriority());
        skill.setSkillMd(toSkillMd(metadata, spec));
        skill.setResource(toResources(spec.getResources()));
        return skill;
    }

    public static String toSkillMd(AgentKitMetadata metadata, AgentKitSkillSpec spec) {
        StringBuilder yaml = new StringBuilder(256);
        yaml.append("---\n");
        appendYamlScalar(yaml, "name", metadata.getName());
        appendYamlScalar(yaml, "description", spec.getDescription());
        appendYamlScalar(yaml, "license", spec.getLicense());
        appendYamlScalar(yaml, "compatibility", spec.getCompatibility());
        if (spec.getAllowedTools() != null && !spec.getAllowedTools().isEmpty()) {
            yaml.append("allowed-tools:\n");
            for (String tool : spec.getAllowedTools()) {
                yaml.append("  - ").append(quoteYaml(tool)).append('\n');
            }
        }
        yaml.append("metadata:\n");
        appendYamlNestedScalar(yaml, "agentkit.version", metadata.getVersion());
        appendYamlNestedScalar(yaml, "agentkit.status", metadata.getStatus());
        appendYamlNestedScalar(yaml, "agentkit.visibility", metadata.getVisibility());
        appendYamlNestedScalar(yaml, "agentkit.skillSet", spec.getSkillSet());
        appendYamlNestedList(yaml, "agentkit.groups", spec.getGroups());
        appendYamlNestedList(yaml, "agentkit.keywords", spec.getKeywords());
        appendYamlNestedScalar(yaml, "agentkit.modelName", spec.getModelName());
        appendYamlNestedScalar(yaml, "agentkit.modelDescription", spec.getModelDescription());
        appendYamlNestedScalar(yaml, "agentkit.matchHint", spec.getMatchHint());
        appendYamlNestedScalar(yaml, "agentkit.activation", spec.getActivation());
        if (spec.getPriority() != null) {
            yaml.append("  agentkit.priority: ").append(spec.getPriority()).append('\n');
        }
        yaml.append("---\n");
        if (StringUtils.isNotBlank(spec.getBody())) {
            yaml.append(spec.getBody());
            if (!spec.getBody().endsWith("\n")) {
                yaml.append('\n');
            }
        }
        return yaml.toString();
    }

    private static List<AgentKitResourceRef> toRefs(Map<String, SkillResource> resources) {
        if (resources == null || resources.isEmpty()) {
            return null;
        }
        List<AgentKitResourceRef> refs = new ArrayList<>(resources.size());
        for (Map.Entry<String, SkillResource> entry : resources.entrySet()) {
            SkillResource resource = entry.getValue();
            AgentKitResourceRef ref = new AgentKitResourceRef();
            ref.setPath(entry.getKey());
            if (resource != null) {
                ref.setContentType(resource.getType());
                ref.setContent(resource.getContent());
                if (resource.getMetadata() != null) {
                    Object encoding = resource.getMetadata().get("encoding");
                    ref.setEncoding(encoding == null ? null : String.valueOf(encoding));
                }
            }
            refs.add(ref);
        }
        return refs;
    }

    private static Map<String, SkillResource> toResources(List<AgentKitResourceRef> refs) {
        if (refs == null || refs.isEmpty()) {
            return null;
        }
        Map<String, SkillResource> resources = new LinkedHashMap<>();
        for (AgentKitResourceRef ref : refs) {
            if (ref == null || StringUtils.isBlank(ref.getPath())) {
                continue;
            }
            SkillResource resource = new SkillResource();
            resource.setName(ref.getPath());
            resource.setType(ref.getContentType());
            resource.setContent(ref.getContent());
            if (StringUtils.isNotBlank(ref.getEncoding())) {
                Map<String, Object> metadata = new LinkedHashMap<>();
                metadata.put("encoding", ref.getEncoding());
                resource.setMetadata(metadata);
            }
            resources.put(ref.getPath(), resource);
        }
        return resources;
    }

    private static String extractBody(String skillMd) {
        if (StringUtils.isBlank(skillMd)) {
            return "";
        }
        Matcher matcher = YAML_FRONT_MATTER.matcher(skillMd);
        if (matcher.matches()) {
            return matcher.group(2);
        }
        return skillMd;
    }

    private static List<String> parseStringList(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        String[] split = trimmed.split("[, ]+");
        List<String> result = new ArrayList<>();
        for (String item : split) {
            String normalized = trimQuote(item);
            if (StringUtils.isNotBlank(normalized)) {
                result.add(normalized);
            }
        }
        return result.isEmpty() ? null : result;
    }

    private static String trimQuote(String value) {
        if (value == null) {
            return null;
        }
        String v = value.trim();
        if (v.length() >= 2 && ((v.startsWith("\"") && v.endsWith("\""))
            || (v.startsWith("'") && v.endsWith("'")))) {
            return v.substring(1, v.length() - 1);
        }
        return v;
    }

    private static void appendYamlScalar(StringBuilder yaml, String key, String value) {
        if (StringUtils.isNotBlank(value)) {
            yaml.append(key).append(": ").append(quoteYaml(value)).append('\n');
        }
    }

    private static void appendYamlNestedScalar(StringBuilder yaml, String key, String value) {
        if (StringUtils.isNotBlank(value)) {
            yaml.append("  ").append(key).append(": ").append(quoteYaml(value)).append('\n');
        }
    }

    private static void appendYamlNestedList(StringBuilder yaml, String key, List<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        yaml.append("  ").append(key).append(": [");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                yaml.append(", ");
            }
            yaml.append(quoteYaml(values.get(i)));
        }
        yaml.append("]\n");
    }

    private static String quoteYaml(String value) {
        if (value == null) {
            return "\"\"";
        }
        return '"' + value.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }

    private static String firstNotBlank(String... values) {
        return Arrays.stream(values).filter(StringUtils::isNotBlank).findFirst().orElse(null);
    }
}
