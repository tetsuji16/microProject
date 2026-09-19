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
package com.microproject.graphic.configuration.cellstyles;

import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.graphic.configuration.CellFormat;
import com.microproject.graphic.configuration.CellStyle;


public class AssignmentCellStyle implements CellStyle {
	protected CellFormat format=new CellFormat();
	public AssignmentCellStyle(){
		
	}
	public CellFormat getCellFormat(Object object) {
		GraphicNode node=(GraphicNode)object;
		//CellFormat format=new CellFormat();		
		format.reset();
		format.setBold(node.isSummary());
		// Assignment rows use the standard spreadsheet font. The former italic
		// treatment conflicts with the FlatLaf table typography.
		format.setItalic(false);
		format.setCompositeIcon(node.isSummary()||node.isValidLazyParent());
		if (node.isGroup()){
			if (node.getLevel()==1) format.setBackground("TAN");
			else format.setBackground("LINEN");
		}
		// Ordinary assignments inherit the table's alternating background instead
		// of the legacy yellow highlight.
		return format;
	}
}

