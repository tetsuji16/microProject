/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2012-2019 ProjectLibre, Inc.  (Previous Copyright Holder)
 * Copyright (c) 2026 microProject
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
package com.microproject.pm.graphic.spreadsheet;

import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import com.microproject.menu.MenuActionConstants;
import com.microproject.graphic.configuration.SpreadSheetCategories;
import com.microproject.pm.graphic.IconManager;
import com.microproject.strings.Messages;
import com.microproject.util.Environment;

/**
 *
 */
public class SpreadSheetPopupMenu extends JPopupMenu {
	   protected int row;
	    protected int col;
	    protected final SpreadSheet spreadSheet;
	    /**
	     * 
	     */
	    public SpreadSheetPopupMenu(SpreadSheet spreadSheet) {
	        super();
	        this.spreadSheet=spreadSheet;
	        
	        //setLabel("");
	        final SpreadSheet sp=spreadSheet;
			String[] actions=spreadSheet.getActionList();
			if (SpreadSheetCategories.taskSpreadsheetCategory
					.equals(spreadSheet.getSpreadSheetCategory())) {
				add(new AbstractAction(Messages.getString("TaskInformationDialog.TaskInformation")) {
					private static final long serialVersionUID = 1L;

					@Override
					public void actionPerformed(ActionEvent e) {
						spreadSheet.doDoubleClick(row, col);
					}
				}, Environment.isNewLook() ? "menu24.taskInformation" : "menu.taskInformation");
				if (actions != null && actions.length > 0) {
					addSeparator();
				}
			}
			
			//Normal spreadsheet
			//NodeListTransferHandler.registerWith(this);
			if (actions!=null)
			for (int i=0;i<actions.length;i++){
				String actionId = actions[i];
				add(spreadSheet.prepareAction(actionId),getMenuAction(actionId));
				if (MenuActionConstants.ACTION_PASTE.equals(actionId)) {
					add(spreadSheet.prepareAction(MenuActionConstants.ACTION_PASTE_INSERT), getInsertPasteMenuIcon());
				}
			}
		}
	    
    private Map<String, String> menuActionMap = null;
	    protected String getMenuAction(String action){
	    	if (menuActionMap==null){
                menuActionMap = new HashMap<>();
	    		if (Environment.isNewLook()) {
		    		menuActionMap.put(MenuActionConstants.ACTION_NEW,"menu24.insertTask");
		    		menuActionMap.put(MenuActionConstants.ACTION_DELETE,"menu24.delete");
		    		menuActionMap.put(MenuActionConstants.ACTION_INDENT,"menu24.indent");
		    		menuActionMap.put(MenuActionConstants.ACTION_OUTDENT,"menu24.outdent");
		    		menuActionMap.put(MenuActionConstants.ACTION_CUT,"menu24.cut");
		    		menuActionMap.put(MenuActionConstants.ACTION_COPY,"menu24.copy");
		    		menuActionMap.put(MenuActionConstants.ACTION_PASTE,"menu24.paste");
		    		menuActionMap.put(MenuActionConstants.ACTION_EXPAND,"menu24.expand");
		    		menuActionMap.put(MenuActionConstants.ACTION_COLLAPSE,"menu24.collapse");
	    		} else {
		    		menuActionMap.put(MenuActionConstants.ACTION_NEW,"menu.insertTask");
		    		menuActionMap.put(MenuActionConstants.ACTION_DELETE,"menu.delete");
		    		menuActionMap.put(MenuActionConstants.ACTION_INDENT,"menu.rightArrow");
		    		menuActionMap.put(MenuActionConstants.ACTION_OUTDENT,"menu.leftArrow");
		    		menuActionMap.put(MenuActionConstants.ACTION_CUT,"menu.cut");
		    		menuActionMap.put(MenuActionConstants.ACTION_COPY,"menu.copy");
		    		menuActionMap.put(MenuActionConstants.ACTION_PASTE,"menu.paste");
		    		menuActionMap.put(MenuActionConstants.ACTION_EXPAND,"menu.expand");
		    		menuActionMap.put(MenuActionConstants.ACTION_COLLAPSE,"menu.collapse");
	    		}
	    	}
            return menuActionMap.get(action);
        }

	    protected String getInsertPasteMenuIcon() {
	    	return Environment.isNewLook() ? "menu24.insertTask" : "menu.insertTask";
	    }
	    
	    private void add(Action action, String iconName) {
	    	JMenuItem menuItem = new JMenuItem(action);
	    	menuItem.setIcon(IconManager.getIcon(iconName));
	    	add(menuItem);
	    }

	    /**
	     * @return Returns the col.
	     */
	    public int getCol() {
	        return col;
	    }
	    /**
	     * @param col The col to set.
	     */
	    public void setCol(int col) {
	        this.col = col;
	    }
	    /**
	     * @return Returns the row.
	     */
	    public int getRow() {
	        return row;
	    }
	    /**
	     * @param row The row to set.
	     */
	    public void setRow(int row) {
	        this.row = row;
	    }
    
    

		public SpreadSheet getSpreadSheet() {
			return spreadSheet;
		}
}

