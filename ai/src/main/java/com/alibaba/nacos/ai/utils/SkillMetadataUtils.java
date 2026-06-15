/*
 * Copyright 1999-2026 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.nacos.ai.utils;

import com.alibaba.nacos.api.ai.model.skills.SkillBase;
import com.alibaba.nacos.common.utils.JacksonUtils;
import com.alibaba.nacos.common.utils.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utilities for skill grouping / runtime metadata.
 *
 * <p>Metadata is persisted in {@code ai_resource.ext}; groups and keywords are also mirrored into
 * {@code ai_resource.biz_tags} so existing fuzzy-search filters continue to work without schema changes.</p>
 *
 * @author nacos
 */
public final class SkillMetadataUtils {

    public static final String FIELD_SKILL_SET = "skillSet";

    public static final String FIELD_SKILL_SET_KEBAB = "skill-set";

    public static final String FIELD_GROUPS = "groups";

    public static final String FIELD_GROUP = "group";

    public static final String FIELD_KEYWORDS = "keywords";

    public static final String FIELD_MODEL_NAME = "modelName";

    public static final String FIELD_MODEL_DESCRIPTION = "modelDescription";

    public static final String FIELD_MATCH_HINT = "matchHint";

    public static final String FIELD_ACTIVATION = "activation";

    public static final String FIELD_PRIORITY = "priority";

    private SkillMetadataUtils() {
    }

    public static void applyFromSkillMd(SkillBase skill, String skillMd) {
        if (skill == null || StringUtils.isBlank(skillMd)) {
            return;
        }
        applyFromFrontMatter(skill, SkillZipParser.parseYamlFrontMatterFromMarkdown(skillMd));
    }

    public static void applyFromFrontMatter(SkillBase skill, Map<String, String> frontMatter) {
        if (skill == null || frontMatter == null || frontMatter.isEmpty()) {
            return;
        }
        String skillSet = firstNonBlank(frontMatter.get(FIELD_SKILL_SET),
            frontMatter.get(FIELD_SKILL_SET_KEBAB));
        if (StringUtils.isNotBlank(skillSet)) {
            skill.setSkillSet(skillSet.trim());
        }
        List<String> groups = firstNonEmptyList(frontMatter.get(FIELD_GROUPS),
            frontMatter.get(FIELD_GROUP));
        if (!groups.isEmpty()) {
            skill.setGroups(groups);
        }
        List<String> keywords = parseStringList(frontMatter.get(FIELD_KEYWORDS));
        if (!keywords.isEmpty()) {
            skill.setKeywords(keywords);
        }
        copyScalar(frontMatter, FIELD_MODEL_NAME, skill::setModelName);
        copyScalar(frontMatter, FIELD_MODEL_DESCRIPTION, skill::setModelDescription);
        copyScalar(frontMatter, FIELD_MATCH_HINT, skill::setMatchHint);
        copyScalar(frontMatter, FIELD_ACTIVATION, skill::setActivation);
        Integer priority = parseInteger(frontMatter.get(FIELD_PRIORITY));
        if (priority != null) {
            skill.setPriority(priority);
        }
    }

    public static void applyFromExt(SkillBase skill, String ext) {
        if (skill == null || StringUtils.isBlank(ext)) {
            return;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> meta = JacksonUtils.toObj(ext, Map.class);
            if (meta == null || meta.isEmpty()) {
                return;
            }
            Object skillSet = meta.get(FIELD_SKILL_SET);
            if (skillSet instanceof String && StringUtils.isNotBlank((String) skillSet)) {
                skill.setSkillSet(((String) skillSet).trim());
            }
            List<String> groups = parseObjectList(meta.get(FIELD_GROUPS));
            if (!groups.isEmpty()) {
                skill.setGroups(groups);
            }
            List<String> keywords = parseObjectList(meta.get(FIELD_KEYWORDS));
            if (!keywords.isEmpty()) {
                skill.setKeywords(keywords);
            }
            copyObjectScalar(meta, FIELD_MODEL_NAME, skill::setModelName);
            copyObjectScalar(meta, FIELD_MODEL_DESCRIPTION, skill::setModelDescription);
            copyObjectScalar(meta, FIELD_MATCH_HINT, skill::setMatchHint);
            copyObjectScalar(meta, FIELD_ACTIVATION, skill::setActivation);
            Object priority = meta.get(FIELD_PRIORITY);
            if (priority instanceof Number) {
                skill.setPriority(((Number) priority).intValue());
            } else if (priority instanceof String) {
                skill.setPriority(parseInteger((String) priority));
            }
        } catch (Exception ignored) {
        }
    }

    public static String buildExt(SkillBase skill) {
        if (skill == null) {
            return null;
        }
        Map<String, Object> meta = new LinkedHashMap<>(8);
        putIfNotBlank(meta, FIELD_SKILL_SET, skill.getSkillSet());
        putIfNotEmpty(meta, FIELD_GROUPS, skill.getGroups());
        putIfNotEmpty(meta, FIELD_KEYWORDS, skill.getKeywords());
        putIfNotBlank(meta, FIELD_MODEL_NAME, skill.getModelName());
        putIfNotBlank(meta, FIELD_MODEL_DESCRIPTION, skill.getModelDescription());
        putIfNotBlank(meta, FIELD_MATCH_HINT, skill.getMatchHint());
        putIfNotBlank(meta, FIELD_ACTIVATION, skill.getActivation());
        if (skill.getPriority() != null) {
            meta.put(FIELD_PRIORITY, skill.getPriority());
        }
        return meta.isEmpty() ? null : JacksonUtils.toJson(meta);
    }

    public static String buildBizTags(SkillBase skill) {
        Set<String> tags = new LinkedHashSet<>();
        if (skill != null) {
            addTag(tags, skill.getSkillSet());
            addTags(tags, skill.getGroups());
            addTags(tags, skill.getKeywords());
        }
        return tags.isEmpty() ? null : JacksonUtils.toJson(new ArrayList<>(tags));
    }

    public static List<String> parseStringList(String raw) {
        List<String> result = new ArrayList<>();
        if (StringUtils.isBlank(raw)) {
            return result;
        }
        String value = raw.trim();
        if (value.startsWith("[") && value.endsWith("]")) {
            value = value.substring(1, value.length() - 1);
        }
        for (String item : value.split(",")) {
            String normalized = stripQuotes(item.trim());
            if (StringUtils.isNotBlank(normalized) && !result.contains(normalized)) {
                result.add(normalized);
            }
        }
        return result;
    }

    private static List<String> firstNonEmptyList(String first, String second) {
        List<String> result = parseStringList(first);
        if (result.isEmpty()) {
            result = parseStringList(second);
        }
        return result;
    }

    private static List<String> parseObjectList(Object raw) {
        List<String> result = new ArrayList<>();
        if (raw instanceof List) {
            for (Object item : (List<?>) raw) {
                if (item == null) {
                    continue;
                }
                String value = String.valueOf(item).trim();
                if (StringUtils.isNotBlank(value) && !result.contains(value)) {
                    result.add(value);
                }
            }
            return result;
        }
        if (raw instanceof String) {
            return parseStringList((String) raw);
        }
        return result;
    }

    private static void copyScalar(Map<String, String> map, String key, java.util.function.Consumer<String> consumer) {
        String value = map.get(key);
        if (StringUtils.isNotBlank(value)) {
            consumer.accept(value.trim());
        }
    }

    private static void copyObjectScalar(Map<String, Object> map, String key, java.util.function.Consumer<String> consumer) {
        Object value = map.get(key);
        if (value instanceof String && StringUtils.isNotBlank((String) value)) {
            consumer.accept(((String) value).trim());
        }
    }

    private static void putIfNotBlank(Map<String, Object> map, String key, String value) {
        if (StringUtils.isNotBlank(value)) {
            map.put(key, value.trim());
        }
    }

    private static void putIfNotEmpty(Map<String, Object> map, String key, List<String> value) {
        if (value != null && !value.isEmpty()) {
            map.put(key, value);
        }
    }

    private static void addTags(Set<String> target, List<String> tags) {
        if (tags == null) {
            return;
        }
        for (String tag : tags) {
            addTag(target, tag);
        }
    }

    private static void addTag(Set<String> target, String tag) {
        if (StringUtils.isBlank(tag)) {
            return;
        }
        target.add(tag.trim());
    }

    private static String firstNonBlank(String first, String second) {
        return StringUtils.isNotBlank(first) ? first : second;
    }

    private static Integer parseInteger(String raw) {
        if (StringUtils.isBlank(raw)) {
            return null;
        }
        try {
            return Integer.valueOf(raw.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String stripQuotes(String value) {
        if (value == null) {
            return null;
        }
        if (value.length() >= 2
            && ((value.startsWith("\"") && value.endsWith("\""))
            || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length() - 1).trim();
        }
        return value;
    }
}
