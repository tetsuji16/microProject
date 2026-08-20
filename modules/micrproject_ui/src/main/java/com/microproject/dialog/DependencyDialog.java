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
import java.awt.event.ActionEvent;
import java.text.ParseException;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.microproject.association.InvalidAssociationException;
import com.microproject.configuration.Configuration;
import com.microproject.datatype.Duration;
import com.microproject.datatype.DurationFormat;
import com.microproject.field.Field;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.task.NormalTask;
import com.microproject.strings.Messages;
import com.microproject.util.Alert;

/**
 *  
 */
public class DependencyDialog extends AbstractDialog {
	private static final long serialVersionUID = 1L;
	protected JLabel preLabel, sucLabel;
	protected JComboBox typeCombo;
	protected JTextField lagTextField;
	protected JButton removeButton = new JButton();

	Field dependencyTypeField = Configuration.getFieldFromId("Field.dependencyType");
	boolean remove=false;
	Dependency dependency;
	
	public static boolean doDialog(DependencyDialog dialog, Dependency dependency) {
		dialog.setDependency(dependency);
		boolean result;
		if (result = dialog.doModal()) {
			if (dialog.remove) {
				DependencyService.getInstance().remove(dependency,dialog,true);
			} else {
				DependencyService.getInstance().update(dependency,dialog);
			}
		}
		dialog.dependency = null;
		return result;
	}
	public DependencyDialog(Frame frame, Dependency dependency) {
		super(frame, Messages.getString("Text.TaskDependency"), true);
		initControls();
		setDependency(dependency);
		addDocHelp("Task_Dependency_Dialog");
	}

	public void setDependency(Dependency dependency) {
		remove= false;
		this.dependency = dependency;
		if (dependency.isExternal()) {
			if (dependency.isDisabled())
				setTitle(Messages.getString("Text.DisabledExternalTaskDependency"));
			else
				setTitle(Messages.getString("Text.ExternalTaskDependency"));
		} else {
			setTitle(Messages.getString("Text.TaskDependency"));
		}
		bind(true);
		
	}
	protected void initComponents() {
		if (contentPanel != null) // if already shown once
			return;
		super.initComponents();
	}
	protected void initControls() {
		preLabel = new JLabel();
		sucLabel = new JLabel();
		Object[] options = dependencyTypeField.getOptions(null);
		typeCombo = new JComboBox(options);
		lagTextField = new JTextField();
		bind(true);
	}
	
	public ButtonPanel createButtonPanel() {
		AbstractAction action = new AbstractAction(Messages.getString("Text.Remove")) {
			public void actionPerformed(ActionEvent e) {
				delete();
			}
		};
		removeButton.setAction(action);
		removeButton.setEnabled(!isReadOnly());
		createOkCancelButtons();
		ButtonPanel buttonPanel = new ButtonPanel();
		buttonPanel.addButton(removeButton);
		buttonPanel.addButton(ok);
		buttonPanel.addButton(cancel);
		buttonPanel.add(getHelpButton());
		return buttonPanel;
	}   	

	public JComponent createContentPanel() {
		// Separating the component initialization and configuration
		// from the layout code makes both parts easier to read.
		initControls();
		FormLayout layout = new FormLayout(
				"50dlu,3dlu,50dlu,3dlu,50dlu,3dlu,50dlu", // cols
				"p,3dlu,p,3dlu,p,3dlu,p,3dlu"); // rows

		// Create a builder that assists in adding components to the container.
		// Wrap the panel with a standardized border.
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		CellConstraints cc = new CellConstraints();
		builder.append(java.text.MessageFormat.format("{0}:", Messages.getString("Text.From")));
		builder.add(preLabel,cc.xyw(builder.getColumn(), builder
				.getRow(), 5)); 
		builder.nextLine(2);
		builder.append(java.text.MessageFormat.format("{0}:", Messages.getString("Text.To")));
		builder.add(sucLabel,cc.xyw(builder.getColumn(), builder
				.getRow(), 5)); 
		
		builder.nextLine(2);
		builder.append(java.text.MessageFormat.format("{0}:", Messages.getString("Text.Type")), typeCombo);

		builder.addLabel(java.text.MessageFormat.format("{0}:", Messages.getString("Text.Lag")));
		builder.nextColumn(2);
		builder.add(lagTextField);

		return builder.getPanel();
	}

//	void ok() {
//		Integer type = DependencyType.mapStringToValue((String) typeCombo
//				.getSelectedItem());
//		model.modifyEdge(edge, type.intValue(), -1);
//		edge = null;
//		setDialogResult(RESULT_AFFIRMED);
//		setVisible(false);
//	}
//

	void delete() {
		remove = true;
		setDialogResult(JOptionPane.OK_OPTION);
		setVisible(false);
	}

	protected boolean bind(boolean get) {
		if (dependency == null)
			return false;
		if (get) {
			preLabel.setText(dependency.getQualifiedPredecessorName());
			sucLabel.setText(dependency.getQualifiedSuccessorName());
			String stype = DependencyType.mapValueToString(Integer.valueOf(dependency.getDependencyType()));
			typeCombo.setSelectedItem(stype);
			lagTextField.setText(DurationFormat.format(dependency.getLag()));
			
			boolean readOnly = ((NormalTask)dependency.getPredecessor()).getProject().isReadOnly();
			typeCombo.setEnabled(!readOnly);
			lagTextField.setEnabled(!readOnly);
			removeButton.setEnabled(!readOnly);
		} else {
			try {
				Duration duration = (Duration) DurationFormat.getInstance().parseObject(lagTextField.getText());
				int type = ((Number)DependencyType.mapStringToValue(typeCombo.getSelectedItem().toString())).intValue();
				
				
//				dependency.setLag(duration.getEncodedMillis());
//				dependency.setDependencyType(type);
				DependencyService.getInstance().setFields(dependency,duration.getEncodedMillis(),type,this);
			} catch (ParseException e) {
				Alert.warn(Messages.getString("Message.invalidDuration"),this);
				return false;
			} catch (InvalidAssociationException e) {
				Alert.warn(e.getMessage(),this);
				return false;
			}
		}
		return true;
	}
	private boolean isReadOnly() {
		return ((NormalTask)dependency.getPredecessor()).isReadOnly();
	}
//	public void init(GanttModel model, GanttEdge edge) {
//		this.edge = edge;
//		this.model = model;
//		Dependency dep = edge.getDependency();
//		Task preTask = (Task) dep.getPredecessor();
//		preLabel.setText(preTask.getName());
//		Task sucTask = (Task) dep.getSuccessor();
//		sucLabel.setText(sucTask.getName());
//		int type = dep.getDependencyType();
//		String stype = DependencyType.mapValueToString(new Integer(type));
//		typeCombo.setSelectedItem(stype);
//	}

	/**
	 * @return Returns the remove.
	 */
	public boolean isRemove() {
		return remove;
	}
}
