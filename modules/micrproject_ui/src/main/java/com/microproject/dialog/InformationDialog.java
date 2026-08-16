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
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.microproject.dialog.util.ComponentFactory;
import com.microproject.dialog.util.FieldComponentMap;
import com.microproject.help.HelpUtil;
import com.microproject.menu.MenuActionConstants;
import com.microproject.pm.graphic.IconManager;
import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.configuration.Configuration;
import com.microproject.field.Field;
import com.microproject.strings.Messages;

/**
 *
 */
public abstract class InformationDialog extends FieldDialog {
	private JButton changeWorkingTimeButton = null;
	private JButton assignResourceButton = null;
  	protected InformationDialog(final Frame owner, String title) {
		super(owner, title, false, false);
	}
  	
  	protected JButton getChangeWorkingTimeButton() {
  		if (changeWorkingTimeButton == null) {
			changeWorkingTimeButton= new JButton();
			changeWorkingTimeButton.setToolTipText(Messages.getString("InformationDialog.ChangeWorkingTime")); //$NON-NLS-1$
			ImageIcon icon = IconManager.getIcon("menu.changeWorkingTime"); //$NON-NLS-1$
			changeWorkingTimeButton.setIcon(icon);
			changeWorkingTimeButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					//route message to main frame
					GraphicManager.getInstance(changeWorkingTimeButton).getMenuManager().getActionFromId(MenuActionConstants.ACTION_CHANGE_WORKING_TIME).actionPerformed(arg0);
				}});
  		}
  		return changeWorkingTimeButton;
  	}
  	protected JButton getAssignResourceButton() {
  		if (assignResourceButton == null) {
			assignResourceButton= new JButton();
			assignResourceButton.setToolTipText(Messages.getString("InformationDialog.AssignResources")); //$NON-NLS-1$
			ImageIcon icon = IconManager.getIcon("menu24.assignResources"); //$NON-NLS-1$
			assignResourceButton.setIcon(icon);
			assignResourceButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					//route message to main frame
					GraphicManager.getInstance(assignResourceButton).getMenuManager().getActionFromId(MenuActionConstants.ACTION_ASSIGN_RESOURCES).actionPerformed(arg0);
				}});
  		}
  		return assignResourceButton;
  	}

  	protected abstract JComponent createHeaderFieldsPanel(FieldComponentMap map);
	protected  JComponent createNotesPanel(){
		FieldComponentMap map = createMap();
		FormLayout layout = new FormLayout(
		        "p:grow", // extra padding on right is for estimated field //$NON-NLS-1$
				"p, 3dlu,p, 3dlu, fill:50dlu:grow"); //$NON-NLS-1$

		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		CellConstraints cc = new CellConstraints();
		builder.setDefaultDialogBorder();
		JComponent header = createHeaderFieldsPanel(map);
		if (header != null)
			builder.add(header,cc.xyw(builder.getColumn(), builder
				.getRow(), 1));
		
		builder.nextLine(2);
		builder.append(map.getLabel("Field.notes") + ":"); //$NON-NLS-1$ //$NON-NLS-2$
		builder.nextLine(2);
		builder.append(map.getComponent("Field.notes", 0)); //$NON-NLS-1$
		return builder.getPanel();
	}

	protected  JComponent pairedComponents(FieldComponentMap map,String fieldId,int fieldFlag,JComponent tool){
		FormLayout layout = new FormLayout(
		        "p:grow,0dlu,16dlu", // extra padding on right is for estimated field //$NON-NLS-1$
				"p"); //$NON-NLS-1$

		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		Component c = map.getComponent(fieldId, fieldFlag);
		Field field = Configuration.getFieldFromId(fieldId);
		builder.append(c);
		if (field.getHelp() != null)
			HelpUtil.addDocHelp(c,field.getHelp());
		if (fieldFlag!=ComponentFactory.READ_ONLY) builder.append(tool);
		return builder.getPanel();
	}
	
	protected boolean hasCloseButton() {
		return true;
	}
	
}

