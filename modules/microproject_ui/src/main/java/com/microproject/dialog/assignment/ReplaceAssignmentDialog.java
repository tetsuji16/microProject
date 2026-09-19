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
package com.microproject.dialog.assignment;

import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.microproject.dialog.AbstractDialog;
import com.microproject.pm.graphic.frames.DocumentFrame;
import com.microproject.pm.resource.Resource;
import com.microproject.strings.Messages;
import com.microproject.util.FlatUiSupport;

public final class ReplaceAssignmentDialog extends AbstractDialog implements CommonAssignmentDialog{
	DocumentFrame documentFrame;
    Resource resource;
	AssignmentEntryPane spreadSheetPane;
	JLabel resourceName;

	public static List<Resource> getReplacementFromDialog(DocumentFrame documentFrame, Resource resource) {
		ReplaceAssignmentDialog dialog = new ReplaceAssignmentDialog(documentFrame,resource);
		if (!dialog.doModal())
			return null;
		return dialog.getSelectedResources();
	}
	private ReplaceAssignmentDialog(DocumentFrame documentFrame, Resource resource) {
		super(documentFrame.getGraphicManager().getFrame(), Messages.getString("Text.ReplaceResource"), true);
		this.documentFrame= documentFrame;
		this.resource = resource;
		createContentPanel();
		addDocHelp("Assign_Resources#Replacing_one_resource_with_another");
	}

	/**
	 * Creates, intializes and configures the UI components. Real applications
	 * may further bind the components to underlying models.
	 */
	protected void initControls() {
        spreadSheetPane = new AssignmentEntryPane(this ,documentFrame.getProject(),null,true,null);
		spreadSheetPane.setProject(documentFrame.getProject());
		spreadSheetPane.updateTable();
        resourceName = new JLabel(Messages.format("Format.words",
				Messages.getString("Text.Replace"), resource.getName()));
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
		FormLayout layout = new FormLayout("140dlu:grow", // cols
				FlatUiSupport.preferredFormRows(4) + ",fill:200dlu:grow"); // rows

		// Create a builder that assists in adding components to the container.
		// Wrap the panel with a standardized border.
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		CellConstraints cc = new CellConstraints();
		builder.append(resourceName);
		builder.nextLine(2);

		builder.append(Messages.getString("Text.With"));
		builder.nextLine(2);
		builder.append(spreadSheetPane);
		return builder.getPanel();
	}

	
	
	

    public void setEditorButtonsVisible(boolean visible) {
    }
    
	public List<Resource> getSelectedResources(){
 		return spreadSheetPane.getSelectedResources(false);
 	}

}
