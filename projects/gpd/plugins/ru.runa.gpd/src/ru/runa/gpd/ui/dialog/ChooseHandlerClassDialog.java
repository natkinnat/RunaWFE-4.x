package ru.runa.gpd.ui.dialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.LabelProvider;

import ru.runa.gpd.Localization;
import ru.runa.gpd.handler.CustomizationRegistry;
import ru.runa.gpd.util.TypeNameMapping;

public class ChooseHandlerClassDialog extends ChooseItemDialog {
    private String type;

    public ChooseHandlerClassDialog(String type) {
        super(Localization.getString("ChooseClass.title"), Localization.getString("ChooseClass.message"), false);
        this.type = type;
    }

    public String openDialog() {
        try {
            setLabelProvider(new LabelProvider() {
                @Override
                public String getText(Object element) {
                    return TypeNameMapping.getTypeName((String) element);
                }
            });
            List<String> typeList = new ArrayList<String>();
            Set<String> typeNames = CustomizationRegistry.getHandlerClasses(type);
            for (String typeName : typeNames) {
                if (TypeNameMapping.showType(typeName)) {
                    typeList.add(typeName);
                }
            }
            Collections.sort(typeList, new MappedNameComparator());
            setItems(typeList);
            if (open() != IDialogConstants.CANCEL_ID) {
                return (String) getSelectedItem();
            }
        } catch (Exception e) {
            // ignore this and return null;
        }
        return null;

    }

    private static class MappedNameComparator implements Comparator<String> {

		public int compare(String o1, String o2) {
			String m1 = TypeNameMapping.getTypeName(o1);
			String m2 = TypeNameMapping.getTypeName(o2);
			return m1.compareTo(m2);
		}
    	
    }
}
