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
package com.microproject.pm.assignment;

import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.microproject.association.AssociationFormat;
import com.microproject.association.AssociationFormatParameters;
import com.microproject.configuration.Settings;
import com.microproject.datatype.Rate;
import com.microproject.datatype.RateFormat;
import com.microproject.datatype.TimeUnit;
import com.microproject.field.FieldParseException;
import com.microproject.options.GeneralOption;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.task.Task;
import com.microproject.strings.Messages;


public class AssignmentFormat extends AssociationFormat {
	public static AssignmentFormat getInstance(AssociationFormatParameters parameters) {
		return new AssignmentFormat(parameters);
	}
	
	private AssignmentFormat(AssociationFormatParameters parameters) {
		super(parameters);
	}
	

	private String getErrorMessage(String text) {
		return Messages.getString("Message.invalidAssignments");
	}

	private static NumberFormat percentFormat = NumberFormat.getPercentInstance();
	
	private static String typePatternString =  	
		 	"\\s*" // optional whitespace before 
			+ "(" // group1 
				+ "[^" + Messages.getString("Symbol.leftBracketRegex") + "]+" // anything, aside from bracket
			+ ")" // end of group1	
			+ "(?:" // non grouping
				+ Messages.getString("Symbol.leftBracketRegex") // open bracket
				+ "(" // group 2
					+ "\\d+" // one or more digits
					+ ".*" // room here for option percent
				+ ")" // group 2					
				+ Messages.getString("Symbol.rightBracketRegex") // clost bracket
			+ ")?"
		+ "\\s*" // optional white space
		 ;
	
	
	private static Pattern pattern = Pattern.compile(typePatternString);
	
	private Object doParse(String string, ParsePosition pos) throws ParseException {
		Matcher matcher = pattern.matcher(string.substring(pos.getIndex()));
		if (!matcher.matches())
			throw new ParseException(getErrorMessage(string), pos.getIndex());
		
		// group 1 is resource name
		// group 2 is percent

		
		Object found = parameters.getIdField().find(matcher.group(1),getContainer(parameters.isLeftAssociation()));
 
		if (found == null) {
			if (GeneralOption.getInstance().isAutomaticallyAddNewResourcesAndTasks()) {
				found = createNewObject(parameters.isLeftAssociation());
				
				if (found == null) // if couldn't create, such as trying to create a task on resource pool
					throw new ParseException(getErrorMessage(string), pos.getIndex());	
				try {
					parameters.getIdField().setText(found,matcher.group(1),null);
				} catch (FieldParseException e) {
					throw new ParseException(e.getMessage(), pos.getIndex());
				}
			} else {
				throw new ParseException(getErrorMessage(string), pos.getIndex());
			}
		}
		
		double percent = 1.0D;
		Resource resource = (Resource)(parameters.isLeftAssociation() ? found : parameters.getThisObject());
		Rate rate = null;
		if (matcher.group(2) != null) { // if text was empty use default
			if (!getParameters().isAllowDetailsEntry())
				throw new ParseException(Messages.getString("Message.cannotEnterUnits"),0);
			RateFormat format = resource.getRateFormat();
			rate = (Rate) format.parseObject(matcher.group(2));
			percent = rate.getValue();
//			Number percentNumber;
//			if (resource.isLabor())
//				percentNumber = percentFormat.parse(matcher.group(2)+ Settings.PERCENT); // force a percent sign at the end for labor.  If there are two, it is ignored
//			else //TODO allow parsing values like 3/d for material resources
//				percentNumber = NumberFormat.getInstance().parse(matcher.group(2));
//			
//			if (percentNumber == null)
//				throw new ParseException(getErrorMessage(string), pos.getIndex());
//			percent = percentNumber.doubleValue();
		} else if (resource.isMaterial()) {
			rate = new Rate(1,TimeUnit.NON_TEMPORAL);
		}
		Assignment ass = Assignment.getInstance((Task) (parameters.isLeftAssociation() ? parameters.getThisObject() : found),
										resource,
									  	percent,
										0);
		if (rate != null)
			ass.detail.setRate(rate);
		return ass;
	}
	public Object parseObject(String string, ParsePosition pos) {
		try {
			return doParse(string, pos);
		} catch (ParseException e) {
			parameters.setError(e.getMessage());
			return null;
		} catch (RuntimeException e) {
			parameters.setError(e.getMessage());
			return null;
		}
	}

	/**
	 * convert to text.  The format is either, John or John[50%] 
	 */
	public StringBuffer format(Object assignmentObject,	StringBuffer string, FieldPosition fieldPos) {
		Assignment assignment = (Assignment)assignmentObject;
		Object showObject = ((parameters.isLeftAssociation()) ? (Object)assignment.getResource() : (Object)assignment.getTask());
		string.append(parameters.getIdField().getValue(showObject,null));
		if (parameters.isEncloseInBrackets()) {
			double units = assignment.getUnits();
			if (units != 1D) {
				string.append(Settings.LEFT_BRACKET);
				string.append(assignment.getRateFormat().format(assignment.getRate()));
				string.append(Settings.RIGHT_BRACKET);
			}
		}
		return string;
	}

	protected Collection<Resource> getContainer(boolean left) {
		if (left)
			return ((Task) parameters.getThisObject()).getProject().getResourcePool().getResourceList();
		throw new IllegalArgumentException("Assignment parsing for the right side is not supported");
	}

	protected Object createNewObject(boolean left) {
		if (left)
			return ((Task) parameters.getThisObject()).getProject().getResourcePool().newResourceInstance();
		throw new IllegalArgumentException("Assignment parsing for the right side is not supported");
	}
	
}
