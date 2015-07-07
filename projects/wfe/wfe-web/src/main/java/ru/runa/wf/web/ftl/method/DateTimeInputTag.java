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
package ru.runa.wf.web.ftl.method;

import java.util.Date;

import ru.runa.wfe.commons.CalendarUtil;
import ru.runa.wfe.commons.ftl.FreemarkerTag;
import ru.runa.wfe.var.dto.WfVariable;

/**
 * @deprecated code moved to {@link InputVariableTag}.
 * 
 * @author dofs
 * @since 4.0
 */
@Deprecated
public class DateTimeInputTag extends FreemarkerTag {
    private static final long serialVersionUID = 1L;

    @Override
    protected Object executeTag() {
        String variableName = getParameterAsString(0);
        String view = getParameterAsString(1);
        WfVariable variable = variableProvider.getVariableNotNull(variableName);
        Date date = (Date) variable.getValue();
        String html = "";
        if ("date".equals(view)) {
            html += "<input type=\"text\" class=\"inputDate\" name=\"" + variableName + "\" style=\"width: 100px;\" ";
            if (date != null) {
                html += "value=\"" + CalendarUtil.formatDate(date) + "\" ";
            }
            html += "/>";
        }
        if ("time".equals(view)) {
            html += "<input type=\"text\" class=\"inputTime\" name=\"" + variableName + "\" style=\"width: 50px;\" ";
            if (date != null) {
                html += "value=\"" + CalendarUtil.formatTime(date) + "\" ";
            }
            html += "/>";
        }
        if ("datetime".equals(view)) {
            html += "<input type=\"text\" class=\"inputDateTime\" name=\"" + variableName + "\" style=\"width: 150px;\" ";
            if (date != null) {
                html += "value=\"" + CalendarUtil.formatDateTime(date) + "\" ";
            }
            html += "/>";
        }
        if (html.length() == 0) {
            log.warn("No HTML built (" + variableName + ") for variable " + variable);
        }
        return html;
    }

}
