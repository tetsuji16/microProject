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
package com.microproject.toolbar;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseEvent;

import javax.swing.JComboBox;
import javax.swing.JToolTip;
import javax.swing.UIManager;

import com.microproject.menu.HyperLinkToolTip;
import com.microproject.menu.MenuManager;
import com.microproject.menu.MenuTextKeys;
import com.microproject.grouping.core.transform.CommonTransformFactory;
import com.microproject.grouping.core.transform.ViewConfiguration;

/**
 * 
 */
public class TransformComboBox extends JComboBox {
	protected int type;
	public TransformComboBox(MenuManager menuManager, String command,int type) {
		super(new TransformComboBoxModel(type));
		setActionCommand(command);
		this.type=type;
		setMaximumSize(new Dimension(150,Integer.MAX_VALUE));
		String tip;
		if (menuManager != null) { // for the combox on top of the screen, use the properties file
			String text = menuManager.getString(command + MenuTextKeys.TOOLTIP_SUFFIX);
			String help = menuManager.getStringOrNull(command + MenuTextKeys.HELP_SUFFIX);
			String demo = menuManager.getStringOrNull(command + MenuTextKeys.DEMO_SUFFIX);
			String doc = menuManager.getStringOrNull(command + MenuTextKeys.DOC_SUFFIX);
			tip = HyperLinkToolTip.helpTipText(text, help, demo, doc);
		} else { // for the one in the histogram, just show filter text
			tip =((TransformComboBoxModel)getModel()).getTipText();
		}
		setToolTipText(tip);
	}
	public void setView(ViewConfiguration view){
		((TransformComboBoxModel)getModel()).setView(view);
		Object selectedItem = getModel().getSelectedItem();
		if (selectedItem != null && getItemCount() > 0) {
			setSelectedItem(selectedItem);//JComboBox local state update
		}
	}
	public Point getToolTipLocation(MouseEvent event) { // the tip MUST be touching the button if html because you can click on links
		String tipText = getToolTipText();
		if (tipText != null && tipText.startsWith("<html>"))
			return new Point(0, getHeight()-2);
		else
			return super.getToolTipLocation(event);
	}

	public JToolTip createToolTip() {
		String tipText = getToolTipText();
		if (tipText != null && tipText.startsWith("<html>")) {
			JToolTip tip = new HyperLinkToolTip();
			tip.setComponent(this);
			return tip;
		} else {
			return super.createToolTip();
		}
	}
	
	public void paintComponent(Graphics graphics) {
		boolean none =  (getSelectedIndex() <= 0);
		setForeground(none ? UIManager.getColor("ComboBox.foreground") : Color.RED); //$NON-NLS-1$
		super.paintComponent(graphics);
	}
	public void transformBasedOnValue() {
		CommonTransformFactory factory = (CommonTransformFactory)getSelectedItem();
		((TransformComboBoxModel)getModel()).changeTransform(factory);
	}

}

