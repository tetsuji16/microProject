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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.plaf.UIResource;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;

import org.apache.commons.beanutils.BeanUtils;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.microproject.help.HelpUtil;
import com.microproject.configuration.Settings;
import com.microproject.exchange.ResourceMappingForm;
import com.microproject.strings.Messages;
import com.microproject.util.FlatUiSupport;

public final class ResourceMappingDialog extends AbstractDialog {
	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(ResourceMappingDialog.class.getName());

	private ResourceMappingForm form;

	protected AssociationTable associationTable;
	protected JComboBox field1,editorCombo;
	protected JLabel field1Label,accessControlLabel;
	protected JCheckBox localProject;
	protected JCheckBox masterProject=null;
	protected JComboBox accessControl;

	public static ResourceMappingDialog getInstance( ResourceMappingForm form) {
		return new ResourceMappingDialog(form);
	}

	private ResourceMappingDialog(ResourceMappingForm form) {
		super(form.getOwner(), Messages.getString("ResourceMappingDialog.ResourceMerging"), true); //$NON-NLS-1$
		this.form = form;
		addDocHelp("Merge_Dialog");
	}



	// Component Creation and Initialization **********************************

	public void setForm(ResourceMappingForm form) {
		this.form = form;
	}

	/**
	 * Creates, intializes and configures the UI components. Real applications
	 * may further bind the components to underlying models.
	 */
	protected void initControls() {
		editorCombo=new JComboBox();
		associationTable=new AssociationTable();

//		field1=new JComboBox(form.getMergeFields());
//		field1.setSelectedItem(form.getMergeField());
		field1=new JComboBox();

		field1.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				form.setMergeField((ResourceMappingForm.MergeField)field1.getSelectedItem());
				((AssociationTableModel)associationTable.getModel()).update();
			}
		});
		field1Label=new JLabel(Messages.getString("ResourceMappingDialog.MergeResourcesUsingField")); //$NON-NLS-1$
		localProject=new JCheckBox(Messages.getString("ResourceMappingDialog.DontMergeOpenProjectReadOnly")); //$NON-NLS-1$
		accessControlLabel=new JLabel(Messages.getString("ResourceMappingDialog.ProjectTeam")); //$NON-NLS-1$
		accessControl=new JComboBox(new Object[]{Messages.getString("ResourceMappingDialog.AllResourcesExceptCustomerPartner"),Messages.getString("ResourceMappingDialog.BasedOnProjectRoleInResourcesView")}); //$NON-NLS-1$ //$NON-NLS-2$
		HelpUtil.addDocHelp(accessControl,"Project_Team");
		//		localProject.addItemListener(new ItemListener(){
//			public void itemStateChanged(ItemEvent e) {
//				accessControl.setEnabled(!accessControl.isEnabled());
//			}
//		});
//
		localProject.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				setLocal(localProject.isSelected());
				if (masterProject!=null&&localProject.isSelected()){
					form.setMaster(false);
					masterProject.setSelected(false);
				}

			}
		});

	}

	private void setLocal(boolean local){
		form.setLocal(local);
		field1Label.setEnabled(!local);
		field1.setEnabled(!local);
		associationTable.setEnabled(!local);
		//accessControl.setEnabled(!accessControl.isEnabled());
		accessControlLabel.setEnabled(!local);
		accessControl.setEnabled(!local);
	}


	public boolean bind(boolean get) {
		if (form == null)
			return false;
		if (get) {
			field1.setModel(new DefaultComboBoxModel(mergeFields().toArray()));
			field1.setSelectedItem(form.getMergeField());
			editorCombo.setModel(new DefaultComboBoxModel(resources().toArray()));
			editorCombo.setSelectedIndex(0);

			accessControl.setSelectedIndex(form.getAccessControlType());
			localProject.setSelected(form.isLocal());
			setLocal(form.isLocal());

			//associationTable.setModel(new AssociationTableModel());
			AssociationTableModel tableModel=(AssociationTableModel)associationTable.getModel();
			tableModel.update();
		} else {
			form.setAccessControlType(accessControl.getSelectedIndex());
			//associationTable.finishCurrentOperations();
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
		initControls();
		FormLayout layout = new FormLayout("310dlu:grow", // cols //$NON-NLS-1$
				(masterProject==null)?FlatUiSupport.preferredFormRows(7):FlatUiSupport.preferredFormRows(9)); // rows //$NON-NLS-1$ //$NON-NLS-2$

		// Create a builder that assists in adding components to the container.
		// Wrap the panel with a standardized border.
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		CellConstraints cc = new CellConstraints();
		builder.append(createFieldPanel());
		builder.nextLine(2);
		builder.add(new JScrollPane(associationTable));
		if (masterProject!=null){
			builder.nextLine(2);
			builder.append(masterProject);
		}
		builder.nextLine(2);
		builder.append(localProject);
		builder.nextLine(2);
		builder.append(createFooterPanel());
		return builder.getPanel();
	}
	public JComponent createFieldPanel(){
		FormLayout layout = new FormLayout("p,3dlu,p",// cols //$NON-NLS-1$
		"p"); // rows //$NON-NLS-1$
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		CellConstraints cc = new CellConstraints();
		builder.append(field1Label);
		builder.append(field1);
		return builder.getPanel();
	}
	public JComponent createFooterPanel(){
		FormLayout layout = new FormLayout("p,3dlu,p",// cols //$NON-NLS-1$
		"p"); // rows //$NON-NLS-1$
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		CellConstraints cc = new CellConstraints();

		builder.append(accessControlLabel); //$NON-NLS-1$
		builder.add(accessControl, cc.xy(builder.getColumn(), builder.getRow(),
			"left,default")); //$NON-NLS-1$

		return builder.getPanel();
	}

	/**
	 * @return Returns the form.
	 */
	public ResourceMappingForm getForm() {
		return form;
	}
	public ResourceMappingForm getBean(){
		return form;
	}

	private List<ResourceMappingForm.MergeField> mergeFields() {
		return form.getMergeFields();
	}

	private List<Object> resources() {
		return form.getResources();
	}

	private List<Object> importedResources() {
		return form.getImportedResources();
	}

	private List<Object> selectedResources() {
		return form.getSelectedResources();
	}

//	private void mapResources(String mpxFieldName){
//		Map fieldMap=new Hashtable();
//		for (Iterator i=form.getImportedResources().iterator();i.hasNext();){
//
//		}
//		for (Iterator i=form.getImportedResources().iterator();i.hasNext();){
//
//		}
//
//	}



	private class AssociationTable extends JTable {
	    public AssociationTable() {
	        super(new AssociationTableModel(),new AssociationTableColumnModel());
			setCellSelectionEnabled(true);

			getTableHeader().setDefaultRenderer(new HeaderRenderer());

			registerEditors();
	        createDefaultColumnsFromModel();
	    }

		protected void registerEditors(){
			//setDefaultEditor(Date.class,new DateEditor());
		}

		public void setEnabled(boolean enabled) {
			super.setEnabled(enabled);
			getTableHeader().setEnabled(enabled);

		}





//		public void finishCurrentOperations(){
//			if (isEditing()){
//				CellEditor editor=getCellEditor();
//				if (editor!=null){
//					editor.stopCellEditing();
//
//				}
//			}
//		}


	}

    private static class HeaderRenderer extends DefaultTableCellRenderer implements UIResource {
	    public HeaderRenderer(){
	    	super();
	    	setHorizontalAlignment(JLabel.CENTER);
	    }
    	public Component getTableCellRendererComponent(JTable table, Object value,
                         boolean isSelected, boolean hasFocus, int row, int column) {

	    	setEnabled(table == null || table.isEnabled());

	    	if (table != null) {
	            JTableHeader header = table.getTableHeader();
	            if (header != null) {
	                setForeground(FlatUiSupport.headerForeground());
	                setBackground(FlatUiSupport.headerBackground());
	                setFont(FlatUiSupport.headerFont());
	            }
                }

                setText((value == null) ? "" : value.toString()); //$NON-NLS-1$
		setBorder(FlatUiSupport.tableHeaderBorder()); //$NON-NLS-1$
	        return this;
            }
    }

	private class AssociationTableModel extends AbstractTableModel{

		public int getColumnCount() {
			return 2;
		}

		public int getRowCount() {
			return importedResources().size();
		}

		public Object getValueAt(int rowIndex, int columnIndex) {
			if (columnIndex==0){
				try {
					return BeanUtils.getProperty(importedResources().get(rowIndex),"name"); //$NON-NLS-1$
				} catch (Exception e) { //claur
					logger.log(Level.WARNING, "Failed to resolve imported resource name", e);
				}
			} else if (columnIndex==1){
				return selectedResources().get(rowIndex);
			}
			return null;
		}

		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return columnIndex==1;
		}

		public void setValueAt(Object value, int rowIndex, int columnIndex) {
			if (columnIndex==1){
				selectedResources().set(rowIndex,value);
			}
		}

		public void update(){
			fireTableDataChanged();
		}




	}




	private class AssociationTableColumnModel extends DefaultTableColumnModel{
		protected int columnIndex=0;
	    public AssociationTableColumnModel() {
	        super();
	    }
		public void addColumn(TableColumn tc){
			if (columnIndex==0){
				tc.setHeaderValue(Messages.getString("ResourceMappingDialog.ImportedResources")); //$NON-NLS-1$
				tc.setPreferredWidth(150);
			}else{
				tc.setHeaderValue(Messages.getString("ResourceMappingDialog.ServerResources")); //$NON-NLS-1$
				tc.setPreferredWidth(150);

//				tc.setCellEditor(new ListComboBoxCellEditor(new DefaultComboBoxModel(form.getResources())));
				tc.setCellEditor(new DefaultCellEditor(editorCombo));
			}
			tc.setCellRenderer(new DefaultTableCellRenderer(){
				public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused, int row, int column){
					setEnabled(table == null || table.isEnabled());
					super.getTableCellRendererComponent(table, value, selected, focused, row, column);
					return this;
				}
			});
			super.addColumn(tc);
			columnIndex++;
		}


		//no move
		public void moveColumn(int columnIndex, int newIndex) {
		}

	}




}
