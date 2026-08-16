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
package com.microproject.pm.graphic.spreadsheet.selection.event;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.SwingUtilities;

import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetColumnMenu;
import com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheet;
/**
 *
 */
public class HeaderMouseListener extends MouseAdapter {
	protected SpreadSheet table;
	public HeaderMouseListener(SpreadSheet table) {
		super();
		this.table=table;
	}
	public void mouseClicked(MouseEvent e){
		int col = table.columnAtPoint(e.getPoint());
		if  (SwingUtilities.isLeftMouseButton(e)) {
			if (table.isColumnFullySelected(col)) {
				table.clearSelection();
				e.consume();
				return;
			}
			table.selectColumnAndAllRows(col);
		} else if (SwingUtilities.isRightMouseButton(e)) {
			if (table instanceof CommonSpreadSheet && ((CommonSpreadSheet)table).getSpreadSheetCategory() != null){
				CommonSpreadSheet sp=(CommonSpreadSheet)table;
				if (sp.isHasColumnHeaderPopup()) {
					SpreadSheetColumnMenu columnsPopup = new SpreadSheetColumnMenu(sp,col+1);
					columnsPopup.show(sp,e.getX(),e.getY());
				}
			}
		}

	}
}

