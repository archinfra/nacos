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

package com.alibaba.nacos.ai.form.skills.admin;

import com.alibaba.nacos.ai.utils.SkillMetadataUtils;
import com.alibaba.nacos.api.ai.model.skills.Skill;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.exception.api.NacosApiException;
import com.alibaba.nacos.api.model.v2.ErrorCode;
import com.alibaba.nacos.common.utils.StringUtils;

import java.io.Serial;

/**
 * Skill runtime metadata update form.
 *
 * @author nacos
 */
public class SkillMetadataUpdateForm extends SkillForm {

    @Serial
    private static final long serialVersionUID = 1L;

    private String skillSet;

    /**
     * JSON array string or comma-separated text.
     */
    private String groups;

    /**
     * JSON array string or comma-separated text.
     */
    private String keywords;

    private String modelName;

    private String modelDescription;

    private String matchHint;

    private String activation;

    private Integer priority;

    @Override
    public void validate() throws NacosApiException {
        fillDefaultNamespaceId();
        if (StringUtils.isBlank(getSkillName())) {
            throw new NacosApiException(NacosException.INVALID_PARAM, ErrorCode.PARAMETER_MISSING,
                "Request parameter `skillName` should not be blank.");
        }
    }

    public Skill toSkillMetadata() {
        Skill skill = new Skill();
        skill.setNamespaceId(getNamespaceId());
        skill.setName(getSkillName());
        skill.setSkillSet(skillSet);
        skill.setGroups(SkillMetadataUtils.parseStringList(groups));
        skill.setKeywords(SkillMetadataUtils.parseStringList(keywords));
        skill.setModelName(modelName);
        skill.setModelDescription(modelDescription);
        skill.setMatchHint(matchHint);
        skill.setActivation(activation);
        skill.setPriority(priority);
        return skill;
    }

    public String getSkillSet() {
        return skillSet;
    }

    public void setSkillSet(String skillSet) {
        this.skillSet = skillSet;
    }

    public String getGroups() {
        return groups;
    }

    public void setGroups(String groups) {
        this.groups = groups;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
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
