package org.jbpm.ui.jpdl3.figure;

import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.geometry.Rectangle;
import org.jbpm.ui.SharedImages;
import org.jbpm.ui.common.figure.StateFigure;

public class MailNodeFigure extends StateFigure {

    @Override
    public void init(boolean bpmnNotation) {
        super.init(bpmnNotation);
        addLabel();
    }

    @Override
    protected void paintUMLFigure(Graphics g, Rectangle r) {
        super.paintUMLFigure(g, r);
        g.drawImage(SharedImages.getImage("icons/uml/mail_envelope.gif"), 5, 5);
    }
    
}
