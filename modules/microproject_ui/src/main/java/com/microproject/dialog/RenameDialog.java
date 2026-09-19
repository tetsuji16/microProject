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

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.configuration.Dictionary;
import com.microproject.configuration.NamedItem;
import com.microproject.strings.Messages;
import com.microproject.util.FlatUiSupport;
import com.microproject.util.Alert;

public final class RenameDialog extends AbstractDialog {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2400103374793798171L;
	JLabel oldName;
	JTextField newName;

	NamedItem namedItem;
	String result = null;
//	private RenameDialog instance = null; // Single-instance handling would need graphic-manager coordination.
	
	public static boolean doRename(Component component, NamedItem namedItem) {
		String value = getValue(component,namedItem);
		if (value == null || value.equals(namedItem.getName()))
			return false;
		Dictionary.rename(namedItem,value);
		return true;
	}

	public static String getValue(Component component, NamedItem namedItem) {
		RenameDialog dlg = getInstance(component,namedItem);
		if (dlg.doModal())
			return dlg.getResult();
		return null;
	}

	public static RenameDialog getInstance(Component component, NamedItem namedItem) {
		return new RenameDialog(component,namedItem);

		//		if (instance == null) {
//			instance = new RenameDialog(component,namedItem);
//		} else {
//			instance.namedItem = namedItem;
//			instance.bind(true);
//		}
//		return instance;
			
	}
	public final String getResult() {
		return result;
	}
	
	private RenameDialog(Component component, NamedItem namedItem) {
		super(GraphicManager.getInstance(component).getFrame(), Messages.getString("RenameDialog.Rename"), true); //$NON-NLS-1$
		this.namedItem = namedItem;
		oldName = new JLabel();
		newName = new JTextField();

	}

	// Component Creation and Initialization **********************************

	/**
	 * Creates, intializes and configures the UI components. Real applications
	 * may further bind the components to underlying models.
	 */
	protected void initControls() {
		bind(true);
	}

	protected boolean bind(boolean get) {
		if (get) {
			oldName.setText(namedItem.getName());
			newName.setText(namedItem.getName());
			setTitle(Messages.getStringWithParam("Text.rename.mf", Dictionary.getCategoryText(namedItem.getCategory()))); //$NON-NLS-1$
		} else {
			result =newName.getText().trim();
			if (result.equals(namedItem.getName()))
				return true; // no change
			if (result.length() == 0) {
				Alert.warn(Messages.getString("RenameDialog.TheNameCannotBeEmpty"),this); //$NON-NLS-1$
				return false;
			}
			if (Dictionary.get(namedItem.getCategory(),result) != null) {
				Alert.warn(Messages.getString("RenameDialog.AnotherItemWithThatNameAlreadyExists"),this); //$NON-NLS-1$
				return false;
			}
		}
		return true;
	}

	// Building *************************************************************

	/**
	 * Builds the panel. Initializes and configures components first, then
	 * creates a FormLayout, configures the layout, creates a builder, sets a
	 * border, and finally adds the components.
	 * 
	 * @return the built panel
	 */

	public JComponent createContentPanel() {
		// Separating the component initialization and configuration
		// from the layout code makes both parts easier to read.
		initControls();
		FormLayout layout = new FormLayout("default, 3dlu, 120dlu:grow", // cols //$NON-NLS-1$
				FlatUiSupport.preferredFormRows(5)); // rows //$NON-NLS-1$

		// Create a builder that assists in adding components to the container.
		// Wrap the panel with a standardized border.
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		builder.append(Messages.getString("RenameDialog.CurrentName"), oldName); //$NON-NLS-1$
		builder.nextLine(2);
		builder.append(Messages.getString("RenameDialog.NewName"), newName); //$NON-NLS-1$
		return builder.getPanel();
	}

}
