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
package com.microproject.pm.graphic.spreadsheet.renderer;
import javax.swing.JComponent;
import javax.swing.JLabel;

import com.microproject.pm.graphic.IconManager;
import com.microproject.configuration.Configuration;
import com.microproject.field.Field;
import com.microproject.graphic.configuration.HasTaskIndicators;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.SubProj;
import com.microproject.pm.task.Task;
import com.microproject.strings.Messages;
/**
 *  
 */
public class TaskIndicatorsComponent extends IndicatorsComponent{
	private static final long serialVersionUID = 192992920101L;
	protected JLabel calendar;
	protected  JLabel constraint;
	protected JLabel invalidCalendar;
	protected JLabel notes;
	protected JLabel completed;
	protected JLabel empty;
	protected JLabel missedDeadline;
	protected JLabel parentAssignment;
	protected JLabel subproject;
	protected JLabel invalidProject;
	protected JLabel delegated;
	protected JLabel delegatedMe;
	Field constraintTypeField = Configuration.getFieldFromId("Field.constraintType"); //$NON-NLS-1$
	Field constraintDateField = Configuration.getFieldFromId("Field.constraintDate"); //$NON-NLS-1$
	Field deadlineField = Configuration.getFieldFromId("Field.deadline"); //$NON-NLS-1$
	Field finish = Configuration.getFieldFromId("Field.finish"); //$NON-NLS-1$

	public boolean acceptValue(Object value){
		return acceptTask(value);
	}
	public static boolean acceptTask(Object value){
		return value instanceof HasTaskIndicators;
	}

	
	public void init() {
		//empty = new JLabel("");
		calendar = new JLabel(" ",IconManager.getIcon("indicator.calendar"),  JLabel.RIGHT); //$NON-NLS-1$ //$NON-NLS-2$
		calendar.setOpaque(false);
		constraint = new JLabel(" ", IconManager.getIcon("indicator.constraint"),  JLabel.RIGHT); //$NON-NLS-1$ //$NON-NLS-2$
		constraint.setOpaque(false);
		invalidCalendar = new JLabel(" ", IconManager.getIcon("indicator.invalidCalendar"),  JLabel.RIGHT); //$NON-NLS-1$ //$NON-NLS-2$
		invalidCalendar.setOpaque(false);
		notes = new JLabel(" ",IconManager.getIcon("indicator.note"),  JLabel.RIGHT); //$NON-NLS-1$ //$NON-NLS-2$
		notes.setOpaque(false);
		parentAssignment = new JLabel(" ",IconManager.getIcon("indicator.parentAssignment"),  JLabel.RIGHT); //$NON-NLS-1$ //$NON-NLS-2$
		parentAssignment.setOpaque(false);
		completed = new JLabel(" ",IconManager.getIcon("indicator.completed"),  JLabel.RIGHT); //$NON-NLS-1$ //$NON-NLS-2$
		completed.setOpaque(false);
		missedDeadline = new JLabel(" ",IconManager.getIcon("indicator.missedDeadline"),  JLabel.RIGHT); //$NON-NLS-1$ //$NON-NLS-2$
		missedDeadline.setOpaque(false);
		subproject = new JLabel(" ",IconManager.getIcon("indicator.subproject"),  JLabel.RIGHT); //$NON-NLS-1$ //$NON-NLS-2$
		subproject.setOpaque(false);
		invalidProject = new JLabel(" ",IconManager.getIcon("indicator.invalidProject"),  JLabel.RIGHT); //$NON-NLS-1$ //$NON-NLS-2$
		invalidProject.setOpaque(false);
		delegated = new JLabel(" ",IconManager.getIcon("indicator.delegated"),  JLabel.RIGHT); //$NON-NLS-1$ //$NON-NLS-2$
		delegated.setOpaque(false);
		delegatedMe = new JLabel(" ",IconManager.getIcon("indicator.delegatedMe"),  JLabel.RIGHT); //$NON-NLS-1$ //$NON-NLS-2$
		delegatedMe.setOpaque(false);
	}
	
	
	
	public void setIndicators(Object value,JComponent label,StringBuilder text,boolean isSelected, boolean hasFocus){
		HasTaskIndicators indicators = (HasTaskIndicators)value;

		if (indicators.getWorkCalendar() != null) {
			setLook(calendar,isSelected,hasFocus);
			label.add(calendar);
			text.append(Messages.getString("TaskIndicatorsComponent.TheCalendar") + indicators.getWorkCalendar().getName() + Messages.getString("TaskIndicatorsComponent.isAssignedToTheTask")); //$NON-NLS-1$ //$NON-NLS-2$
		}
		long constraintDate = indicators.getConstraintDate();
		if (constraintDate != 0) {
			setLook(constraint,isSelected,hasFocus);
			label.add(constraint);
			text.append(Messages.getString("TaskIndicatorsComponent.ThisTaskHasA") + constraintTypeField.getText(indicators,null) + Messages.getString("TaskIndicatorsComponent.constraintOn") + constraintDateField.getText(indicators,null)+"<br>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		if (indicators.isInvalidIntersectionCalendar()) {
			setLook(invalidCalendar,isSelected,hasFocus);
			label.add(invalidCalendar);
			text.append(Messages.getString("TaskIndicatorsComponent.TheIntersection")); //$NON-NLS-1$
		}
		String note = indicators.getNotes();
		if (note != null && note.length() > 0) {
			setLook(notes,isSelected,hasFocus);
			label.add(notes);
			text.append(Messages.getString("TaskIndicatorsComponent.Notes") + note + "'<br>"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (indicators.isComplete()) {
			setLook(completed,isSelected,hasFocus);
			label.add(completed);
			text.append(Messages.getString("TaskIndicatorsComponent.TheTaskWasCompletedOn") + finish.getText(indicators,null)+"<br>"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		if (indicators.isParentWithAssignments()) {
			setLook(parentAssignment,isSelected,hasFocus);
			label.add(parentAssignment);
			text.append(Messages.getString("TaskIndicatorsComponent.ThisParentTaskHasResources") + ((NormalTask)indicators).getResourceNames()+"<br>"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		if (indicators instanceof Task) {
			Task t = (Task)indicators;
			if (t.isSubproject()) {
				SubProj s = (SubProj)t;
				if (s.isValid()) {
					setLook(subproject,isSelected,hasFocus);
					label.add(subproject);
					text.append(Messages.getString("TaskIndicatorsComponent.ThisTasksRepresentsThe") + (s.isSubprojectOpen() ? Messages.getString("TaskIndicatorsComponent.opened") : Messages.getString("TaskIndicatorsComponent.unopened")) + Messages.getString("TaskIndicatorsComponent.subproject") + ((Task)s).getName()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				} else {
					setLook(invalidProject,isSelected,hasFocus);
					label.add(invalidProject);
					text.append(Messages.getString("TaskIndicatorsComponent.ThisSubprojectIsNotValid")); //$NON-NLS-1$
				}
			}
			
			
			if (t.isMissedDeadline()) {
				setLook(missedDeadline,isSelected,hasFocus);
				label.add(missedDeadline);
				text.append(Messages.getString("TaskIndicatorsComponent.ThisTaskFinishesOn") + finish.getText(indicators,null) +  //$NON-NLS-1$
						Messages.getString("TaskIndicatorsComponent.whichIsAfterItsDeadline") + deadlineField.getText(indicators,null)); //$NON-NLS-1$
			}
			if (t.getDelegatedTo() != null) {
				if (t.isDelegatedToUser()) {
					setLook(delegatedMe,isSelected,hasFocus);
					label.add(delegatedMe);
					text.append(Messages.getString("TaskIndicatorsComponent.ThisTaskHasBeenDelegatedToYou")); //$NON-NLS-1$

				} else {
					setLook(delegated,isSelected,hasFocus);
					label.add(delegated);
					text.append(Messages.getString("TaskIndicatorsComponent.ThisTaskHasBeenDelegatedTo") + t.getDelegatedTo().getName()); //$NON-NLS-1$
				}
			}
		}
	}
	
	
}

