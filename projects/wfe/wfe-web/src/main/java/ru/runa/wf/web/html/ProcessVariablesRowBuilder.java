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
package ru.runa.wf.web.html;

import java.util.List;

import javax.servlet.jsp.PageContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ecs.html.TD;
import org.apache.ecs.html.TR;

import ru.runa.common.WebResources;
import ru.runa.common.web.Commons;
import ru.runa.common.web.Messages;
import ru.runa.common.web.Resources;
import ru.runa.common.web.StrutsWebHelper;
import ru.runa.common.web.html.RowBuilder;
import ru.runa.wfe.user.User;
import ru.runa.wfe.var.dto.WfVariable;
import ru.runa.wfe.var.format.VariableDisplaySupport;
import ru.runa.wfe.var.format.VariableFormat;

public class ProcessVariablesRowBuilder implements RowBuilder {
    private static final Log log = LogFactory.getLog(ProcessVariablesRowBuilder.class);

    private int idx = 0;
    private final List<WfVariable> variables;
    private final PageContext pageContext;
    private final Long processId;

    public ProcessVariablesRowBuilder(Long processId, List<WfVariable> variables, PageContext pageContext) {
        this.variables = variables;
        this.processId = processId;
        this.pageContext = pageContext;
    }

    @Override
    public boolean hasNext() {
        return idx < variables.size();
    }

    @Override
    public TR buildNext() {
        WfVariable variable = variables.get(idx);
        Object value = variable.getValue();
        TR tr = new TR();
        tr.addElement(new TD(variable.getDefinition().getName()).setClass(Resources.CLASS_LIST_TABLE_TD));
        tr.addElement(new TD(variable.getDefinition().getFormatLabel()).setClass(Resources.CLASS_LIST_TABLE_TD));
        if (WebResources.isDisplayVariablesJavaType()) {
            String className = value != null ? value.getClass().getName() : "";
            tr.addElement(new TD(className).setClass(Resources.CLASS_LIST_TABLE_TD));
        }
        String formattedValue;
        if (value == null) {
            formattedValue = Messages.getMessage("label.unset_empty.value", pageContext);
        } else {
            try {
                VariableFormat variableFormat = variable.getDefinition().getFormat();
                if (variableFormat instanceof VariableDisplaySupport) {
                    User user = Commons.getUser(pageContext.getSession());
                    formattedValue = ((VariableDisplaySupport) variableFormat).getHtml(user, new StrutsWebHelper(pageContext), processId, variable
                            .getDefinition().getName(), value);
                } else {
                    formattedValue = variableFormat.format(value);
                }
            } catch (Exception e) {
                log.warn("Unable to format value " + value + " of decl " + variable.getDefinition() + " in " + processId, e);
                formattedValue = value.toString() + " <span class=\"error\">(" + e.getMessage() + ")</span>";
            }
        }
        tr.addElement(new TD(formattedValue).setClass(Resources.CLASS_LIST_TABLE_TD));

        idx++;
        return tr;
    }

}
