package ru.runa.gpd.ui.graphiti.create;

import ru.runa.gpd.ui.graphiti.DiagramFeatureProvider;

public class CreateMultiInstanceFeature extends AbstractCreateNodeFeature {
    public static final String ID = "multiinstance-state";

    public CreateMultiInstanceFeature(DiagramFeatureProvider provider) {
        super(provider);
    }

    @Override
    protected String getNodeId() {
        return ID;
    }

}
