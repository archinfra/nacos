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

package com.alibaba.nacos.api.ai.model.skills;

import java.util.List;

/**
 * Base class for Skill model objects. Contains common basic info fields shared across Skill-related models.
 *
 * @author nacos
 * @since 3.2.0
 */
public class SkillBase {

    private String namespaceId;

    private String name;

    private String description;

    /** Logical skill set name for grouping skills in console and runtime manifests. */
    private String skillSet;

    /** Display/search groups, for example: novel, dialogue, ops, database. */
    private List<String> groups;

    /** Search and intent matching keywords. */
    private List<String> keywords;

    /** Model-facing skill name. Falls back to name/display name when empty. */
    private String modelName;

    /** Model-facing description. Falls back to description when empty. */
    private String modelDescription;

    /** Runtime matching hint for router/LLM selection. */
    private String matchHint;

    /** Runtime activation strategy, for example always/on_intent/manual. */
    private String activation;

    /** Runtime priority, lower value can be rendered earlier by the caller. */
    private Integer priority;

    public String getNamespaceId() {
        return namespaceId;
    }

    public void setNamespaceId(String namespaceId) {
        this.namespaceId = namespaceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
