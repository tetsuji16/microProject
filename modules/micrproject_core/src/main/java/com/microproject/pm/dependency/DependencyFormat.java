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
package com.microproject.pm.dependency;

import java.text.FieldPosition;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Collection;

import com.microproject.association.AssociationFormat;
import com.microproject.association.AssociationFormatParameters;
import com.microproject.configuration.Settings;
import com.microproject.datatype.Duration;
import com.microproject.datatype.DurationFormat;
import com.microproject.field.FieldParseException;
import com.microproject.options.GeneralOption;
import com.microproject.pm.task.Task;
import com.microproject.strings.Messages;


public class DependencyFormat extends AssociationFormat {
	public static DependencyFormat getInstance(AssociationFormatParameters parameters) {
		return new DependencyFormat(parameters);
	}
	
	private DependencyFormat(AssociationFormatParameters parameters) {
		super(parameters);
	}
	
	
	private String getErrorMessage(String text) {
		String errorMessage = MessageFormat.format(Messages.getString("Message.invalidDependency.mf"),
			new Object[] {parameters.isLeftAssociation() ?
					Messages.getString("Text.predecessor") : 
					Messages.getString("Text.successor")});
		return errorMessage;
	}
	private static NumberFormat integerFormat = NumberFormat
	.getIntegerInstance();

	private Object doParse(String string, ParsePosition pos) throws ParseException {
		Long number = (Long) integerFormat.parseObject(string, pos);
		if (number == null)
			throw new ParseException(getErrorMessage(string), pos.getIndex());
		
		Object found = null;
		Collection container = getContainer(parameters.isLeftAssociation());
		if (container != null)
			found = parameters.getIdField().find(number,container);

		if (found == null) { //TODO this should probably be moved to finder

			if (GeneralOption.getInstance().isAutomaticallyAddNewResourcesAndTasks()) {
				found = createNewObject(parameters.isLeftAssociation());
				try {
					parameters.getIdField().setText(found,number.toString(),null);
				} catch (FieldParseException e) {
					throw new ParseException(e.getMessage(), pos.getIndex());
				}
			} else {
				throw new ParseException(getErrorMessage(string), pos.getIndex());
			}
		}
		Integer type = (Integer) DependencyType.Format.getInstance().parseObject(string, pos);
		
		if (type == null)
			throw new ParseException(getErrorMessage(string), pos.getIndex());

		Duration duration;
		String durationPart = string.substring(pos.getIndex()).trim();

		if (durationPart.length() == 0) { // if a duration was entered, use it, otherwise 0
			duration = Duration.ZERO;
		} else {
			duration = (Duration) DurationFormat.getInstance().parseObject(string, pos);
			if (duration == null)
				throw new ParseException(getErrorMessage(string), pos.getIndex());
		}
		return Dependency.getInstance(	parameters.isLeftAssociation() ? (HasDependencies)found : (HasDependencies)parameters.getThisObject(),
										parameters.isLeftAssociation() ? (HasDependencies)parameters.getThisObject() : (HasDependencies)found,
									  	type.intValue(),
										duration.getEncodedMillis());
		
	}
	public Object parseObject(String string, ParsePosition pos) {
		try {
			return doParse(string, pos);
		} catch (ParseException e) {
			parameters.setError(e.getMessage());
			return null;
		}
	}

	/**
	 * convert to text.  The format is either, 123, 123FF, or 123FS-1d 
	 */
	public StringBuffer format(Object dependencyObject,	StringBuffer string, FieldPosition fieldPos) {
		Dependency dependency = (Dependency)dependencyObject;
		Task task = (Task) ((parameters.isLeftAssociation()) ? dependency.getPredecessor() : dependency.getSuccessor());
		string.append(parameters.getIdField().getValue(task,null));
		boolean hasLag = !Duration.isZero(dependency.getLag());

		StringBuilder details = new StringBuilder();
		if (!DependencyType.isDefault(dependency.getDependencyType()) || hasLag)
			details.append(DependencyType.mapValueToString( Integer.valueOf(dependency.getDependencyType())));

		Duration duration = new Duration(dependency.getLag()); // use duration format to format duration
		if (hasLag) {
			details.append(DurationFormat.getSignedInstance().format(duration));
		}
		if (details.length() != 0) {
			if (parameters.isEncloseInBrackets())
				string.append(Settings.LEFT_BRACKET);
			string.append(details);
			if (parameters.isEncloseInBrackets())
				string.append(Settings.RIGHT_BRACKET);
		}
		return string;
	}

	protected Collection<Task> getContainer(boolean left) {
		return ((Task) parameters.getThisObject()).getProject().getTaskList();
	}
	protected Object createNewObject(boolean left) {
		return ((Task) parameters.getThisObject()).getProject().newNormalTaskInstance(); //TODO this should not search only in current
	}


}
