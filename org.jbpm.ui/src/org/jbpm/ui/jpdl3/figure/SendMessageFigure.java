package org.jbpm.ui.jpdl3.figure;

import org.eclipse.draw2d.ChopboxAnchor;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.draw2d.geometry.Rectangle;
import org.jbpm.ui.SharedImages;

public class SendMessageFigure extends MessageNodeFigure {

    @Override
    public void init(boolean bpmnNotation) {
        super.init(bpmnNotation);
        if (!bpmnNotation) {
            this.connectionAnchor = new SendMessageNodeAnchor(this);
            addEmptySpace(1, GRID_SIZE);
        }
    }

    @Override
    protected void paintBPMNFigure(Graphics g, Rectangle r) {
        g.drawImage(SharedImages.getImage("icons/bpmn/graph/sendmessage.png"), r.getLocation());
    }

    @Override
    protected void paintUMLFigure(Graphics g, Rectangle r) {
        g.translate(getLocation());
        int halfHeight = Math.round(getSize().height / 2);
        int xRight = (int) ((getSize().width - 1) - halfHeight * Math.tan(Math.PI / 6));
        PointList points = new PointList(5);
        points.addPoint(xRight, 0);
        points.addPoint(0, 0);
        points.addPoint(0, getSize().height - 1);
        points.addPoint(xRight, getSize().height - 1);
        points.addPoint(getSize().width - 1, halfHeight);
        g.drawPolygon(points);
    }

    static class SendMessageNodeAnchor extends ChopboxAnchor {

        public SendMessageNodeAnchor(IFigure owner) {
            super(owner);
        }

        @Override
        public Point getLocation(Point reference) {
            Rectangle r = Rectangle.SINGLETON;
            r.setBounds(getOwner().getBounds());
            getOwner().translateToAbsolute(r);
            Point ref = r.getCenter().negate().translate(reference);
            if (ref.x > 0) {
                double p = (r.width - r.height * Math.tan(Math.PI / 6)) / 2;
                double cutOffAngle = Math.atan(r.height / (2 * p));
                double refAngle = Math.atan((double) ref.y / ref.x);
                if (Math.abs(refAngle) < cutOffAngle) {
                    double k1 = (double) ref.y / ref.x;
                    double b1 = 0;
                    double k2 = r.height / (2 * p - r.width);
                    if (ref.y < 0) {
                        k2 = -1 * k2;
                    }
                    double b2 = -r.width * k2 / 2;
                    double dx = (b2 - b1) / (k1 - k2);
                    double dy = dx * k1 + b1;
                    return new Point(Math.round(r.getCenter().x + dx), Math.round(r.getCenter().y + dy));
                }
            }
            return super.getLocation(reference);
        }
    }

}
