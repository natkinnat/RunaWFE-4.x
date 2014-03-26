package ru.runa.gpd.ui.dialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.PlatformUI;

import ru.runa.gpd.Localization;
import ru.runa.gpd.ProcessCache;
import ru.runa.gpd.lang.model.MultiSubprocess;
import ru.runa.gpd.lang.model.ProcessDefinition;
import ru.runa.gpd.lang.model.Subprocess;
import ru.runa.gpd.lang.model.SubprocessDefinition;
import ru.runa.gpd.lang.model.Variable;
import ru.runa.gpd.ui.custom.Dialogs;
import ru.runa.gpd.ui.custom.DragAndDropAdapter;
import ru.runa.gpd.ui.custom.LoggingModifyTextAdapter;
import ru.runa.gpd.ui.custom.LoggingSelectionAdapter;
import ru.runa.gpd.ui.custom.LoggingSelectionChangedAdapter;
import ru.runa.gpd.ui.custom.SWTUtils;
import ru.runa.gpd.ui.custom.TableViewerLocalDragAndDropSupport;
import ru.runa.gpd.util.MultiinstanceParameters;
import ru.runa.gpd.util.VariableMapping;
import ru.runa.gpd.util.VariableUtils;

import com.google.common.collect.Lists;

public class SubprocessDialog extends Dialog {
    private Combo subprocessDefinitionCombo;
    private String subprocessName;
    protected final ProcessDefinition definition;
    protected final List<VariableMapping> variableMappings;
    private VariablesComposite variablesComposite;
    private final boolean multiinstance;
    private Button moveUpButton;
    private Button moveDownButton;
    private Button changeButton;
    private Button deleteButton;

    public SubprocessDialog(Subprocess subprocess) {
        super(PlatformUI.getWorkbench().getDisplay().getActiveShell());
        this.variableMappings = MultiinstanceParameters.getCopyWithoutMultiinstanceLinks(subprocess.getVariableMappings());
        this.definition = subprocess.getProcessDefinition();
        this.subprocessName = subprocess.getSubProcessName();
        this.multiinstance = subprocess instanceof MultiSubprocess;
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    @Override
    protected void configureShell(Shell newShell) {
        newShell.setSize(800, 500);
        super.configureShell(newShell);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        composite.setLayout(new GridLayout(1, false));

        Composite subprocessComposite = new Composite(composite, SWT.NONE);
        subprocessComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        subprocessComposite.setLayout(new GridLayout(2, false));

        Label label = new Label(subprocessComposite, SWT.NO_BACKGROUND);
        label.setLayoutData(new GridData());
        label.setText(Localization.getString("Subprocess.Name"));
        subprocessDefinitionCombo = new Combo(subprocessComposite, SWT.BORDER);
        subprocessDefinitionCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        subprocessDefinitionCombo.setItems(getProcessDefinitionNames());
        subprocessDefinitionCombo.setVisibleItemCount(10);
        if (subprocessName != null) {
            subprocessDefinitionCombo.setText(subprocessName);
        }
        subprocessDefinitionCombo.addSelectionListener(new LoggingSelectionAdapter() {

            @Override
            protected void onSelection(SelectionEvent e) throws Exception {
                onSubprocessChanged();
            }
        });
        subprocessDefinitionCombo.addModifyListener(new LoggingModifyTextAdapter() {

            @Override
            protected void onTextChanged(ModifyEvent e) throws Exception {
                onSubprocessChanged();
            }
        });

        createConfigurationComposite(composite);

        Label variablesLabel = new Label(composite, SWT.NONE);
        variablesLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        variablesLabel.setText(Localization.getString("Subprocess.VariableMappings"));

        variablesComposite = new VariablesComposite(composite);
        variablesComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
        variablesComposite.setFocus();
        return composite;
    }

    protected void createConfigurationComposite(Composite composite) {

    }

    protected void onSubprocessChanged() {
        subprocessName = subprocessDefinitionCombo.getText();
    }

    private class VariablesComposite extends Composite {
        private TableViewer tableViewer;
        
        public VariablesComposite(Composite parent) {
            super(parent, SWT.BORDER);
            setLayout(new GridLayout(2, false));

            tableViewer = new TableViewer(this, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.FULL_SELECTION);
            tableViewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
            Table table = tableViewer.getTable();
            table.setHeaderVisible(true);
            table.setLinesVisible(true);
            String[] columnNames = new String[] { "VariableMapping.Usage.Read", "VariableMapping.Usage.Write",  
                    "Subprocess.ProcessVariableName", "Subprocess.SubprocessVariableName" };
            int[] columnWidths = new int[] { 50, 50, 250, 250 };
            int[] columnAlignments = new int[] { SWT.CENTER, SWT.CENTER, SWT.LEFT, SWT.LEFT };
            for (int i = 0; i < columnNames.length; i++) {
                TableColumn tableColumn = new TableColumn(table, columnAlignments[i]);
                tableColumn.setText(Localization.getString(columnNames[i]));
                tableColumn.setToolTipText(Localization.getString(columnNames[i] + ".description"));
                tableColumn.setWidth(columnWidths[i]);
            }
            if (multiinstance) {
                TableColumn tableColumn = new TableColumn(table, SWT.CENTER);
                tableColumn.setText(Localization.getString("VariableMapping.Usage.MultiinstanceLink"));
                tableColumn.setWidth(50);
                tableColumn.setToolTipText(Localization.getString("VariableMapping.Usage.MultiinstanceLink.description"));
            }
            tableViewer.setLabelProvider(new VariableMappingTableLabelProvider());
            tableViewer.setContentProvider(new ArrayContentProvider());
            setTableInput();

            Composite buttonsComposite = new Composite(this, SWT.NONE);
            buttonsComposite.setLayout(new GridLayout());
            GridData gridData = new GridData();
            gridData.horizontalAlignment = SWT.LEFT;
            gridData.verticalAlignment = SWT.TOP;
            buttonsComposite.setLayoutData(gridData);
            SWTUtils.createButtonFillHorizontal(buttonsComposite, Localization.getString("button.add"), new LoggingSelectionAdapter() {

                @Override
                protected void onSelection(SelectionEvent e) throws Exception {
                    editVariableMapping(null);
                }
            });
            changeButton = SWTUtils.createButtonFillHorizontal(buttonsComposite, Localization.getString("button.change"), new LoggingSelectionAdapter() {

                @Override
                protected void onSelection(SelectionEvent e) throws Exception {
                    IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
                    if (!selection.isEmpty()) {
                        VariableMapping oldMapping = (VariableMapping) selection.getFirstElement();
                        editVariableMapping(oldMapping);
                    }
                }
            });
            moveUpButton = SWTUtils.createButtonFillHorizontal(buttonsComposite, Localization.getString("button.up"), new MoveVariableSelectionListener(true));
            moveDownButton = SWTUtils.createButtonFillHorizontal(buttonsComposite, Localization.getString("button.down"), new MoveVariableSelectionListener(false));
            deleteButton = SWTUtils.createButtonFillHorizontal(buttonsComposite, Localization.getString("button.delete"), new LoggingSelectionAdapter() {

                @Override
                protected void onSelection(SelectionEvent e) throws Exception {
                    IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
                    if (!selection.isEmpty()) {
                        VariableMapping mapping = (VariableMapping) selection.getFirstElement();
                        if (Dialogs.confirm(Localization.getString("confirm.delete"))) {
                            variableMappings.remove(mapping);
                            tableViewer.refresh();
                            setTableInput();
                        }
                    }
                }
            });
            tableViewer.addSelectionChangedListener(new LoggingSelectionChangedAdapter() {
                @Override
                protected void onSelectionChanged(SelectionChangedEvent event) throws Exception {
                    updateButtons();
                }
            });
            updateButtons();
            TableViewerLocalDragAndDropSupport.enable(tableViewer, new DragAndDropAdapter<VariableMapping>() {

                @Override
                public void onDropElement(VariableMapping beforeElement, VariableMapping element) {
                    if (variableMappings.remove(element)) {
                        variableMappings.add(variableMappings.indexOf(beforeElement), element);
                    }
                }

                @Override
                public void onDrop(VariableMapping beforeElement, List<VariableMapping> elements) {
                    super.onDrop(beforeElement, elements);
                    setTableInput();
                }
                
            });
        }

        private void updateButtons() {
            List<?> selected = ((IStructuredSelection) tableViewer.getSelection()).toList();
            changeButton.setEnabled(selected.size() == 1);
            moveUpButton.setEnabled(selected.size() == 1 && variableMappings.indexOf(selected.get(0)) > 0);
            moveDownButton.setEnabled(selected.size() == 1 && variableMappings.indexOf(selected.get(0)) < variableMappings.size() - 1);
            deleteButton.setEnabled(selected.size() > 0);
        }

        private void editVariableMapping(VariableMapping mapping) {
            SubprocessVariableDialog dialog = new SubprocessVariableDialog(getProcessVariablesNames(definition.getName()), 
                    getProcessVariablesNames(getSubprocessName()), mapping);
            if (dialog.open() != IDialogConstants.CANCEL_ID) {
                if (mapping == null) {
                    mapping = new VariableMapping();
                    variableMappings.add(mapping);
                    setTableInput();
                }
                mapping.setProcessVariableName(dialog.getProcessVariable());
                mapping.setSubprocessVariableName(dialog.getSubprocessVariable());
                String usage = dialog.getAccess();
                if (isListVariable(definition.getName(), mapping.getProcessVariableName()) && !isListVariable(getSubprocessName(), mapping.getSubprocessVariableName())) {
                    usage += "," + VariableMapping.USAGE_MULTIINSTANCE_LINK;
                }
                mapping.setUsage(usage);
                tableViewer.refresh();
            }
        }

        private void setTableInput() {
            tableViewer.setInput(getVariableMappings(false));
        }

        private class MoveVariableSelectionListener extends LoggingSelectionAdapter {
            private final boolean up;

            public MoveVariableSelectionListener(boolean up) {
                this.up = up;
            }

            @Override
            protected void onSelection(SelectionEvent e) throws Exception {
                IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
                VariableMapping mapping = (VariableMapping) selection.getFirstElement();
                int index = variableMappings.indexOf(mapping);
                Collections.swap(variableMappings, index, up ? (index - 1) : (index + 1));
                setTableInput();
                tableViewer.setSelection(selection);
            }
        }

    }

    public List<VariableMapping> getVariableMappings(boolean includeMetadata) {
        return variableMappings;
    }

    private static class VariableMappingTableLabelProvider extends LabelProvider implements ITableLabelProvider {
        @Override
        public String getColumnText(Object element, int index) {
            VariableMapping mapping = (VariableMapping) element;
            switch (index) {
            case 0:
                return mapping.isReadable() ? "+" : "";
            case 1:
                return mapping.isWritable() ? "+" : "";
            case 2:
                return mapping.getProcessVariableName();
            case 3:
                return mapping.getSubprocessVariableName();
            case 4:
                return mapping.isMultiinstanceLink() ? "+" : "";
            default:
                return "unknown " + index;
            }
        }

        @Override
        public Image getColumnImage(Object element, int columnIndex) {
            return null;
        }
    }

    private String[] getProcessDefinitionNames() {
        List<String> names = Lists.newArrayList();
        for (ProcessDefinition testProcessDefinition : ProcessCache.getAllProcessDefinitions()) {
            if (testProcessDefinition instanceof SubprocessDefinition) {
                continue;
            }
            if (!names.contains(testProcessDefinition.getName())) {
                names.add(testProcessDefinition.getName());
            }
        }
        Collections.sort(names);
        return names.toArray(new String[names.size()]);
    }

    private List<String> getProcessVariablesNames(String name) {
        ProcessDefinition definition = ProcessCache.getFirstProcessDefinition(name);
        if (definition != null) {
            return definition.getVariableNames(true);
        }
        return new ArrayList<String>();
    }

    private boolean isListVariable(String name, String variableName) {
        ProcessDefinition definition = ProcessCache.getFirstProcessDefinition(name);
        if (definition != null) {
            Variable variable = VariableUtils.getVariableByName(definition, variableName);
            if (variable != null) {
                return List.class.getName().equals(variable.getJavaClassName());
            }
        }
        return false;
    }

    public String getSubprocessName() {
        return subprocessName;
    }
}
