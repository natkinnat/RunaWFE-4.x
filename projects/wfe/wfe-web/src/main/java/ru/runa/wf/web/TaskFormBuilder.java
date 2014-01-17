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
package ru.runa.wf.web;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;

import ru.runa.common.web.Messages;
import ru.runa.wfe.form.Interaction;
import ru.runa.wfe.service.client.DelegateDefinitionVariableProvider;
import ru.runa.wfe.service.client.DelegateProcessVariableProvider;
import ru.runa.wfe.task.dto.WfTask;
import ru.runa.wfe.user.User;
import ru.runa.wfe.var.IVariableProvider;
import ru.runa.wfe.var.MapDelegableVariableProvider;

/**
 * Created on 17.11.2004
 */
public abstract class TaskFormBuilder {
    protected User user;
    protected PageContext pageContext;
    protected Interaction interaction;

    public void setUser(User user) {
        this.user = user;
    }

    public void setInteraction(Interaction interaction) {
        this.interaction = interaction;
    }

    public void setPageContext(PageContext pageContext) {
        this.pageContext = pageContext;
    }

    public final String build(Long definitionId) {
        if (interaction.hasForm()) {
            IVariableProvider variableProvider = new MapDelegableVariableProvider(interaction.getDefaultVariableValues(),
                    new DelegateDefinitionVariableProvider(user, definitionId));
            Map<String, Object> userDefinedVariables = FormSubmissionUtils.getUserFormInputVariables((HttpServletRequest) pageContext.getRequest(),
                    interaction);
            if (userDefinedVariables != null) {
                variableProvider = new MapDelegableVariableProvider(userDefinedVariables, variableProvider);
            }
            return buildForm(variableProvider, definitionId);
        } else {
            return buildEmptyForm();
        }
    }

    public final String build(WfTask task) {
        if (interaction.hasForm()) {
            IVariableProvider variableProvider = new DelegateProcessVariableProvider(user, task.getProcessId());
            Map<String, Object> userDefinedVariables = FormSubmissionUtils.getUserFormInputVariables((HttpServletRequest) pageContext.getRequest(),
                    interaction);
            if (userDefinedVariables != null) {
                variableProvider = new MapDelegableVariableProvider(userDefinedVariables, variableProvider);
            }
            return buildForm(variableProvider, task.getDefinitionId());
        } else {
            return buildEmptyForm();
        }
    }

    private String buildForm(IVariableProvider variableProvider, Long definitionId) {
        String form = buildForm(variableProvider);
        return FormPresentationUtils.adjustForm(pageContext, definitionId, form, variableProvider, interaction.getRequiredVariableNames());
    }

    protected abstract String buildForm(IVariableProvider variableProvider);

    private String buildEmptyForm() {
        String message = "Task form is not defined";
        if (pageContext != null) {
            message = Messages.getMessage("task.form.not.defined.error", pageContext);
        }
        return message;
    }

}