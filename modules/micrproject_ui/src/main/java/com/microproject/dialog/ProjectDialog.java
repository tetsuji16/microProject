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
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;

import com.microproject.dialog.util.ComponentFactory;
import com.microproject.dialog.util.ExtDateField;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.microproject.dialog.util.FieldComponentMap;
import com.microproject.help.HelpUtil;
import com.microproject.configuration.Configuration;
import com.microproject.configuration.FieldDictionary;
import com.microproject.configuration.Settings;
import com.microproject.field.HasExtraFields;
import com.microproject.field.HasExtraFieldsImpl;
import com.microproject.field.Field;
import com.microproject.options.CalendarOption;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.resource.ResourcePoolFactory;
import com.microproject.strings.Messages;
import com.microproject.util.Alert;
import com.microproject.util.DateTime;
import com.microproject.util.Environment;

public final class ProjectDialog extends FieldDialog { // extends FieldDialog for extra fields handling
	private static final long serialVersionUID = 1L;

	public static class Form {
		String notes;
		String manager;
		String name;
		long startDate = CalendarOption.getInstance().makeValidStart(DateTime.gmt(new Date()), true);
		ResourcePool resourcePool = null;
		boolean resourcePoolProject;
		boolean forward = true;
		boolean local=Environment.getStandAlone();
		int projectType;
		int projectStatus;
		int expenseType;
		String group;
		String division;
		HasExtraFields extra = new HasExtraFieldsImpl();
		int accessControlType;
		
		/**
		 * @return Returns the manager.
		 */
		public String getManager() {
			return manager;
		}
		/**
		 * @param manager
		 *            The manager to set.
		 */
		public void setManager(String manager) {
			this.manager = manager;
		}
		/**
		 * @return Returns the name.
		 */
		public String getName() {
			return name;
		}
		/**
		 * @param name
		 *            The name to set.
		 */
		public void setName(String name) {
			this.name = name;
		}
		/**
		 * @return Returns the notes.
		 */
		public String getNotes() {
			return notes;
		}
		/**
		 * @param notes
		 *            The notes to set.
		 */
		public void setNotes(String notes) {
			this.notes = notes;
		}
		/**
		 * @return Returns the start.
		 */
		public long getStartDate() {
			return startDate;
		}
		/**
		 * @param start
		 *            The start to set.
		 */
		public void setStartDate(long startDate) {
			this.startDate = startDate;
		}
		/**
		 * @return Returns the resourcePool.
		 */
		public ResourcePool getResourcePool() {
			return resourcePool;
		}
		/**
		 * @param resourcePool The resourcePool to set.
		 */
		public void setResourcePool(ResourcePool resourcePool) {
			this.resourcePool = resourcePool;
		}
		public boolean isResourcePoolProject() {
			return resourcePoolProject;
		}
		public void setResourcePoolProject(boolean resourcePoolProject) {
			this.resourcePoolProject = resourcePoolProject;
		}
		/**
		 * @return Returns the forward.
		 */
		public final boolean isForward() {
			return forward;
		}
		/**
		 * @param forward The forward to set.
		 */
		public final void setForward(boolean forwardScheduled) {
			this.forward = forwardScheduled;
		}
		public boolean isLocal() {
			return local;
		}
		public void setLocal(boolean local) {
			this.local = local;
		}
		public final HasExtraFields getExtra() {
			return extra;
		}
		public int getExpenseType() {
			return expenseType;
		}
		public void setExpenseType(int expenseType) {
			this.expenseType = expenseType;
		}
		public int getProjectType() {
			return projectType;
		}
		public void setProjectType(int projectType) {
			this.projectType = projectType;
		}
		public String getDivision() {
			return division;
		}
		public void setDivision(String division) {
			this.division = division;
		}
		public String getGroup() {
			return group;
		}
		public void setGroup(String group) {
			this.group = group;
		}
		public int getProjectStatus() {
			return projectStatus;
		}
		public void setProjectStatus(int projectStatus) {
			this.projectStatus = projectStatus;
		}
		public int getAccessControlType() {
			return accessControlType;
		}
		public void setAccessControlType(int accessControlType) {
			this.accessControlType = accessControlType;
		}
		
		
	}
	private Form form;
	// use property utils to copy to project like struts

	JTextPane notes;
//	DateComboBox startDateChooser;
	ExtDateField startDateChooser;// = ComponentFactory.createDateField();
	JTextField manager;
	JTextField name;
	JComboBox resourcePool;
	JCheckBox forward,remote;
	JLabel dateLabel;
	JComboBox projectType;
	JComboBox projectStatus;
	JComboBox expenseType;
	JTextField group;
	JTextField division;
	JComboBox accessControl;

	public static ProjectDialog getInstance(Frame owner, Form project) {
		return new ProjectDialog(owner, project);
	}

	private ProjectDialog(Frame owner, Form project) {
		super(owner, Messages.getString("ProjectDialog.NewProject"), true,false); //$NON-NLS-1$
		addDocHelp("Creating_a_Project");
		if (project != null)
			this.form = project;
		else
			this.form = new Form();
		setObjectClass(HasExtraFieldsImpl.class);
		setObject(form.extra);
		
	}

	// Component Creation and Initialization **********************************

	/**
	 * Creates, intializes and configures the UI components. Real applications
	 * may further bind the components to underlying models.
	 */
	protected void initControls() {
		notes = new JTextPane(); // Notes are kept in a plain text pane until the scrollable wrapper is refactored.
		notes.setAutoscrolls(true);
//		startDateChooser = ComponentFactory.createDateComboBox();
		startDateChooser = ComponentFactory.createDateField();
		manager = new JTextField();
		name = new JTextField();
		ArrayList<Object> choices = new ArrayList<>();
		choices.add("");
		choices.addAll(ResourcePoolFactory.getInstance().getResourcePools());
		resourcePool = new JComboBox(choices.toArray());
		forward = new JCheckBox(Messages.getString("Field.forward")); //$NON-NLS-1$
		dateLabel = new JLabel();
		forward.addActionListener(new ActionListener(){
		    public void actionPerformed(ActionEvent e){
		    	setDateLabel();
		    }});
		
//		remote = new JCheckBox(Messages.getString("Text.newServerProject"));
		if (!Environment.getStandAlone()){
			accessControl=new JComboBox(new Object[]{Messages.getString("ProjectDialog.AllResourcesExceptCustomerPartner"),Messages.getString("ProjectDialog.BasedOnProjectRole")}); //$NON-NLS-1$ //$NON-NLS-2$
			HelpUtil.addDocHelp(accessControl,"Project_Team");
		}
		projectType = new JComboBox(Configuration.getFieldFromId("Field.projectType").getOptions(null)); //$NON-NLS-1$
		projectStatus = new JComboBox(Configuration.getFieldFromId("Field.projectStatus").getOptions(null)); //$NON-NLS-1$
		expenseType = new JComboBox(Configuration.getFieldFromId("Field.expenseType").getOptions(null)); //$NON-NLS-1$
		group = new JTextField();
		division = new JTextField();
		bind(true);
	}
	
	private void setDateLabel() {
		if (forward.isSelected())
			dateLabel.setText(Messages.getString("ProjectDialog.StartDate")); //$NON-NLS-1$
		else
			dateLabel.setText(Messages.getString("ProjectDialog.FinishDate")); //$NON-NLS-1$
	}

	protected boolean bind(boolean get) {
		if (form == null)
			return false;
		if (get) {
			notes.setText(form.getNotes());
			manager.setText(form.getManager());
			name.setText(form.getName());
//			startDateChooser.setDate(new Date(form.getStartDate()));
			Date d = new Date(form.getStartDate());
			Date zz = DateTime.gmtDate(d);
//			System.out.println("start " +d);
//			System.out.println("zz " +zz);
			startDateChooser.setValue(d);
			forward.setSelected(form.isForward());
			resourcePool.setSelectedItem(form.getResourcePool() == null ? "" : form.getResourcePool());
//			remote.setSelected(!form.isLocal());
			projectType.setSelectedItem(Integer.valueOf(form.getProjectType()));
			projectStatus.setSelectedItem(Integer.valueOf(form.getProjectStatus()));
			expenseType.setSelectedItem(Integer.valueOf(form.getExpenseType()));
			group.setText(form.getGroup());
			division.setText(form.getDivision());
			setDateLabel();
			
			if (!Environment.getStandAlone()) accessControl.setSelectedIndex(0);
		} else {
			form.setNotes(notes.getText());
			form.setManager(manager.getText());
			if (name.getText().length() == 0) {
				Alert.error(Messages.getString("Message.projectMustHaveName"),this); //$NON-NLS-1$
				return false;
			}
			form.setName(name.getText());
			// make valid start
//			long d = DateTime.gmt(startDateChooser.getDate()); // + startDateChooser.getDate().getTimezoneOffset() * 60000;
			long d = DateTime.gmt((Date) startDateChooser.getValue()); // + startDateChooser.getDate().getTimezoneOffset() * 60000;
//	System.out.println("chooser " + new Date(d));
			//		d = ((Date)startDateChooser.getValue()).getTime();
			if (forward.isSelected()) {
				d = CalendarOption.getInstance().makeValidStart(d, true);
			} else {
				d = CalendarOption.getInstance().makeValidEnd(d, true);
			}
			form.setStartDate(d);
			form.setResourcePool(selectedResourcePool(resourcePool.getSelectedItem()));
			form.setForward(forward.isSelected());
//			form.setLocal(!remote.isSelected());
			form.setProjectType(projectType.getSelectedIndex()); // caution ids must be sequential
			form.setProjectStatus(projectStatus.getSelectedIndex()); // caution ids must be sequential
			form.setExpenseType(expenseType.getSelectedIndex());// caution ids must be sequential
			form.setGroup(group.getText());
			form.setDivision(division.getText());
			if (!Environment.getStandAlone()) form.setAccessControlType(accessControl.getSelectedIndex());
		}
		return true;
	}

	static ResourcePool selectedResourcePool(Object selected) {
		return selected instanceof ResourcePool pool ? pool : null;
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
		FormLayout layout = new FormLayout("default, 3dlu, 220dlu, 3dlu, default:grow", // cols //$NON-NLS-1$
				"p, 3dlu,p, 3dlu,p, 3dlu, p, 3dlu, p, 3dlu,p, 3dlu, p, 3dlu, fill:50dlu:grow"); // rows //$NON-NLS-1$

		// Create a builder that assists in adding components to the container.
		// Wrap the panel with a standardized border.
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		CellConstraints cc = new CellConstraints();
		builder.append(Messages.getString("ProjectDialog.ProjectName"), name,3); //$NON-NLS-1$
		builder.nextLine(2);
		builder.append(Messages.getString("ProjectDialog.Manager"), manager,3); //$NON-NLS-1$
		builder.nextLine(2);
		builder.append(dateLabel);
		builder.append(startDateChooser);
		builder.append(forward);
		builder.nextLine(2);
		builder.append(Messages.getString("Field.resourcePool"), resourcePool, 3); //$NON-NLS-1$
		builder.nextLine(2);

		if (!Environment.getStandAlone()) {
			builder.append(Messages.getString("ProjectDialog.ProjectTeam")); //$NON-NLS-1$
			builder.add(accessControl, cc.xy(builder.getColumn(), builder.getRow(),
			"left,default")); //$NON-NLS-1$
		}
		HelpUtil.addDocHelp(accessControl,"Project_Team");
//		builder.nextLine(2);
//		builder.append("Project Status:",projectStatus);
//		builder.nextLine(2);
//		builder.append("Project Type:",projectType);
//		builder.nextLine(2);
//		builder.append("Expense Type:",expenseType);
//		builder.nextLine(2);
//		builder.append("Division:",division);
//		builder.nextLine(2);
//		builder.append("Group:",group);
		builder.nextLine(2);

		FieldComponentMap map = createMap();
		Collection<Field> extraFields = FieldDictionary.extractExtraFields(FieldDictionary.getInstance().getProjectFields(),true);
		JComponent extra = createFieldsPanel(map, extraFields);
		if (extra != null) {
			builder.add(extra,cc.xyw(builder.getColumn(), builder
					.getRow(), 3));
		}
		builder.nextLine(2);
		
		builder.append(Messages.getString("ProjectDialog.Notes")); //$NON-NLS-1$
		builder.nextLine(2);
		builder.add(new JScrollPane(notes), cc.xyw(builder.getColumn(), builder
				.getRow(), 5)); // allow spanning 3 cols
		return builder.getPanel();
	}
	/**
	 * @return Returns the form.
	 */
	public Form getForm() {
		return form;
	}
	public Object getBean(){
		return form;
	}
	protected void onCancel() {
		setVisible(false);
	}

}
