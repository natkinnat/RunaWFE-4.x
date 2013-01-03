package ru.runa.wfe.commons;

import java.util.Map;

import com.google.common.collect.Maps;

/**
 * This class contains substitution for loaded classes (back compatibility on
 * class loading).
 * 
 * @author dofs
 * @since 4.0
 */
public class BackCompatibilityClassNames {
    private static Map<String, String> bcc = Maps.newHashMap();
    static {
        // formats renamed
        bcc.put("ru.runa.web.formgen.format.DoubleFormat", "ru.runa.wfe.var.format.DoubleFormat");
        bcc.put("org.jbpm.web.formgen.format.DoubleFormat", "ru.runa.wfe.var.format.DoubleFormat");
        bcc.put("org.jbpm.web.formgen.format.DefaultFormat", "ru.runa.wfe.var.format.StringFormat");
        bcc.put("ru.runa.wf.web.forms.format.ArrayListFormat", "ru.runa.wfe.var.format.ListFormat");
        bcc.put("ru.runa.wf.web.forms.format.BooleanFormat", "ru.runa.wfe.var.format.BooleanFormat");
        bcc.put("ru.runa.wf.web.forms.format.DateFormat", "ru.runa.wfe.var.format.DateFormat");
        bcc.put("ru.runa.wf.web.forms.format.DateTimeFormat", "ru.runa.wfe.var.format.DateTimeFormat");
        bcc.put("ru.runa.wf.web.forms.format.DoubleFormat", "ru.runa.wfe.var.format.DoubleFormat");
        bcc.put("ru.runa.wf.web.forms.format.FileFormat", "ru.runa.wfe.var.format.FileFormat");
        bcc.put("ru.runa.wf.web.forms.format.LongFormat", "ru.runa.wfe.var.format.LongFormat");
        bcc.put("ru.runa.wf.web.forms.format.StringArrayFormat", "ru.runa.wfe.var.format.ListFormat");
        bcc.put("ru.runa.wf.web.forms.format.StringFormat", "ru.runa.wfe.var.format.StringFormat");
        bcc.put("ru.runa.wf.web.forms.format.TimeFormat", "ru.runa.wfe.var.format.TimeFormat");
        // assignment handler renamed
        bcc.put("ru.runa.wf.jbpm.delegation.assignment.AssignmentHandler", "ru.runa.wfe.handler.assign.DefaultAssignmentHandler");
        // decision handler renamed
        bcc.put("ru.runa.wf.jbpm.delegation.decision.BSFDecisionHandler", "ru.runa.wfe.handler.decision.BSFDecisionHandler");
        // action handlers renamed
        bcc.put("ru.runa.wf.jbpm.delegation.action.SetSubProcessPermissionsActionHandler", "ru.runa.wfe.handler.action.DebugActionHandler");
        bcc.put("ru.runa.wf.jbpm.delegation.action.BotInvokerActionHandler", "ru.runa.service.bot.handler.BotInvokerActionHandler");
        bcc.put("ru.runa.wfe.bp.commons.ExecuteFormulaActionHandler", "ru.runa.wfe.handler.action.var.FormulaActionHandler");
        bcc.put("ru.runa.wf.swimlane.AssignSwimlaneActionHandler", "ru.runa.wfe.handler.action.user.AssignSwimlaneActionHandler");
        bcc.put("ru.runa.wf.users.ActorNameActionHandler", "ru.runa.wfe.handler.action.user.ActorNameActionHandler");
        bcc.put("ru.runa.wf.var.AddObjectToListActionHandler", "ru.runa.wfe.handler.action.var.AddObjectToListActionHandler");
        bcc.put("ru.runa.wf.var.SetObjectToListActionHandler", "ru.runa.wfe.handler.action.var.SetObjectToListActionHandler");
        bcc.put("ru.runa.wf.var.GetObjectFromListActionHandler", "ru.runa.wfe.handler.action.var.GetObjectFromListActionHandler");
        bcc.put("ru.runa.wf.var.ListAggregateFunctionActionHandler", "ru.runa.wfe.handler.action.var.ListAggregateFunctionActionHandler");
        bcc.put("ru.runa.wf.var.GetObjectFromMapActionHandler", "ru.runa.wfe.handler.action.var.GetObjectFromMapActionHandler");
        bcc.put("ru.runa.wf.var.PutObjectToMapActionHandler", "ru.runa.wfe.handler.action.var.PutObjectToMapActionHandler");
        bcc.put("ru.runa.wf.var.RemoveObjectFromMapActionHandler", "ru.runa.wfe.handler.action.var.RemoveObjectFromMapActionHandler");
        bcc.put("ru.runa.wf.var.MapAggregateFunctionActionHandler", "ru.runa.wfe.handler.action.var.MapAggregateFunctionActionHandler");
        bcc.put("ru.runa.wf.var.ConvertMapKeysToListActionHandler", "ru.runa.wfe.handler.action.var.ConvertMapKeysToListActionHandler");
        bcc.put("ru.runa.wf.var.ConvertMapValuesToListActionHandler", "ru.runa.wfe.handler.action.var.ConvertMapValuesToListActionHandler");
        bcc.put("ru.runa.wf.var.MergeMapsActionHandler", "ru.runa.wfe.handler.action.var.MergeMapsActionHandler");
        bcc.put("ru.runa.wf.var.ClearVariableActionHandler", "ru.runa.wfe.handler.action.var.ClearVariableActionHandler");
        bcc.put("ru.runa.wf.var.FormulaActionHandler", "ru.runa.wfe.handler.action.var.FormulaActionHandler");
        bcc.put("ru.runa.wf.var.RemoveObjectFromListActionHandler", "ru.runa.wfe.handler.action.var.RemoveObjectFromListActionHandler");
        bcc.put("ru.runa.wf.var.SortListActionHandler", "ru.runa.wfe.handler.action.var.SortListActionHandler");
        bcc.put("ru.runa.wf.BSHActionHandler", "ru.runa.wfe.handler.action.BSHActionHandler");
        bcc.put("ru.runa.wf.CreateOptionActionHandler", "ru.runa.wfe.handler.action.CreateOptionActionHandler");
        bcc.put("ru.runa.wf.EmailTaskNotifierActionHandler", "ru.runa.wfe.handler.action.EmailTaskNotifierActionHandler");
        bcc.put("ru.runa.wf.EscalationActionHandler", "ru.runa.wfe.handler.action.EscalationActionHandler");
        bcc.put("ru.runa.wf.GroovyActionHandler", "ru.runa.wfe.handler.action.GroovyActionHandler");
        bcc.put("ru.runa.wf.SendEmailActionHandler", "ru.runa.wfe.handler.action.SendEmailActionHandler");
        bcc.put("ru.runa.wf.SQLActionHandler", "ru.runa.wfe.handler.action.SQLActionHandler");
        // org functions renamed
        bcc.put("ru.runa.af.organizationfunction.ChiefFunction", "ru.runa.wfe.os.func.ChiefFunction");
        bcc.put("ru.runa.af.organizationfunction.ChiefRecursiveFunction", "ru.runa.wfe.os.func.ChiefRecursiveFunction");
        bcc.put("ru.runa.af.organizationfunction.DemoChiefFunction", "ru.runa.wfe.os.func.DemoChiefFunction");
        bcc.put("ru.runa.af.organizationfunction.DirectorFunction", "ru.runa.wfe.os.func.DirectorFunction");
        bcc.put("ru.runa.af.organizationfunction.ExecutorByCodeFunction", "ru.runa.wfe.os.func.ExecutorByCodeFunction");
        bcc.put("ru.runa.af.organizationfunction.ExecutorByNameFunction", "ru.runa.wfe.os.func.ExecutorByNameFunction");
        bcc.put("ru.runa.af.organizationfunction.SQLFunction", "ru.runa.wfe.os.func.SQLFunction");
        bcc.put("ru.runa.af.organizationfunction.SubordinateFunction", "ru.runa.wfe.os.func.SubordinateFunction");
        bcc.put("ru.runa.af.organizationfunction.SubordinateRecursiveFunction", "ru.runa.wfe.os.func.SubordinateRecursiveFunction");
        // custom html tags
        bcc.put("ru.runa.wf.web.html.vartag.GroupMembersAutoCompletionVarTag", "ru.runa.wf.web.customtag.impl.GroupMembersAutoCompletionVarTag");
        bcc.put("ru.runa.wf.web.html.vartag.DemoSubordinateAutoCompletingComboboxVarTag",
                "ru.runa.wf.web.customtag.impl.DemoSubordinateAutoCompletingComboboxVarTag");
        bcc.put("ru.runa.wf.web.html.vartag.ActorComboboxVarTag", "ru.runa.wf.web.customtag.impl.ActorComboboxVarTag");
        bcc.put("ru.runa.wf.web.html.vartag.GroupMembersComboboxVarTag", "ru.runa.wf.web.customtag.impl.GroupMembersComboboxVarTag");
        bcc.put("ru.runa.wf.web.html.vartag.ActorFullNameDisplayVarTag", "ru.runa.wf.web.customtag.impl.ActorFullNameDisplayVarTag");
        bcc.put("ru.runa.wf.web.html.vartag.ActorNameDisplayVarTag", "ru.runa.wf.web.customtag.impl.ActorNameDisplayVarTag");
        bcc.put("ru.runa.wf.web.html.vartag.DateInputVarTag", "ru.runa.wf.web.customtag.impl.DateInputVarTag");
        bcc.put("ru.runa.wf.web.html.vartag.DateTimeInputVarTag", "ru.runa.wf.web.customtag.impl.DateTimeInputVarTag");
        bcc.put("ru.runa.wf.web.html.vartag.DateTimeValueDisplayVarTag", "ru.runa.wf.web.customtag.impl.DateTimeValueDisplayVarTag");
        bcc.put("ru.runa.wf.web.html.vartag.DateValueDisplayVarTag", "ru.runa.wf.web.customtag.impl.DateValueDisplayVarTag");
        bcc.put("ru.runa.wf.web.html.vartag.FileVariableValueDownloadVarTag", "ru.runa.wf.web.customtag.impl.FileVariableValueDownloadVarTag");
        bcc.put("ru.runa.wf.web.html.vartag.TimeValueDisplayVarTag", "ru.runa.wf.web.customtag.impl.TimeValueDisplayVarTag");
        bcc.put("ru.runa.wf.web.html.vartag.TimeInputVarTag", "ru.runa.wf.web.customtag.impl.TimeInputVarTag");
        bcc.put("ru.runa.wf.web.html.vartag.VariableValueDisplayVarTag", "ru.runa.wf.web.customtag.impl.VariableValueDisplayVarTag");
    }

    /**
     * Gets back-compatible class name if found.
     * 
     * @param className
     *            original class name
     * @return adjusted class name or original class name
     */
    public static String getClassName(String className) {
        if (bcc.containsKey(className)) {
            return bcc.get(className);
        }
        return className;
    }
}
