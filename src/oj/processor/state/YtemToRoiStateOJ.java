/*
 * YtemToRoiStateOJ.java
 */
package oj.processor.state;

import ij.IJ;
import ij.gui.ImageCanvas;
import ij.gui.Line;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Toolkit;
import oj.OJ;
import oj.project.CellOJ;
import oj.project.LocationOJ;
import oj.project.YtemDefOJ;
import oj.project.YtemOJ;
import oj.graphics.CustomCanvasOJ;
import oj.processor.ToolStateProcessorOJ;

public class YtemToRoiStateOJ extends ToolStateAdaptorOJ {

    Cursor defaultCursor = Toolkit.getDefaultToolkit().createCustomCursor(Toolkit.getDefaultToolkit().createImage(getClass().getResource("/oj/processor/state/resources/ObjectToRoiCursor32.png")), new Point(9, 9), "Object To ROI");

    public YtemToRoiStateOJ() {
        setCanvasCursor();
    }

    public int getToolState() {
        return ToolStateProcessorOJ.STATE_OBJECT_TOOL;
    }
    
    public void mousePressed(String imageName, int sliceNo, double x, double y, int flags) {
        super.mousePressed(imageName, sliceNo, x, y, flags);
        int imgIndex = OJ.getData().getImages().getIndexOfImage(imageName);
        Object[] closestPt = OJ.getData().getCells().closestPoint(imgIndex, x, y, sliceNo);
        CellOJ cell = (CellOJ) closestPt[0];
        YtemOJ ytm = (YtemOJ) closestPt[1];
        LocationOJ loc = (LocationOJ) closestPt[2];
        if (loc == null) {

        } else {
            YtemDefOJ ytem_def = OJ.getData().getYtemDefs().getYtemDefByName(ytm.getYtemDef());
            int[] xcoords = ytm.toXArray();
            int[] ycoords = ytm.toYArray();
            Roi roi;
            switch (ytem_def.getYtemType()) {
                case YtemDefOJ.YTEM_TYPE_ANGLE:
                    roi = new PolygonRoi(xcoords, ycoords, xcoords.length, IJ.getImage(), Roi.POINT);
                    IJ.getImage().setRoi(roi);
                    break;
                case YtemDefOJ.YTEM_TYPE_LINE:
                    roi = new Line(xcoords[0], ycoords[0], xcoords[1], ycoords[1], IJ.getImage());
                    IJ.getImage().setRoi(roi);
                    break;
                case YtemDefOJ.YTEM_TYPE_POINT:
                    //this is a workaround - this is how should be also for the rest but it is a bug in creating the ROIs
                    //for the rest of the ROI types - the image is set after the x,y position are set so the image magnification is
                    //not used in creating a ROI of type LINE or POLYGON
                    roi = new PointRoi(IJ.getImage().getCanvas().screenX(xcoords[0]), IJ.getImage().getCanvas().screenY(ycoords[0]), IJ.getImage());
                    IJ.getImage().setRoi(roi);
                    break;
                case YtemDefOJ.YTEM_TYPE_POLYGON:
                    roi = new PolygonRoi(xcoords, ycoords, xcoords.length, IJ.getImage(), Roi.POLYGON);
                    IJ.getImage().setRoi(roi);
                    break;
                case YtemDefOJ.YTEM_TYPE_ROI:
                    roi = new PolygonRoi(xcoords, ycoords, xcoords.length, IJ.getImage(), Roi.FREEROI);
                    IJ.getImage().setRoi(roi);
                    break;
                case YtemDefOJ.YTEM_TYPE_SEGLINE:
                    roi = new PolygonRoi(xcoords, ycoords, xcoords.length, IJ.getImage(), Roi.POLYLINE);
                    IJ.getImage().setRoi(roi);
                    break;
                default:
                //
                }
        }
        setCanvasCursor();
    }

    public void mouseEntered(String imageName, int stackIndex, double x, double y, int flags) {
        super.mouseEntered(imageName, stackIndex, x, y, flags);
        setCanvasCursor();
    }

    public Cursor getDefaultCursor() {
        return defaultCursor;
    }

    private void setCanvasCursor() {
        ImageCanvas ic = getCanvas();
        if ((ic != null) && (ic instanceof CustomCanvasOJ)) {
            if (ic instanceof CustomCanvasOJ) {
                ic.setCursor(defaultCursor);
            } else {
                ic.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            }
        }
    }
}
