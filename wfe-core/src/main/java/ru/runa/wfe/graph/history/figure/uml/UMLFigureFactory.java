/*
 * This file is part of the RUNA WFE project.
 * 
 * This program is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU Lesser General Public License 
 * as published by the Free Software Foundation; version 2.1 
 * of the License. 
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the 
 * GNU Lesser General Public License for more details. 
 * 
 * You should have received a copy of the GNU Lesser General Public License 
 * along with this program; if not, write to the Free Software 
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 */
package ru.runa.wfe.graph.history.figure.uml;

import ru.runa.wfe.InternalApplicationException;
import ru.runa.wfe.graph.history.figure.AbstractFigure;
import ru.runa.wfe.graph.history.figure.AbstractFigureFactory;
import ru.runa.wfe.graph.history.figure.TransitionFigureBase;
import ru.runa.wfe.graph.history.model.NodeModel;
import ru.runa.wfe.graph.history.model.TransitionModel;

public class UMLFigureFactory extends AbstractFigureFactory {

    @Override
    public AbstractFigure createFigure(NodeModel nodeModel, boolean useEgdingOnly) {
        AbstractFigure figure = null;
        switch (nodeModel.getType()) {
        case TASK_STATE:
            figure = new TaskNodeFigure();
            break;
        case MULTI_TASK_STATE:
            figure = new MultiTaskNodeFigure();
            break;
        case DECISION:
            figure = new DecisionFigure();
            break;
        case MERGE:
            figure = new ConjunctionFigure();
            break;
        case FORK:
        case JOIN:
            figure = new ForkJoinFigure();
            break;
        case START_EVENT:
            figure = new StartStateFigure();
            break;
        case END_PROCESS:
            figure = new EndStateFigure();
            break;
        case END_TOKEN:
            figure = new EndTokenStateFigure();
            break;
        case SUBPROCESS:
            figure = new SubprocessFigure();
            break;
        case ACTION_NODE:
            figure = new ActionNodeFigure();
            break;
        case WAIT_STATE:
            figure = new WaitStateFigure();
            break;
        case MULTI_SUBPROCESS:
            figure = new MultiSubprocessFigure();
            break;
        case SEND_MESSAGE:
            figure = new SendMessageNodeFigure();
            break;
        case RECEIVE_MESSAGE:
            figure = new ReceiveMessageNodeFigure();
            break;
        default:
            throw new InternalApplicationException("Unexpected figure type found: " + nodeModel.getType());
        }
        figure.initFigure(nodeModel, useEgdingOnly);
        return figure;
    }

    @Override
    public TransitionFigureBase createTransitionFigure(TransitionModel transitionModel, AbstractFigure figureFrom, AbstractFigure figureTo) {
        return new TransitionFigure();
    }
}