package ru.runa.gpd.lang.model;

import java.util.List;

public class EndState extends Node {

    @Override
    protected boolean allowLeavingTransition(List<Transition> transitions) {
        return false;
    }

}