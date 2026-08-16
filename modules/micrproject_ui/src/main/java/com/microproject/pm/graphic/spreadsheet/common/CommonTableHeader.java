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
package com.microproject.pm.graphic.spreadsheet.common;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.plaf.UIResource;

import com.microproject.util.FlatUiSupport;

public class CommonTableHeader extends JTableHeader {

	public CommonTableHeader() {
	}

	public CommonTableHeader(TableColumnModel cm) {
		super(cm);
		setDefaultRenderer(new SafeHeaderRenderer());
    }
    public void updateUI(){
    	super.updateUI();
    	if (!(getDefaultRenderer() instanceof SafeHeaderRenderer)) {
    		setDefaultRenderer(new SafeHeaderRenderer());
    	}
    	FlatUiSupport.applyTableHeaderStyle(this);
    	resizeAndRepaint();
    	invalidate();//PENDING
        }

    private static final class SafeHeaderRenderer extends DefaultTableCellRenderer implements UIResource {
    	private static final long serialVersionUID = 1L;

    	@Override
    	public Component getTableCellRendererComponent(javax.swing.JTable table, Object value, boolean isSelected,
    			boolean hasFocus, int row, int column) {
    		JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			boolean active = table != null && table.getSelectedColumn() == column;
    		FlatUiSupport.applyTableHeaderCellStyle(label, isSelected, active);
    		label.setHorizontalAlignment(CENTER);
    		return label;
    	}
    }

}

