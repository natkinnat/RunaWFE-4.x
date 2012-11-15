package ru.runa.gpd.property;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.views.properties.PropertyDescriptor;

import ru.runa.gpd.lang.model.GraphElement;
import ru.runa.gpd.lang.model.TaskState;
import ru.runa.gpd.ui.dialog.EscalationActionEditDialog;

public class EscalationActionPropertyDescriptor extends PropertyDescriptor {
    private final TaskState element;

    public EscalationActionPropertyDescriptor(Object id, String displayName, TaskState element) {
        super(id, displayName);
        this.element = element;
    }

    @Override
    public CellEditor createPropertyEditor(Composite parent) {
        return new TimerActionDialogCellEditor(parent);
    }

    private class TimerActionDialogCellEditor extends DialogCellEditor {

        public TimerActionDialogCellEditor(Composite parent) {
            super(parent, SWT.NONE);
        }

        @Override
        protected Object openDialogBox(Control cellEditorWindow) {
            EscalationActionEditDialog dialog = new EscalationActionEditDialog(((GraphElement) element).getProcessDefinition(), element.getEscalationAction());
            return dialog.openDialog();
        }
    }
}
