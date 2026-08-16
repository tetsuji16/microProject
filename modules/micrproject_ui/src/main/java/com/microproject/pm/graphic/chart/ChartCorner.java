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
package com.microproject.pm.graphic.chart;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.microproject.pm.graphic.spreadsheet.common.GradientCorner;

/**
 *
 */
public class ChartCorner extends GradientCorner {
	private static final long serialVersionUID = 2835631007345336881L;
	protected TimeChartPanel chart;
	/**
	 * 
	 */
	public ChartCorner(TimeChartPanel chart) {
		super();
		this.chart=chart;
		setBackground(UIManager.getColor("TableHeader.cellBackground"));
		setBorder (UIManager.getBorder ("TableHeader.cellBorder"));
		addMouseListener(new MouseAdapter(){
			public void mousePressed(MouseEvent e){
				TimeChartPanel chart=ChartCorner.this.chart;

				if  (SwingUtilities.isRightMouseButton(e)) {
					JPopupMenu popup = chart.getPopupMenu();
					// not using this anymoreTimeChartPopupMenu popup = new TimeChartPopupMenu(chart);
					popup.show(chart,e.getX(),e.getY());

				}			
			}
		});
	}

}

