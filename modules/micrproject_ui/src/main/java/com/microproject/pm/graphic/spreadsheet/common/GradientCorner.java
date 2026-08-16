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

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.JComponent;
import com.microproject.util.FlatUiSupport;

/**
 *
 */
public class GradientCorner extends JComponent {
	protected boolean selected;
	public GradientCorner() {
		super();
	}
	
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Color background = selected
			? FlatUiSupport.spreadsheetHeaderSelectedBackground()
			: FlatUiSupport.spreadsheetHeaderBackground();
		g.setColor(background);
		g.fillRect(0, 0, getWidth(), getHeight());
		g.setColor(FlatUiSupport.spreadsheetGridColor());
		g.drawRect(0, 0, Math.max(0, getWidth() - 1), Math.max(0, getHeight() - 1));
		if (selected) {
			g.setColor(FlatUiSupport.spreadsheetActiveCellBorderColor());
			g.drawRect(0, 0, Math.max(0, getWidth() - 1), Math.max(0, getHeight() - 1));
		}
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		if (this.selected != selected){
			//System.out.println("selected="+selected);
			this.selected = selected;
			repaint();
		}
	}
	
	

}

