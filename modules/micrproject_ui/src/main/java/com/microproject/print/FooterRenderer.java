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
package com.microproject.print;

import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.JLabel;

import com.microproject.pm.graphic.Renderer;
import com.microproject.pm.graphic.graph.GraphParams;
import com.microproject.strings.Messages;

public class FooterRenderer extends Renderer {

	public FooterRenderer(GraphParams graphInfo) {
		super(graphInfo);
	}

	public FooterRenderer() {
	}

	public void paint(Graphics g) {
	}

	protected JLabel footerLabel;
	protected int lastPage=-1;
	protected Rectangle lastBounds;
	public void paint(Graphics g,int page,Rectangle bounds,String name) {
		int pageCount=graphInfo.getPrintCols()*graphInfo.getPrintRows();
			if (footerLabel==null){
				footerLabel=new JLabel();
			}
			if (lastPage!=page||lastBounds!=bounds){
				footerLabel.setText(name+(pageCount>1?(Messages.getString("FooterRenderer.page")+(page+1)):"")); //$NON-NLS-1$
				footerLabel.setHorizontalTextPosition(JLabel.CENTER);
				footerLabel.setHorizontalAlignment(JLabel.CENTER);
				footerLabel.setDoubleBuffered(false);
				footerLabel.setOpaque(false);
				//footerLabel.setForeground(Color.BLACK);
				footerLabel.setSize(bounds.width, bounds.height);
			}
	    	g.translate(bounds.x,bounds.y);
	    	footerLabel.doLayout();
	    	footerLabel.print(g);
	    	g.translate(-bounds.x,-bounds.y);
	}

}

