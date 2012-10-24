package org.jbpm.ui.common.command;

import org.eclipse.gef.commands.Command;
import org.jbpm.ui.common.model.Node;

public class NodeSetNameCommand extends Command {

    private String oldName;

    private String newName;

    private Node node;

    public void setName(String name) {
        if (name == null) {
            name = "";
        }
        newName = name;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    @Override
    public void execute() {
        oldName = node.getName();
        node.setName(newName);
    }

    @Override
    public boolean canExecute() {
        return node.canSetNameTo(newName);
    }

    @Override
    public void undo() {
        node.setName(oldName);
    }

}
