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
package com.microproject.pm.graphic.laf;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.LookAndFeel;

public interface LafManager {

	public abstract void clean();

	public abstract LookAndFeel getPlaf();

	public abstract void initLookAndFeel();

	public abstract void setColorTheme(String viewName);

	public abstract void changePalette();

	public abstract boolean isChangePaletteAllowed(LookAndFeel lookAndFeel);

	public abstract void paintComponent(Graphics g, Component component,
			boolean selected);

	public abstract void setUI(JTabbedPane component);

	public abstract void setColorScheme(JComponent component);

	public abstract void paintTimeScale(Graphics2D g2, int x, int y, int w,
			int h, Shape[] shapes);
	
	public abstract Color getSelectedBackgroundColor();
	
	public abstract Color getUnselectedBackgroundColor();

	public abstract void dumpUIValues();
	public boolean isToolbarOpaque();
}

