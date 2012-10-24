package ru.runa.jbpm.ui.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.jbpm.ui.ProcessCache;
import org.jbpm.ui.util.WorkspaceOperations;

public class ExportAction extends BaseActionDelegate {

    public void run(IAction action) {
        WorkspaceOperations.exportProcessDefinition(getStructuredSelection());
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        action.setEnabled(ProcessCache.getAllProcessDefinitions().size() > 0);
    }

}
