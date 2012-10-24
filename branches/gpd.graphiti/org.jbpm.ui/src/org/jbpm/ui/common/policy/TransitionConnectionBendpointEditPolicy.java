package org.jbpm.ui.common.policy;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.BendpointEditPolicy;
import org.eclipse.gef.requests.BendpointRequest;
import org.jbpm.ui.common.command.TransitionAbstractBendpointCommand;
import org.jbpm.ui.common.command.TransitionCreateBendpointCommand;
import org.jbpm.ui.common.command.TransitionDeleteBendpointCommand;
import org.jbpm.ui.common.command.TransitionMoveBendpointCommand;
import org.jbpm.ui.common.figure.GEFConstants;
import org.jbpm.ui.common.model.Transition;

public class TransitionConnectionBendpointEditPolicy extends BendpointEditPolicy {

    @Override
    protected Command getCreateBendpointCommand(BendpointRequest request) {
        TransitionCreateBendpointCommand command = new TransitionCreateBendpointCommand();
        fillCommand(request, command);
        return command;
    }

    @Override
    protected Command getDeleteBendpointCommand(BendpointRequest request) {
        TransitionDeleteBendpointCommand command = new TransitionDeleteBendpointCommand();
        fillCommand(request, command);
        return command;
    }

    @Override
    protected Command getMoveBendpointCommand(BendpointRequest request) {
        TransitionMoveBendpointCommand command = new TransitionMoveBendpointCommand();
        fillCommand(request, command);
        return command;
    }

    private void fillCommand(BendpointRequest request, TransitionAbstractBendpointCommand command) {
        Point location = request.getLocation();
        getConnection().translateToRelative(location);
        Point newLoc = getClosestPoint(location.x, location.y);
        command.setLocation(newLoc.x, newLoc.y);
        command.setTransitionDecorator((Transition) request.getSource().getModel());
        command.setIndex(request.getIndex());
    }

    private Point getClosestPoint(int x, int y) {
        int xCount = x / GEFConstants.GRID_SIZE;
        if (x - xCount * GEFConstants.GRID_SIZE > GEFConstants.GRID_SIZE / 2)
            xCount++;
        int yCount = y / GEFConstants.GRID_SIZE;
        if (y - yCount * GEFConstants.GRID_SIZE > GEFConstants.GRID_SIZE / 2)
            yCount++;

        return new Point(xCount * GEFConstants.GRID_SIZE, yCount * GEFConstants.GRID_SIZE);
    }

}
