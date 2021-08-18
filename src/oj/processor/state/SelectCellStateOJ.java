/*
 * SelectCellStateOJ.java
 */
package oj.processor.state;

import ij.gui.ImageCanvas;
import java.awt.Cursor;
import java.awt.Event;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

import oj.OJ;
import oj.project.CellOJ;
import oj.project.LocationOJ;
import oj.project.YtemOJ;
import oj.project.results.QualifiersOJ;
import oj.graphics.CustomCanvasOJ;
import oj.processor.ToolStateProcessorOJ;
import oj.processor.events.YtemDefSelectionChangedEventOJ;
import oj.processor.events.YtemDefSelectionChangedListenerOJ;
import oj.gui.tools.ToolManagerOJ;
import oj.plugin.GlassWindowOJ;

public class SelectCellStateOJ extends ToolStateAdaptorOJ {

    private enum Mode { // @ TODO : Modified to allow switching point type

        SELECT, QUALIFY, SWITCHTYPE
    }
    
    private enum PropagateType { // @ TODO : Propagate switchtype in one/two directions
    	SINGLE, FORWARD, BACKWARD, BIDIRECTIONAL
    }
    
    private double yPos = 0;
    private double xPos = 0;
    private Mode stateMode = Mode.SELECT;
    private PropagateType propType = PropagateType.SINGLE;
    private Cursor selectCursor = Toolkit.getDefaultToolkit().createCustomCursor(Toolkit.getDefaultToolkit().createImage(getClass().getResource("/oj/processor/state/resources/SelectCellCursor32.png")), new Point(14, 8), "Select Cell");
    private Cursor qualifyCursor = Toolkit.getDefaultToolkit().createCustomCursor(Toolkit.getDefaultToolkit().createImage(getClass().getResource("/oj/processor/state/resources/SelectCellCursorQ32.png")), new Point(14, 8), "Qualify Cell");
    private int currYtemDef = 0; // added to allow changing to particular ytem def

    public SelectCellStateOJ() {
        setCanvasCursor();
    }

    public int getToolState() {
        return ToolStateProcessorOJ.STATE_OBJECT_TOOL;
    }

    public void cleanup() {
        yPos = 0;
        xPos = 0;
        OJ.getDataProcessor().unselectCell();
    }

    public void mousePressed(String imageName, int sliceNo, double x, double y, int flags) {//sliceNo is 1-based
        super.mousePressed(imageName, sliceNo, x, y, flags);


        int imgIndex = OJ.getData().getImages().getIndexOfImage(imageName);
        Object[] closestPt = OJ.getData().getCells().closestPoint(imgIndex, x, y, sliceNo);
        CellOJ cell = (CellOJ) closestPt[0];
        YtemOJ ytm = (YtemOJ) closestPt[1];
        LocationOJ loc = (LocationOJ) closestPt[2];
        if (loc == null) {
            OJ.getDataProcessor().unselectCell();
        } else {
            double dd = ((Double) closestPt[3]).doubleValue();//distance mouse to point in pixels: not used here
            int cell_index = OJ.getData().getCells().indexOfCell(cell);
            int ytem_index = cell.indexOfYtem(ytm);
            int location_index = ytm.indexOf(loc);
            updateState(flags);
            switch (stateMode) {
                case QUALIFY:
                    OJ.getData().getResults().getQualifiers().setQualifyMethod(QualifiersOJ.QUALIFY_METHOD_ARBITRARY, true);
                    cell.setQualified(!cell.isQualified());
                    OJ.getEventProcessor().fireCellChangedEvent();//30.8.2013
                    //setCanvasCursor();
                    break;
                default:
                    if ((xPos == x) && (yPos == y)) {
                        ToolManagerOJ.getInstance().selectTool("Marker");
                        ((CreateCellStateOJ) OJ.getToolStateProcessor().getToolStateObject()).openCell(cell_index, ytem_index);
                    } else {
                        OJ.getDataProcessor().selectCell(cell_index);
                    }
                    // @ TODO : added code to do some other stuff with changing type
                    // before doing anything: determine if changing type is desired by user
                    if (stateMode == Mode.SWITCHTYPE)
                    {
                    	// step one: open ytem for editing
                    	YtemOJ existingYtem = cell.getSelectedYtem();
                    	existingYtem.setOpen(true);
                    	
                    	// step two: determine next type based on current selection
                    	String nextYtemDefName = OJ.getMacroProcessor().activeYtemName();
                    	
                    	// step three: change type of the ytem
                    	existingYtem.setObjectDef(nextYtemDefName);

                    	// step four: close the ytem
                    	existingYtem.setOpen(false);
                    	
                    	//step five: if applicable, change all other ytems of that number
                    	int currImageIdx = OJ.getImageProcessor().getCurrentImageIndex();
                    	int firstCellNum = OJ.getData().getImages().getImageByIndex(currImageIdx).getFirstCell();
                    	int currCellNumRelativized = OJ.getData().getCells().getCellIndex(existingYtem.getCell()) - firstCellNum + 1;
                    	
                    	if (currImageIdx != -1) // only work if image is linked to same objectJ thing
                    	{
                    		switch (propType) {
                    			case FORWARD:
                    				// 	iterate over images linked to project
                    				for (int x0 = currImageIdx + 1; x0 < OJ.getData().getImages().getImagesCount(); x0++) {
                    					int imgFirstCell = OJ.getData().getImages().getImageByIndex(x0).getFirstCell();
                    					int newCellIndex = currCellNumRelativized + imgFirstCell - 1;
                    					if (newCellIndex <= OJ.getData().getImages().getImageByIndex(x0).getLastCell()) { // corresponding cell number exists in other session
                    						changeCellDef(newCellIndex, OJ.getData().getCells().getCellByIndex(newCellIndex));
                    					}
                    				}
                    				break;
                    			case BACKWARD:
                    				break;
                    			case BIDIRECTIONAL:
                    				break;
                    			default:
                    				break;
                    		}
                    	}
                    	
                    }
                    xPos = x;
                    yPos = y;
            }
        }
        setCanvasCursor();
    }
    
    private void changeCellDef(int cellIdx, CellOJ cell)
    // helper function to select cells, then change their types when propagating cell types across multiple imaging sessions
    {
    	OJ.getDataProcessor().selectCell(cellIdx);
    	// step one: open ytem for editing
    	YtemOJ existingYtem = cell.getSelectedYtem();
    	existingYtem.setOpen(true);
    	
    	// step two: determine next type based on current selection
    	String nextYtemDefName = OJ.getMacroProcessor().activeYtemName();
    	
    	// step three: change type of the ytem
    	existingYtem.setObjectDef(nextYtemDefName);

    	// step four: close the ytem
    	existingYtem.setOpen(false);
    }
    
    private void updateState(int flags) {//30.9.2009 // except not really i borrowed this from MoveCellStateOJ

//      boolean isShift = ShortcutManagerOJ.getInstance().isShiftPressed();//didn't work on Windows 21.6.2009
//      boolean isAlt = ShortcutManagerOJ.getInstance().isAltPressed();
      boolean isShift = (flags & KeyEvent.SHIFT_MASK) != 0;
      boolean isAlt = (flags & KeyEvent.ALT_MASK) != 0;

      //       ij.IJ.log("updateState: shift = " + isShift);
      //       ij.IJ.log("updateState: alt = " + isAlt);
      if (isShift && !isAlt) {
          propType = PropagateType.FORWARD;
      } else if (!isShift && isAlt) {
          propType = PropagateType.BACKWARD;
      } else if (isShift && isAlt) {
    	  propType = PropagateType.BIDIRECTIONAL;
      } else {
          propType = PropagateType.SINGLE;
      }
      setCanvasCursor();
  }

    public void mouseEntered(String imageName, int stackIndex, double x, double y, int flags) {
        super.mouseEntered(imageName, stackIndex, x, y, flags);
        setCanvasCursor();
        if (GlassWindowOJ.exists()) {// &&  false) {
            GlassWindowOJ.getInstance().setCursor(selectCursor);
        }
    }

    public void keyPressed(String imageName, int stackIndex, int keyCode, int flags) {
    	// assign stateMode of pointer tool
        if (keyCode == KeyEvent.VK_Q) {
            stateMode = Mode.QUALIFY;
            setCanvasCursor();
        }
        if (keyCode == KeyEvent.VK_C) {
        	stateMode = Mode.SWITCHTYPE;
        	setCanvasCursor();
        }
        
        // @ TODO : shift and alt controlling behavior
        if (((flags & Event.SHIFT_MASK) != 0) && ((flags & Event.ALT_MASK) == 0)) {
        	propType = PropagateType.FORWARD;
        	setCanvasCursor();
        }
        else if (((flags & Event.SHIFT_MASK) == 0) && ((flags & Event.ALT_MASK) != 0)) {
        	propType = PropagateType.BACKWARD;
        	setCanvasCursor();
        }
        else if (((flags & Event.SHIFT_MASK) != 0) && ((flags & Event.ALT_MASK) != 0)) {
        	propType = PropagateType.BIDIRECTIONAL;
        	setCanvasCursor();
        }
        else {
        	propType = PropagateType.SINGLE;
        }
        // end shift and alt controlling behavior
        
        
        // assign which ytem def to use in modifying point
        if (keyCode == KeyEvent.VK_1) {
        	currYtemDef = 0;
        }
        if (keyCode == KeyEvent.VK_2) {
        	currYtemDef = 1;
        }
        if (keyCode == KeyEvent.VK_3) {
        	currYtemDef = 2;
        }
        if (keyCode == KeyEvent.VK_4) {
        	currYtemDef = 3;
        }
        if (keyCode == KeyEvent.VK_5) {
        	currYtemDef = 4;
        }
        if (keyCode == KeyEvent.VK_6) {
        	currYtemDef = 5;
        }
        if (keyCode == KeyEvent.VK_7) {
        	currYtemDef = 6;
        }
        if (keyCode == KeyEvent.VK_8) {
        	currYtemDef = 7;
        }
        if (keyCode == KeyEvent.VK_9) {
        	currYtemDef = 8;
        }
        if (keyCode == KeyEvent.VK_0) {
        	currYtemDef = 9;
        }
    }

    public void keyReleased(String imageName, int stackIndex, int keyCode, int flags) {
        if (keyCode == KeyEvent.VK_Q) {
            stateMode = Mode.SELECT;
        }
        if (keyCode == KeyEvent.VK_C) {
        	stateMode = Mode.SELECT;
        }       
        propType = PropagateType.SINGLE;
        setCanvasCursor();
    }

    public boolean isQualifyMode() {
        return (stateMode == Mode.QUALIFY);
    }

    public Cursor getDefaultCursor() {
        return selectCursor;
    }

    private void setCanvasCursor() {
        ImageCanvas ic = getCanvas();
        Cursor cursor = null;
        if ((ic != null) && (ic instanceof CustomCanvasOJ)) {
            if (ic instanceof CustomCanvasOJ) {
                switch (stateMode) {
                    case QUALIFY:
                        cursor =qualifyCursor;
                        ic.setCursor(qualifyCursor);
                    default:
                        cursor = selectCursor;
                }
            } else {
                cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
            }
        }
        if (cursor != null){
           ic.setCursor(cursor);
           if (GlassWindowOJ.exists()) {
                GlassWindowOJ.getInstance().setCursor(cursor);
            }
        }
    }
}
