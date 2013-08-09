package ru.runa.gpd.extension.handler.var;

import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.HyperlinkGroup;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Hyperlink;

import ru.runa.gpd.Localization;
import ru.runa.gpd.PluginLogger;
import ru.runa.gpd.extension.handler.XmlBasedConstructorProvider;
import ru.runa.gpd.lang.model.Delegable;
import ru.runa.wfe.extension.handler.var.ConvertMapToListOperation;
import ru.runa.wfe.extension.handler.var.ConvertMapsToListsConfig;
import ru.runa.wfe.extension.handler.var.ConvertMapsToListsConfig.Sorting;

public class ConvertMapsToListsHandlerProvider extends XmlBasedConstructorProvider<ConvertMapsToListsConfig> {
    @Override
    protected ConvertMapsToListsConfig createDefault() {
        return new ConvertMapsToListsConfig();
    }

    @Override
    protected ConvertMapsToListsConfig fromXml(String xml) throws Exception {
        return ConvertMapsToListsConfig.fromXml(xml);
    }

    @Override
    protected Composite createConstructorView(Composite parent, Delegable delegable) {
        return new ConstructorView(parent, delegable);
    }

    @Override
    protected String getTitle() {
        return Localization.getString("ConvertMapsToListsConfig.title");
    }

    private class ConstructorView extends Composite implements Observer {
        private final HyperlinkGroup hyperlinkGroup = new HyperlinkGroup(Display.getCurrent());
        private final Delegable delegable;

        public ConstructorView(Composite parent, Delegable delegable) {
            super(parent, SWT.NONE);
            this.delegable = delegable;
            setLayout(new GridLayout(3, false));
            buildFromModel();
        }

        @Override
        public void update(Observable o, Object arg) {
            buildFromModel();
        }

        public void buildFromModel() {
            try {
                for (Control control : getChildren()) {
                    control.dispose();
                }
                addRootSection();
                ((ScrolledComposite) getParent()).setMinSize(computeSize(getSize().x, SWT.DEFAULT));
                this.layout(true, true);
            } catch (Throwable e) {
                PluginLogger.logErrorWithoutDialog("Cannot build model", e);
            }
        }

        private void addRootSection() {
            Composite paramsComposite = createParametersComposite(this);
            int index = 0;
            for (ConvertMapToListOperation operation : model.getOperations()) {
                addOperationSection(paramsComposite, operation, index);
                index++;
            }
            createStrokeComposite(this, Localization.getString("ConvertMapsToListsConfig.sorting"), null);
            {
                final Combo combo = new Combo(this, SWT.READ_ONLY);
                combo.add(Localization.getString("ConvertMapsToListsConfig.sorting." + Sorting.NONE));
                combo.add(Localization.getString("ConvertMapsToListsConfig.sorting." + Sorting.KEYS));
                for (ConvertMapToListOperation operation : model.getOperations()) {
                    combo.add(Localization.getString("ConvertMapsToListsConfig.sorting." + Sorting.VALUES) + " " + operation.getMapVariableName());
                }
                combo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                String sortBy = model.getSorting().getSortBy();
                int selectedIndex;
                if (Sorting.NONE.equals(sortBy)) {
                    selectedIndex = 0;
                } else if (Sorting.KEYS.equals(sortBy)) {
                    selectedIndex = 1;
                } else {
                    selectedIndex = Integer.parseInt(sortBy.substring(Sorting.VALUES.length())) + 2;
                }
                combo.select(selectedIndex);
                combo.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        int selectedIndex = combo.getSelectionIndex();
                        String sortBy;
                        if (selectedIndex == 0) {
                            sortBy = Sorting.NONE;
                        } else if (selectedIndex == 1) {
                            sortBy = Sorting.KEYS;
                        } else {
                            sortBy = Sorting.VALUES + (selectedIndex - 2);
                        }
                        model.getSorting().setSortBy(sortBy);
                    }
                });
            }
            {
                final Combo combo = new Combo(this, SWT.READ_ONLY);
                combo.add(Localization.getString("sorting." + Sorting.MODE_ASC));
                combo.add(Localization.getString("sorting." + Sorting.MODE_DESC));
                combo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                combo.select(Sorting.MODE_ASC.equals(model.getSorting().getSortMode()) ? 0 : 1);
                combo.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        String mode = combo.getSelectionIndex() == 0 ? Sorting.MODE_ASC : Sorting.MODE_DESC;
                        model.getSorting().setSortMode(mode);
                    }
                });
            }
        }

        private void createStrokeComposite(Composite parent, String label, HyperlinkAdapter hyperlinkAdapter) {
            Composite strokeComposite = new Composite(parent, SWT.NONE);
            GridData data = new GridData(GridData.FILL_HORIZONTAL);
            data.horizontalSpan = 3;
            strokeComposite.setLayoutData(data);
            strokeComposite.setLayout(new GridLayout(hyperlinkAdapter != null ? 4 : 3, false));
            Label strokeLabel = new Label(strokeComposite, SWT.SEPARATOR | SWT.HORIZONTAL);
            data = new GridData();
            data.widthHint = 50;
            strokeLabel.setLayoutData(data);
            Label headerLabel = new Label(strokeComposite, SWT.NONE);
            headerLabel.setText(label);
            strokeLabel = new Label(strokeComposite, SWT.SEPARATOR | SWT.HORIZONTAL);
            strokeLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            if (hyperlinkAdapter != null) {
                Hyperlink hl1 = new Hyperlink(strokeComposite, SWT.NONE);
                hl1.setText(Localization.getString("button.add"));
                hl1.addHyperlinkListener(hyperlinkAdapter);
                hyperlinkGroup.add(hl1);
            }
        }

        private Composite createParametersComposite(Composite parent) {
            Composite composite = new Composite(parent, SWT.NONE);
            composite.setLayout(new GridLayout(3, false));
            GridData data = new GridData(GridData.FILL_HORIZONTAL);
            data.horizontalSpan = 3;
            composite.setLayoutData(data);
            createStrokeComposite(composite, Localization.getString("ConvertMapsToListsConfig.label.operations"), new HyperlinkAdapter() {
                @Override
                public void linkActivated(HyperlinkEvent e) {
                    model.addOperation();
                }
            });
            return composite;
        }

        private void addOperationSection(Composite parent, final ConvertMapToListOperation operation, final int index) {
            {
                final Combo combo = new Combo(parent, SWT.READ_ONLY);
                for (String variableName : delegable.getVariableNames(false, Map.class.getName())) {
                    combo.add(variableName);
                }
                combo.setText(operation.getMapVariableName());
                combo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                combo.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        operation.setMapVariableName(combo.getText());
                    }
                });
            }
            {
                final Combo combo = new Combo(parent, SWT.READ_ONLY);
                for (String variableName : delegable.getVariableNames(false, List.class.getName())) {
                    combo.add(variableName);
                }
                combo.setText(operation.getListVariableName());
                combo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                combo.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        operation.setListVariableName(combo.getText());
                    }
                });
            }
            Hyperlink hl1 = new Hyperlink(parent, SWT.NONE);
            hl1.setText("[X]");
            hl1.addHyperlinkListener(new HyperlinkAdapter() {
                @Override
                public void linkActivated(HyperlinkEvent e) {
                    model.deleteOperation(index);
                }
            });
            hyperlinkGroup.add(hl1);
        }
    }
}
