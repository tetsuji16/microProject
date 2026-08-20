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

import java.awt.Frame;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.microproject.dialog.util.FieldComponentMap;
import com.microproject.configuration.Settings;
import com.microproject.pm.task.Task;
import com.microproject.strings.Messages;
import com.microproject.util.DataUtils;

/**
 *
 */
public final class UpdateTaskDialog extends FieldDialog {
	private static final long serialVersionUID = 1L;
	JLabel taskNames;

	public static UpdateTaskDialog getInstance(Frame owner, List<?> taskNodes) {
		return new UpdateTaskDialog(owner, taskNodes);
	}

	protected boolean hasCloseButton() {
		return true;
	}

	private UpdateTaskDialog(Frame owner, List<?> taskNodes) {
		super(owner, Messages.getString("UpdateTaskDialog.UpdateTask"), true,true); //$NON-NLS-1$
		addDocHelp("Update_Tasks");

		setObjectClass(Task.class);
		setCollection(taskNodes);
		setObject(getFirstObject()); // set to first object for listener
	}
	
	
	public JComponent createContentPanel() {
		taskNames = new JLabel();
		String names = DataUtils.stringListWithMaxAndMessage(getCollection(),Settings.STRING_LIST_LIMIT,Messages.getString("Message.tooManyTasksSelectedToList.mf")); //$NON-NLS-1$
		taskNames.setText(Messages.format("Format.labelValue", Messages.getString("Text.Tasks"), names)); //$NON-NLS-1$

		
		FieldComponentMap map = createMap();
		
		FormLayout layout = new FormLayout(
		        "p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p" //$NON-NLS-1$
	    		 ,"p,3dlu,p,3dlu,p,3dlu,p,3dlu,p,3dlu,p,3dlu,p,3dlu,p,3dlu,p,3dlu,p"); //$NON-NLS-1$

		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		CellConstraints cc = new CellConstraints();

		// task names span whole dialog
		builder.add(taskNames,cc.xyw(builder.getColumn(), builder.getRow(), builder.getColumnCount()));
		builder.nextLine(2);
		builder.addSeparator(""); //$NON-NLS-1$
		builder.nextLine();
		map.append(builder,"Field.name",5); //$NON-NLS-1$
		builder.nextLine(2);
		map.append(builder,"Field.percentComplete"); //$NON-NLS-1$
		map.append(builder,"Field.duration"); //$NON-NLS-1$
		builder.nextLine(2);
		map.append(builder,"Field.start"); //$NON-NLS-1$
		map.append(builder,"Field.finish"); //$NON-NLS-1$
		builder.nextLine(2);
		builder.addSeparator(""); //$NON-NLS-1$
		builder.nextLine();
		map.append(builder,"Field.actualDuration"); //$NON-NLS-1$
		map.append(builder,"Field.remainingDuration"); //$NON-NLS-1$
		builder.nextLine(2);
		map.append(builder,"Field.actualStart"); //$NON-NLS-1$
		map.append(builder,"Field.actualFinish"); //$NON-NLS-1$
		builder.nextLine(2);
		builder.addSeparator(""); //$NON-NLS-1$
		builder.nextLine();
		map.append(builder,"Field.taskType");
		return builder.getPanel();
	}
	
	
}
