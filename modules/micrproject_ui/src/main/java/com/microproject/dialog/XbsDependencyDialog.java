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

import java.util.function.Consumer;

import java.awt.Frame;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;


import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.microproject.pm.graphic.model.cache.GraphicDependency;
import com.microproject.pm.key.HasName;
import com.microproject.strings.Messages;

/**
 *  
 */
public class XbsDependencyDialog extends AbstractDialog {
	private static final long serialVersionUID = 1L;
	protected JLabel preLabel, sucLabel;
	protected JButton removeButton;
	boolean remove=false;
	GraphicDependency dependency;
	
	public static boolean doDialog(XbsDependencyDialog dialog, GraphicDependency dependency,Consumer<Object> removeClosure) {
		dialog.setDependency(dependency);
		boolean result;
		if (result = dialog.doModal()) {
			if (dialog.remove) 
				removeClosure.accept(null);
		}
		dialog.dependency = null;
		return result;
	}
	public XbsDependencyDialog(Frame frame, GraphicDependency dependency) {
		super(frame, Messages.getString("Text.HierarchicalRelation"), true);
		initControls();
		setDependency(dependency);
	}

	public void setDependency(GraphicDependency dependency) {
		remove= false;
		this.dependency = dependency;
		setTitle(Messages.getString("Text.HierarchicalRelation"));
		bind(true);
		
	}
	protected void initControls() {
		preLabel = new JLabel();
		sucLabel = new JLabel();
		bind(true);
	}
	
	public ButtonPanel createButtonPanel() {
		AbstractAction action = new AbstractAction(Messages.getString("Text.Remove")) {
			public void actionPerformed(ActionEvent e) {
				delete();
			}
		};
		removeButton = new JButton(action);
		
		createOkCancelButtons();
		ButtonPanel buttonPanel = new ButtonPanel();
		buttonPanel.addButton(removeButton);
		//buttonPanel.addButton(ok);
		buttonPanel.addButton(cancel);
		return buttonPanel;
	}   	

	public JComponent createContentPanel() {
		// Separating the component initialization and configuration
		// from the layout code makes both parts easier to read.
		initControls();
		FormLayout layout = new FormLayout(
				"50dlu,3dlu,50dlu,3dlu,50dlu,3dlu,50dlu", // cols
				"p,3dlu,p,3dlu,p,3dlu"); // rows

		// Create a builder that assists in adding components to the container.
		// Wrap the panel with a standardized border.
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		CellConstraints cc = new CellConstraints();
		builder.append(Messages.getString("Text.From") + ":");
		builder.add(preLabel,cc.xyw(builder.getColumn(), builder
				.getRow(), 5)); 
		builder.nextLine(2);
		builder.append(Messages.getString("Text.To") + ":");
		builder.add(sucLabel,cc.xyw(builder.getColumn(), builder
				.getRow(), 5)); 
		
		return builder.getPanel();
	}


	void delete() {
		remove = true;
		setDialogResult(JOptionPane.OK_OPTION);
		setVisible(false);
	}

	protected boolean bind(boolean get) {
		if (dependency == null)
			return false;
		if (get) {
			preLabel.setText(((HasName)dependency.getPredecessor().getNode().getImpl()).getName());
			sucLabel.setText(((HasName)dependency.getSuccessor().getNode().getImpl()).getName());
		} else {
		}
		return true;
	}

	/**
	 * @return Returns the remove.
	 */
	public boolean isRemove() {
		return remove;
	}
}

