package ru.runa.gpd.handler.action;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import ru.runa.gpd.Localization;
import ru.runa.gpd.lang.model.Variable;
import ru.runa.gpd.ui.dialog.EditPropertyDialog;

public class ParamDefDynaComposite extends ParamDefComposite {
    private TableViewer tableViewer;
    private final Map<String, String> aProperties;
    private final String dynaParamsDescription;

    public ParamDefDynaComposite(Composite parent, ParamDefConfig config, Map<String, String> properties, List<Variable> variables, ParamDefGroup group,
            String dynaParamsDescription) {
        super(parent, config, properties, variables);
        aProperties = group.getDynaProperties();
        this.dynaParamsDescription = dynaParamsDescription;
    }

    @Override
    public void createUI() {
        super.createUI();
        Composite dynaComposite = new Composite(this, SWT.NONE);
        GridData data = new GridData(GridData.FILL_BOTH);
        data.horizontalSpan = 3;
        data.minimumHeight = 200;
        dynaComposite.setLayoutData(data);
        dynaComposite.setLayout(new GridLayout(2, false));
        Label descriptionLabel = new Label(dynaComposite, SWT.NONE);
        descriptionLabel.setText(dynaParamsDescription);
        data = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        data.horizontalSpan = 2;
        descriptionLabel.setLayoutData(data);
        tableViewer = new TableViewer(dynaComposite, SWT.SINGLE | SWT.V_SCROLL | SWT.FULL_SELECTION);
        data = new GridData(GridData.FILL_BOTH);
        tableViewer.getControl().setLayoutData(data);
        Table table = tableViewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        String[] columnNames = new String[] { Localization.getString("property.name"), Localization.getString("property.value") };
        int[] columnWidths = new int[] { 300, 300 };
        int[] columnAlignments = new int[] { SWT.LEFT, SWT.LEFT };
        for (int i = 0; i < columnNames.length; i++) {
            TableColumn tableColumn = new TableColumn(table, columnAlignments[i]);
            tableColumn.setText(columnNames[i]);
            tableColumn.setWidth(columnWidths[i]);
        }
        tableViewer.setLabelProvider(new TableLabelProvider());
        tableViewer.setContentProvider(new ArrayContentProvider());
        setTableInput();
        Composite buttonsBar = new Composite(dynaComposite, SWT.NONE);
        data = new GridData(GridData.FILL_VERTICAL);
        buttonsBar.setLayoutData(data);
        buttonsBar.setLayout(new GridLayout(1, false));
        createButton(buttonsBar, Localization.getString("button.add"), new AddSelectionAdapter());
        final Button editButton = createButton(buttonsBar, Localization.getString("button.edit"), new EditSelectionAdapter());
        final Button deleteButton = createButton(buttonsBar, Localization.getString("button.delete"), new DeleteSelectionAdapter());
        editButton.setEnabled(false);
        deleteButton.setEnabled(false);
        tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                editButton.setEnabled(!tableViewer.getSelection().isEmpty());
                deleteButton.setEnabled(!tableViewer.getSelection().isEmpty());
            }
        });
    }

    private Button createButton(Composite parent, String label, SelectionAdapter selectionAdapter) {
        Button button = new Button(parent, SWT.PUSH);
        button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING));
        button.setText(label);
        button.setFont(JFaceResources.getDialogFont());
        button.addSelectionListener(selectionAdapter);
        return button;
    }

    public Map<String, String> getAProperties() {
        return aProperties;
    }

    private void setTableInput() {
        List<String[]> input = new ArrayList<String[]>(aProperties.size());
        for (String name : aProperties.keySet()) {
            String value = aProperties.get(name);
            input.add(new String[] { name, value });
        }
        tableViewer.setInput(input);
    }

    private class AddSelectionAdapter extends SelectionAdapter {
        @Override
        public void widgetSelected(SelectionEvent e) {
            EditPropertyDialog dialog = new EditPropertyDialog(Display.getCurrent().getActiveShell(), false);
            if (dialog.open() == IDialogConstants.OK_ID) {
                aProperties.put(dialog.getName(), dialog.getValue());
                setTableInput();
            }
        }
    }

    private class EditSelectionAdapter extends SelectionAdapter {
        @Override
        public void widgetSelected(SelectionEvent e) {
            String[] data = (String[]) ((IStructuredSelection) tableViewer.getSelection()).getFirstElement();
            EditPropertyDialog dialog = new EditPropertyDialog(Display.getCurrent().getActiveShell(), true);
            dialog.setName(data[0]);
            dialog.setValue(data[1]);
            if (dialog.open() == IDialogConstants.OK_ID) {
                aProperties.put(dialog.getName(), dialog.getValue());
                setTableInput();
            }
        }
    }

    private class DeleteSelectionAdapter extends SelectionAdapter {
        @Override
        public void widgetSelected(SelectionEvent e) {
            String[] data = (String[]) ((IStructuredSelection) tableViewer.getSelection()).getFirstElement();
            aProperties.remove(data[0]);
            setTableInput();
        }
    }

    private static class TableLabelProvider extends LabelProvider implements ITableLabelProvider {
        @Override
        public String getColumnText(Object element, int index) {
            String[] data = (String[]) element;
            return data[index];
        }

        @Override
        public Image getColumnImage(Object element, int columnIndex) {
            return null;
        }
    }
}
