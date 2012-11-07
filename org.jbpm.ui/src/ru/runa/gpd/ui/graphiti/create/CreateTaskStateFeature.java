package ru.runa.gpd.ui.graphiti.create;

import ru.runa.gpd.ui.graphiti.DiagramFeatureProvider;

public class CreateTaskStateFeature extends AbstractCreateNodeFeature {
    public static final String ID = "task-node";

    public CreateTaskStateFeature(DiagramFeatureProvider provider) {
        super(provider);
    }

    @Override
    protected String getNodeId() {
        return ID;
    }

}
