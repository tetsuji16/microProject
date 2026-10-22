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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import com.microproject.undo.EditSupport;

import com.microproject.dialog.FieldDialog;
import com.microproject.field.Field;
import com.microproject.field.FieldContext;
import com.microproject.field.FieldParseException;
import com.microproject.field.ObjectRef;
import com.microproject.field.Select;
import com.microproject.field.CanBeDirty;
import com.microproject.server.data.DataObject;
import com.microproject.options.EditOption;
import com.microproject.undo.FieldEdit;
import com.microproject.undo.TaskConstraintEdit;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Task;
import com.microproject.util.Alert;
import com.microproject.util.DateFieldSupport;
import com.microproject.util.FlatUiSupport;

/**
 *
 */
@SuppressWarnings("deprecation")
public class FieldVerifier extends InputVerifier {
	protected FieldContext context = null;
	protected Field field;
	protected ObjectRef objectRef = null;
	protected Object source;
	protected Object value;
	protected Exception exception = null;
	protected boolean updating = false;
	boolean testing = false;
//	private EditSupport undoableEditSupport;
	/**
	 * @param value the value that should be compared against the current field state
	 * 
	 */
	public FieldVerifier(Field field, ObjectRef objectRef, Object value/*,EditSupport undoableEditSupport*/) {
		super();
		this.field = field;
		this.objectRef = objectRef;
		setValue(value);
		this.source = this;
//		this.undoableEditSupport=undoableEditSupport;
	}
	
	/**
	 * Get the top level component.  For dates and spinners, the verification is triggered on a grandchild.  We need the grandparent
	 * @param component
	 * @return
	 */
	static JComponent valueHoldingComponent(JComponent component) {
		Object p = component.getParent();
		// for spinners and dates, need to go up to grandparent to get the control which holds the value
		if (p instanceof LookupField lookupField)
			p = lookupField.getDisplay();
		else if (p instanceof Component parent)
			p = parent.getParent();
		if (p instanceof JComponent valueComponent && (p instanceof JSpinner || p instanceof ExtDateField))
			component = valueComponent;
		return component;
	}


	
	public boolean verify(JComponent component) {
		if (updating) {
			return true;
		}

		FieldDialog parentFieldDialog = ComponentFactory.getParentFieldDialog(component);
		if (parentFieldDialog != null)
			parentFieldDialog.setDirtyComponent(null);
		
		
		JComponent c = component;
		component = valueHoldingComponent(component);
		
		component.setForeground(FlatUiSupport.infoForeground());
		c.setForeground(FlatUiSupport.infoForeground());
		
		Object newValue = ComponentFactory.getValueFromComponent(component, field);
		if (field.isDate() && c instanceof JTextField textField && textField.getText().isBlank()
				&& field.getValue(objectRef, context) == null)
			return true;
		
		// avoid validating unchanged controls
		if (newValue == value || (newValue != null && newValue.equals(value))) { //unchanged
			if (component instanceof JSpinner || component instanceof ExtDateField) { // if a spinner, check for modified text
				String text = ((JTextField)c).getText();
				try {
					if (component instanceof ExtDateField) {
						if (text.trim().length() > 0) {
							newValue = DateFieldSupport.parseYearless(text, ((ExtDateField) component).getDateFormat(), null);
						} else {
							((JTextField)c).setText(""); // put in empty text
							newValue = null;
						}
					} else {
						newValue = field.getFormat().parseObject(text);
					}
				} catch (ParseException e1) {
					exception = new FieldParseException(field.syntaxErrorForField());
					component.setForeground(FlatUiSupport.errorForeground());
					c.setForeground(FlatUiSupport.errorForeground());
					if (parentFieldDialog != null)
						parentFieldDialog.setDirtyComponent(c);

					return false;
				}
			} else {
				return true;
			}
		}
		if (newValue != null && value != null && newValue.toString().equals(value.toString()))
			return true;
		
		exception = null;
		List<FieldState> priorStates = captureFieldStates();
		try {
			if (field.hasOptions())  {
				if (newValue == null)
					newValue = Select.EMPTY;
				Object oldValue = field.getValue(objectRef, context);
				Task constraintTask = ("Field.constraintType".equals(field.getId())
					|| "Field.constraintDate".equals(field.getId()))
					&& objectRef.getObject() instanceof Task task ? task : null;
				int oldConstraintType = constraintTask == null ? 0 : constraintTask.getConstraintType();
				long oldConstraintDate = constraintTask == null ? 0L : constraintTask.getConstraintDate();
				field.setText(objectRef,newValue.toString(),context);
				Object updatedValue = field.getValue(objectRef, context);
				EditSupport undoableEditSupport = objectRef.getDataFactory().getUndoController().getEditSupport();
				if (undoableEditSupport != null && !java.util.Objects.equals(oldValue, updatedValue)) {
					if (constraintTask != null) {
						undoableEditSupport.postEdit(new TaskConstraintEdit(constraintTask, oldConstraintType, oldConstraintDate,
							constraintTask.getConstraintType(), constraintTask.getConstraintDate(), this));
					} else {
						undoableEditSupport.postEdit(new FieldEdit(field, objectRef, updatedValue, oldValue, this, context));
					}
				}
			} else {
				if (field.isDate()) {
					if (newValue instanceof String dateText) {
						try {
							newValue = EditOption.getInstance().getDateFormat().parseObject(dateText);
						} catch (ParseException e) {
						}
					}
					if (newValue == null || newValue.toString().trim().equals("")) // empty text on date is a null date
						newValue = com.microproject.util.DateTime.getZeroDate();
				}
				if ("Field.constraintDate".equals(field.getId()) && newValue instanceof java.util.Date date
						&& objectRef.getObject() instanceof Task task
						&& task.getEffectiveWorkCalendar().adjustInsideCalendar(date.getTime(), false) == task.getConstraintDate()) {
					setValue(newValue);
					return true;
				}
				if (newValue != value){
					Object oldValue=field.getValue(objectRef, context);
					Task constraintDateTask = "Field.constraintDate".equals(field.getId())
						&& objectRef.getObject() instanceof Task task ? task : null;
					int oldConstraintType = constraintDateTask == null ? 0 : constraintDateTask.getConstraintType();
					long oldConstraintDate = constraintDateTask == null ? 0L : constraintDateTask.getConstraintDate();
					if (field.isMoney())
						field.setText(objectRef,""+newValue,context);
					else		
						field.setValue(objectRef,source,newValue,context);
					EditSupport undoableEditSupport=objectRef.getDataFactory().getUndoController().getEditSupport();
					if (undoableEditSupport!=null){
						if (constraintDateTask != null) {
							undoableEditSupport.postEdit(new TaskConstraintEdit(constraintDateTask, oldConstraintType,
								oldConstraintDate, constraintDateTask.getConstraintType(),
								constraintDateTask.getConstraintDate(), this));
						} else {
							undoableEditSupport.postEdit(new FieldEdit(field,objectRef,newValue,oldValue,this,context));
						}
					}
				}
				
			}
		} catch (FieldParseException e) {
			rollback(priorStates, e);
			exception = e;
			component.setForeground(FlatUiSupport.errorForeground());
			c.setForeground(FlatUiSupport.errorForeground());
			if (parentFieldDialog != null)
				parentFieldDialog.setDirtyComponent(c);
			return false;
		} catch (RuntimeException | Error failure) {
			rollback(priorStates, failure);
			throw failure;
		}
		setValue(newValue); // set to new value for next time
		return true;
	}

	private List<FieldState> captureFieldStates() {
		List<ObjectRef> targets = new ArrayList<>();
		if (objectRef.getNode() != null || objectRef.getObject() != null)
			targets.add(objectRef);
		if (("Field.constraintDate".equals(field.getId()) || "Field.constraintType".equals(field.getId()))
				&& objectRef.getObject() instanceof NormalTask task && task.getProject() != null) {
			for (Object projectTask : task.getProject().getTaskList()) {
				if (projectTask != task)
					targets.add(new SingleObjectRef(projectTask, objectRef));
			}
		}
		Collection<?> collection = objectRef.getCollection();
		if (collection != null) {
			for (Object object : collection)
				targets.add(new SingleObjectRef(object, objectRef));
		}
		List<FieldState> states = new ArrayList<>(targets.size());
		for (ObjectRef target : targets) {
			Object entity = target.getNode() == null ? target.getObject() : target.getNode().getImpl();
			Boolean dirty = entity instanceof DataObject dataObject ? dataObject.isDirty()
					: entity instanceof CanBeDirty canBeDirty ? canBeDirty.isDirty() : null;
			TaskScheduleState schedule = entity instanceof NormalTask normalTask
					? new TaskScheduleState(normalTask.getConstraintType(), normalTask.getConstraintDate(),
							normalTask.getStart(), normalTask.getEnd(), normalTask.getActualStart()) : null;
			states.add(new FieldState(target, field.getValue(target, context), entity, dirty, schedule));
		}
		return states;
	}

	private void rollback(List<FieldState> states, Throwable originalFailure) {
		FieldContext rollbackContext = new FieldContext();
		rollbackContext.setNoUpdate(true);
		rollbackContext.setNoDirty(true);
		for (FieldState state : states) {
			try {
				Object currentValue = field.getValue(state.objectRef(), rollbackContext);
				if (!java.util.Objects.equals(currentValue, state.value()))
					field.setValue(state.objectRef(), source, state.value(), rollbackContext);
			} catch (FieldParseException | RuntimeException | Error rollbackFailure) {
				originalFailure.addSuppressed(rollbackFailure);
			} finally {
				if (state.entity() instanceof NormalTask normalTask && state.schedule() != null) {
					TaskScheduleState schedule = state.schedule();
					normalTask.restoreMpoScheduleSnapshot(schedule.constraintType(), schedule.constraintDate(),
							schedule.start(), schedule.finish(), schedule.actualStart());
				}
				if (state.dirty() != null) {
					if (state.entity() instanceof DataObject dataObject)
						dataObject.setDirty(state.dirty());
					else if (state.entity() instanceof CanBeDirty canBeDirty)
						canBeDirty.setDirty(state.dirty());
				}
			}
		}
	}

	private record FieldState(ObjectRef objectRef, Object value, Object entity, Boolean dirty,
			TaskScheduleState schedule) {}

	private record TaskScheduleState(int constraintType, long constraintDate, long start, long finish, long actualStart) {}

	private static final class SingleObjectRef implements ObjectRef {
		private final Object object;
		private final ObjectRef parent;
		private SingleObjectRef(Object object, ObjectRef parent) { this.object = object; this.parent = parent; }
		@Override public com.microproject.grouping.core.Node getNode() { return null; }
		@Override public com.microproject.grouping.core.model.WalkersNodeModel getNodeModel() { return null; }
		@Override public Object getObject() { return object; }
		@Override public Collection<?> getCollection() { return null; }
		@Override public com.microproject.grouping.core.model.NodeModelDataFactory getDataFactory() { return parent.getDataFactory(); }
	}

	
	public boolean shouldYieldFocus(JComponent arg0) {
		if (testing) // sempaphore to protect infinite focus loop when popping up error dialog.  Does not need to be synchronized since the verifier is not shared
			return true;
		testing = true;
		boolean result = super.shouldYieldFocus(arg0);
		if (result == false)
			Alert.error(exception.getMessage(),arg0);
		testing = false;
		return result;
	}
	/**
	 * @param value The value to set.
	 */
	void setValue(Object value) {
		this.value = value;
	}

	/** A generic listener class that will validate on an event */
	public static class VerifierListener implements ActionListener {
	    public void actionPerformed(ActionEvent e){
	    	JComponent c = (JComponent)e.getSource();
	    	InputVerifier v = c.getInputVerifier();
	    	if (v != null) // on init, it is null
	    		v.verify(c);
	    }
	}
	public final void setUpdating(boolean doNotVerify) {
		this.updating = doNotVerify;
	}
	final Object getValue() {
		return value;
	}

	/**
	 * @return
	 */
	final boolean isUpdating() {
		return updating;
	}
	public final Exception getException() {
		return exception;
	}
}

