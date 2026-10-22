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
package com.microproject.dialog;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.UIManager;

import com.microproject.util.FlatUiSupport;

public class ButtonPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	public ButtonPanel() {
	    super(new FlowLayout(FlowLayout.RIGHT, 6, 0));
	    addComponentListener(new java.awt.event.ComponentAdapter() {
			@Override
			public void componentResized(java.awt.event.ComponentEvent event) {
				if (getParent() != null) {
					getParent().revalidate();
				}
			}
	    });
	    javax.swing.border.Border border = UIManager.getBorder("OptionPane.border");
	    setBorder(border != null ? border : BorderFactory.createEmptyBorder());
	    FlatUiSupport.styleButtonPanel(this);
	}
	public void addButton(JButton button) {
		add(button);
	}
	@Override
	public Dimension getPreferredSize() {
		Dimension preferred = super.getPreferredSize();
		if (getWidth() <= 0 || getWidth() >= preferred.width) {
			return preferred;
		}
		Insets insets = getInsets();
		int availableWidth = Math.max(0, getWidth() - insets.left - insets.right - 12);
		int rowWidth = 0;
		int rowHeight = 0;
		int contentHeight = 0;
		int rows = 0;
		for (Component component : getComponents()) {
			if (!component.isVisible()) {
				continue;
			}
			Dimension size = component.getPreferredSize();
			int nextWidth = rowWidth == 0 ? size.width : rowWidth + 6 + size.width;
			if (rowWidth > 0 && nextWidth > availableWidth) {
				contentHeight += rowHeight;
				rows++;
				rowWidth = size.width;
				rowHeight = size.height;
			} else {
				rowWidth = nextWidth;
				rowHeight = Math.max(rowHeight, size.height);
			}
		}
		if (rowWidth > 0) {
			contentHeight += rowHeight;
			rows++;
		}
		return new Dimension(preferred.width,
			Math.max(preferred.height, contentHeight + insets.top + insets.bottom));
	}
}

