/*
 * This file is part of the RUNA WFE project.
 * 
 * This program is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU Lesser General Public License 
 * as published by the Free Software Foundation; version 2.1 
 * of the License. 
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the 
 * GNU Lesser General Public License for more details. 
 * 
 * You should have received a copy of the GNU Lesser General Public License 
 * along with this program; if not, write to the Free Software 
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 */
package ru.runa.wf.logic.bot;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;

import ru.runa.wf.logic.bot.assigner.AssignerSettings;
import ru.runa.wf.logic.bot.assigner.AssignerSettings.Condition;
import ru.runa.wf.logic.bot.assigner.AssignerSettingsXmlParser;
import ru.runa.wf.logic.bot.assigner.IEvaluationFunction;
import ru.runa.wfe.commons.ClassLoaderUtil;
import ru.runa.wfe.handler.bot.TaskHandler;
import ru.runa.wfe.task.dto.WfTask;
import ru.runa.wfe.var.IVariableProvider;

import com.google.common.collect.Maps;

public class AdvancedSwimlaneAssignerTaskHandler implements TaskHandler {
    private AssignerSettings settings;

    @Override
    public void setConfiguration(byte[] configuration) throws Exception {
        settings = AssignerSettingsXmlParser.read(new ByteArrayInputStream(configuration));
    }

    @Override
    public Object getConfiguration() {
        return settings;
    }

    @Override
    public Map<String, Object> handle(Subject subject, IVariableProvider variableProvider, WfTask task) throws Exception {
        Map<String, Object> outputVariables = Maps.newHashMap();
        List<Condition> conditions = settings.getAssignerConditions();
        for (Condition condition : conditions) {
            if (isAppliedCondition(condition.getFunctionClassName(), variableProvider)) {
                String actor = variableProvider.getValue(String.class, condition.getVariableName());
                outputVariables.put(condition.getSwimlaneName(), actor);
                break;
            }
        }
        return outputVariables;
    }

    private boolean isAppliedCondition(String functionClassName, IVariableProvider variableProvider) {
        if ("true".equalsIgnoreCase(functionClassName)) {
            return true;
        }
        IEvaluationFunction evaluationFunction = ClassLoaderUtil.instantiate(functionClassName);
        return evaluationFunction.evaluate(variableProvider);
    }
}
