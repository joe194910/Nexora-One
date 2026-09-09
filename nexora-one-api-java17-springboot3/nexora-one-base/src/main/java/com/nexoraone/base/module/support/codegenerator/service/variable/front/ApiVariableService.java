package com.nexoraone.base.module.support.codegenerator.service.variable.front;

import com.nexoraone.base.module.support.codegenerator.domain.form.CodeGeneratorConfigForm;
import com.nexoraone.base.module.support.codegenerator.service.variable.CodeGenerateBaseVariableService;

import java.util.*;

/**
 * @Author NexoraOne-主任:卓大
 * @Date 2022/9/29 17:20:41
 * @Wechat NexoraOne
 * @Email NexoraOne
 * @Copyright  <a href="#">NexoraOne</a>
 */

public class ApiVariableService extends CodeGenerateBaseVariableService {

    @Override
    public boolean isSupport(CodeGeneratorConfigForm form) {
        return true;
    }

    @Override
    public Map<String, Object> getInjectVariablesMap(CodeGeneratorConfigForm form) {
        Map<String, Object> variablesMap = new HashMap<>();

        return variablesMap;
    }
}
