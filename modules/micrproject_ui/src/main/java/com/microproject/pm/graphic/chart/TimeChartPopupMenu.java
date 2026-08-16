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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;

import com.microproject.strings.Messages;

/**
 *
 */
public class TimeChartPopupMenu extends JPopupMenu {
	private static final long serialVersionUID = 8713091921383897550L;

	private static class MenuAction extends JRadioButtonMenuItem implements ActionListener {
    	/**
		 * 
		 */
		private static final long serialVersionUID = -7613080255726814444L;
		MenuAction(String text, boolean selected) {
    		super(text);
    		this.setSelected(selected);
    		this.addActionListener(this);
    	}
		public void actionPerformed(ActionEvent e) {
		}
    	
    }
    
    public TimeChartPopupMenu(TimeChartPanel panel) { // this isn't used anymore - the JFreeChart popup has taken its place
        super();
        //add(buildVerticalScrollingItem(panel));
    }
    
    public static JMenuItem buildVerticalScrollingItem(final TimeChartPanel p) {
		return new MenuAction(Messages.getString("TimeChartPopupMenu.VerticalScrolling"), p.isVerticalScrolling()){ //$NON-NLS-1$
			/**
			 * 
			 */
			private static final long serialVersionUID = -5312437474452353833L;

			public void actionPerformed(ActionEvent e) {
				super.actionPerformed(e);
				p.setVerticalScrolling(!p.isVerticalScrolling());
				if (!p.isVerticalScrolling()) p.revalidate();
			}
		};
    	
    }

}

