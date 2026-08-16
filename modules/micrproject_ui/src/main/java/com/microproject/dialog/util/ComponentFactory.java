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
package com.microproject.dialog.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DateFormat;
import java.util.Date;
import java.util.logging.Logger;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.JTextComponent;

import com.microproject.dialog.FieldDialog;
import com.microproject.pm.graphic.spreadsheet.editor.SpinEditor;
import com.microproject.datatype.Hyperlink;
import com.microproject.field.Field;
import com.microproject.field.FieldContext;
import com.microproject.field.FieldParseException;
import com.microproject.field.ObjectRef;
import com.microproject.field.Range;
import com.microproject.field.StaticSelect;
import com.microproject.options.CalendarOption;
import com.microproject.options.EditOption;
import com.microproject.pm.task.AccessControlPolicy;
import com.microproject.pm.task.Project;
import com.microproject.strings.Messages;
import com.microproject.util.Alert;
import com.microproject.util.ClassUtils;
import com.microproject.util.DateTime;
import com.microproject.util.DateFieldSupport;
import com.microproject.util.FlatUiSupport;
import com.microproject.util.MathUtils;

/**
 *
 */
public class ComponentFactory {
	public static final int READ_ONLY = 1;
	public static final int SOMETIMES_READ_ONLY = 2;
	
	private static double MAX_VALUE = 60000000.0; 
	private static final Logger logger = Logger.getLogger(ComponentFactory.class.getName());
	
	private static FieldContext context = null;
	

	private static JComponent getFieldComponent(JComponent component) {
		if (component instanceof JScrollPane)
			component = (JComponent) ((JScrollPane)component).getViewport().getComponent(0);
		return component;
	}
	

	static JTextField getSpinnerTextField(JSpinner spinner) {
		return ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
	}
	
	public static Object getValueFromComponent(JComponent component, Field field) {
		component = getFieldComponent(component);

		if (component instanceof JTextField) 
			return ((JTextField)component).getText();
		else if (component instanceof JTextArea)
			return ((JTextArea)component).getText();
		else if (component instanceof JCheckBox) 
			return Boolean.valueOf(((JCheckBox)component).isSelected());
		else if (component instanceof ExtDateField) { 
			return ((ExtDateField)component).getDateValue();
		}
		else if (component instanceof JComboBox) 
			return ((JComboBox)component).getSelectedItem();
		else if (component instanceof JSpinner) {
			return SpinEditor.getValue((JSpinner)component,field);
		}
		return null;
	}
	
	static void  markComponentAsUnmodified(JComponent component) {
		component.setForeground(FlatUiSupport.infoForeground());
		verifiedComponent(component).setForeground(FlatUiSupport.infoForeground());
	}

	public static void setValueOfComponent(JComponent component, Object value, boolean readOnly) {
		boolean isMultipleValues = ClassUtils.isMultipleValue(value);
		component = getFieldComponent(component);
		if (component instanceof JTextField) 
			((JTextField)component).setText((value==null)?"":(isMultipleValues ? "" : value.toString()));
		else if (component instanceof JTextArea) 
			((JTextArea)component).setText((value==null)?"":(isMultipleValues ? "" : value.toString()));
		//		((JTextArea)component).setText(isMultipleValues ? "" : value.toString());
		else if (component instanceof JCheckBox) 
			((JCheckBox)component).setSelected(value == null ? false : ((Boolean)value).booleanValue());
		else if (component instanceof ExtDateField) {
			if (DateTime.getZeroDate().equals(value))
				value = null;
			((ExtDateField)component).setValue((value==null)?null:value);
		} else if (component instanceof JComboBox)
			((JComboBox)component).setSelectedItem(value);
		else if (component instanceof JSpinner&&value!=null) {
			((JSpinner)component).setValue(value);
			if (isMultipleValues) { // set editor to empty.  Unfortunately, this disables the spinner
				getSpinnerTextField((JSpinner)component).setText("");
			} else {
				// Keep the spinner editor in sync with the converted value until the escape-key restore path is fixed.
//				getSpinnerTextField((JSpinner)component).setText(value.toString());
//				System.out.println("setting spinner text " + value);
			}
		} else if (component instanceof LinkLabel) {
			((LinkLabel)component).setHyperlink((Hyperlink) value);
			
		}else if (component instanceof JLabel) {
			((JLabel)component).setText(value == null ? "" : value.toString());
		} else if (component instanceof LookupField) {
			((LookupField)component).setText(value == null ? "" : value.toString());
			
		}
		
		component.setEnabled(!readOnly);
		markComponentAsUnmodified(component);
	}
	
//	component = valueHoldingComponent(component);


	/**
	 * 
	 */
	public ComponentFactory() {
		super();
	}

	private static JComponent componentFor(final Field field, Object value, boolean readOnly) {
		return componentFor(field, value, readOnly, null);
	}

	private static JComponent componentFor(final Field field, Object value, boolean readOnly, Object object) {
		JComponent component = null;
		Range range = field.getRange();
		JTextComponent text = null;
		if (value instanceof Boolean) {
			component = new JCheckBox(field.getName(), ((Boolean)value).booleanValue());
		} else if (readOnly) {
			if (field.isHyperlink())
				component = new LinkLabel((Hyperlink)value);
			else
				component = new JLabel();
			
		} else if (field.isDate()) {
			ExtDateField d = createDateField(field);
			d.getFormattedTextField().addActionListener(new FieldVerifier.VerifierListener());
			component = d;
			text = (JTextComponent) verifiedComponent(component);
//			text.setEnabled(false);
		} else if (field.getLookupTypes() != null) {
			component = new LookupField(field,null);
		} else if (field.hasOptions()) {
			final JComboBox combo = new JComboBox(field.getOptions(object) == null ? new Object[0] : field.getOptions(object));
//			if ("Field.accessControlPolicy".equals(field.getId())){
//				combo.setInputVerifier(new InputVerifier(){
//					@Override
//					public boolean verify(JComponent input) {
//						return Alert.okCancel(Messages.getString("Text.resetRoles"));
//					}
//				});
//			}
			
//			else 
			combo.addActionListener(new FieldVerifier.VerifierListener());
			component = combo;
			// if the combo can change dynamically, need to rebuild the combo dynamically
			if (field.isDynamicOptions()) {
				combo.getComponent(0).addMouseListener(new MouseListener() {

					public void mouseClicked(MouseEvent arg0) {
					}

					public void mouseEntered(MouseEvent arg0) {
					}

					public void mouseExited(MouseEvent arg0) {
					}

					public void mousePressed(MouseEvent arg0) {
						combo.setModel(new DefaultComboBoxModel(field.getOptions(null)));
						combo.showPopup();
					}

					public void mouseReleased(MouseEvent arg0) {
					}});
				
			}
			
		} else if (range != null) {
			component = SpinEditor.getJSpinnerInstance(field,((Number)value).doubleValue(),false);
			final JSpinner spinner = (JSpinner)component;
			text = getSpinnerTextField(spinner);
			final JTextComponent t = text;
			((JSpinner)component).addChangeListener(new ChangeListener() {

				public void stateChanged(ChangeEvent arg0) {
					FieldVerifier v = (FieldVerifier)t.getInputVerifier();
					boolean same = true;
					if (v != null) {
						Number spinnerValue = (Number) spinner.getValue();
						if (spinnerValue == null)
							return;
						double spin = MathUtils.roundToDecentPrecision(spinnerValue.doubleValue());
						if (v == null || v.getValue() == null)
							same = false;
						else 
							same = spin == ((Number)v.getValue()).doubleValue();
					}
					t.setForeground(same ? FlatUiSupport.infoForeground() : FlatUiSupport.accentColor());
				}});
		} else {
			if (field.isMemo()) {
				text = new JTextArea();
				component = new JScrollPane(text);
			} else {
				text = new JTextField();
				int width = field.getTextWidth(null,null);
				if (width != Integer.MAX_VALUE) {
					((AbstractDocument)text.getDocument()).setDocumentFilter(new FixedSizeFilter(width));
				}

				component = text;
			}
		}
		if (text != null) {
			text.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent arg0) {
			}
			public void keyReleased(KeyEvent arg0) {
			}

			public void keyTyped(KeyEvent arg0) {
				JTextComponent textComponent = (JTextComponent)arg0.getComponent();
				textComponent.setForeground(FlatUiSupport.accentColor());
				FieldDialog parentFieldDialog = getParentFieldDialog(textComponent);
				if (parentFieldDialog != null)
					parentFieldDialog.setDirtyComponent(textComponent);
			}});

		}
		if (!(component instanceof JCheckBox))
			setValueOfComponent(component,value,readOnly);
		return component;
		
	}
	
	public static FieldDialog getParentFieldDialog(Component c) {
		while (c != null && !(c instanceof FieldDialog))
			c = c.getParent();
		return (FieldDialog) c;
	}
	public static Object getFieldValue(Field field,ObjectRef objectRef) {
		if (field.hasOptions() )
			return field.getText(objectRef,context);
		Object value = field.getValue(objectRef,context);
		if (value != null && field.isDate()) {
			value = new Date(DateTime.dayFloor(((Date)value).getTime()));
		}
		return value;
		
	}

	public static void updateValueOfComponent(JComponent component, Field field, ObjectRef objectRef) {
		Object value;
		boolean readOnly = component instanceof JLabel /*|| field.isObjectReadOnly(objectRef.getObject())*/ || field.isReadOnly(objectRef,context);
		// This is ugly
		if (component instanceof LinkLabel) {
			value = field.getValue(objectRef,context);
		} else if (component instanceof LookupField) {
			value = field.getValue(objectRef,context);
		} else if (component instanceof JLabel) {
			value = field.getText(objectRef,context);
		} else if (field.hasOptions() && field.getSelect() instanceof StaticSelect) {
			value = field.getText(objectRef,context);
			if (value instanceof String && ((String)value).length() == 0)
				value = null;
		} else if (field.isDuration() || field.isRate()) {
			value = field.getText(objectRef,context);
		} else {
			value = field.getValue(objectRef,context);
		}
		if (!readOnly && field.isDate() && value != null) {
			if (!(value instanceof Date)) {
				logger.warning("bad date");
				value = field.getValue(objectRef,context);
				return;
			}
		}

		JComponent verifiedComponent = verifiedComponent(component);
		FieldVerifier verifier = (FieldVerifier) verifiedComponent.getInputVerifier();
		if (verifier != null)
			verifier.setUpdating(true);
		setValueOfComponent(component,value,readOnly);
		
		// Need to update cached value of field verifier also
		if (verifier != null) {
			verifier.setValue(value);
			verifier.setUpdating(false);
		}
		markComponentAsUnmodified(component);
	}
	
	static JComponent verifiedComponent(JComponent component) {
		if (component instanceof ExtDateField) { // The editor component is what we want
			return (JComponent) ((ExtDateField)component).getFormattedTextField();
		} else if (component instanceof JSpinner) { // spinners are strange.  See http://mindprod.com/jgloss/focus.html
			return getSpinnerTextField((JSpinner)component);
		} else if (component instanceof LookupField) {
			return ((LookupField)component).getDisplay();
		} else {
			return component; // for normal fields, the component itself is verified
		}
	}

	/**
	 * Use java 1.4s InputVerifier class to check value on focus loss
	 * @param component
	 * @param verifier
	 */
	private static void setVerifier(JComponent component, FieldVerifier verifier) {
		component = getFieldComponent(component);
		JComponent verifiedComponent = verifiedComponent(component);
		verifiedComponent.setInputVerifier(verifier);
	}
	
	/** Get the componenet to use for the field
	 * 
	 * @param field
	 * @param objectRef
	 * @param flag can be READ_ONLY to force the field as a label, or SOMETIMES_READ_ONLY, in which case the component will
	 * not be a label even if the field is read only.  value can also be 0 for default case
	 * @return
	 */
	public static JComponent componentFor(Field field, ObjectRef objectRef, int flag) {
		Object value = getFieldValue(field,objectRef);
		
		boolean readOnly = (flag & READ_ONLY) != 0;
		boolean sometimesReadOnly = (flag & SOMETIMES_READ_ONLY) != 0;
		boolean fieldReadOnly = field.isReadOnly(objectRef,context);
		readOnly |= fieldReadOnly;
		if (sometimesReadOnly)
			readOnly = false;
		JComponent component = componentFor(field,value,readOnly, objectRef.getObject());
		if (component instanceof LookupField) { //checkboxes update immediately on clicking
			((LookupField)component).addChangeListener(new FieldChangeListener(field,objectRef));
		} else if (!readOnly) {
			if (component instanceof JCheckBox) { //checkboxes update immediately on clicking
				((JCheckBox)component).addItemListener(new FieldChangeListener(field,objectRef));
			} else {
				// Keep the access-control verifier on the combo box path so the public/restricted transition stays enforced.
				if (isAccessControlPolicyField(field)){
					component.setInputVerifier(new FieldVerifier(field,objectRef, getValueFromComponent(component, field)){
						public boolean verify(JComponent component) {
							final JComboBox c=(JComboBox)component;
							Object  newValue = (Object)ComponentFactory.getValueFromComponent(component, field);
							Object publicValue=c.getItemAt(AccessControlPolicy.PUBLIC);
							if (newValue != value){
								Integer oldValue=(Integer)field.getValue(objectRef, context);
								try {
									//testing=true;
									if (oldValue==AccessControlPolicy.RESTRICTED&&publicValue.equals(newValue)){
										if(Alert.okCancel(Messages.getString("Text.resetRoles"))){
											Project project=(Project)objectRef.getObject();
											project.resetRoles(true);
											field.setValue(objectRef,source,newValue,context);
											return true;
										}else{
											c.setSelectedIndex(oldValue);
											return false;
										}
									}else field.setValue(objectRef,source,newValue,context);
								} catch (FieldParseException e) {//never happen
								}
							}
							return true;
						}
						public boolean shouldYieldFocus(JComponent component) {
							return true;
						}

					});
				}
				else setVerifier(component,new FieldVerifier(field,objectRef, getValueFromComponent(component, field)));
			}
		} else {
			if (component instanceof JLabel) {
				((JLabel)component).setText(field.getText(objectRef,context));
			}
		}
		// if field may be read only, set its status based on field read only
		if (sometimesReadOnly && fieldReadOnly)
			component.setEnabled(false);
		return component;
	}

	private static boolean isAccessControlPolicyField(Field field) {
		return "Field.accessControlPolicy".equals(field.getId());
	}
	public static ExtDateField createDateField() {
		return createDateField(null);
	}
	public static ExtDateField createDateField(Field field) {
    	DateFormat format = DateFieldSupport.dateFormatFor(field);
	    ExtDateField df = new ExtDateField(format);
//	    df.setValue(new Date(date));
	    df.getFormattedTextField().addPropertyChangeListener(new PropertyChangeListener() {

			public void propertyChange(PropertyChangeEvent evt) {
				JFormattedTextField field = (JFormattedTextField)evt.getSource();
				if (evt.getPropertyName().equals("value") && evt.getNewValue() != evt.getOldValue()) {
					if (field.getInputVerifier() != null)
						field.getInputVerifier().verify(field);
				}
			}});
	    return df;
	}
	
}


