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
import java.text.ParsePosition;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Locale;

import com.microproject.configuration.Configuration;
import com.microproject.field.Field;
import com.microproject.strings.Messages;



/**
 * @stereotype enumeration 
 */
public class DependencyType {
    public static final int FF = 0;
    public static final int FS = 1;
    public static final int SF = 2;
    public static final int SS = 3;
    
    public static final Integer defaultValue = Integer.valueOf(FS);
    private static Field dependencyFieldInstance = null;
    private static Field getDependencyField() {
    	if (dependencyFieldInstance == null)
    		dependencyFieldInstance = Configuration.getFieldFromId("Field.dependencyType");
    	return dependencyFieldInstance;
    }
    
	public static Integer mapStringToValue(String textValue) {
		Integer i = (Integer) getDependencyField().mapStringToValue(textValue);
		if (i == null)
			i = (Integer) getDependencyField().mapStringToValue(textValue.toUpperCase());
		return i;
	}

	public static String mapValueToString(Integer value) {
		return  getDependencyField().mapValueToString(value);
	}
	
	public static boolean isDefault(int value) {
		return value == defaultValue.intValue();
	}
	
	//any better way?
	public static String toLongString(int type){
	    switch (type) {
        case FF:
            return Messages.getString("DependencyType.longFF");
        case SF:
            return Messages.getString("DependencyType.longSF");
        case FS:
            return Messages.getString("DependencyType.longFS");
        case SS:
            return Messages.getString("DependencyType.longSS");
        default:
            throw new IllegalArgumentException("Unknown dependency type: " + type);
        }
	}
	
	/**
	 * We will capture into two groups: group 2 is the text of the dependency.  group 1 is the dependency  with optional white space
	 * We will use group1 to determine the length of the parsed text.
	 */
	private static String typePatternString =  	
         "(" // group1
		 	+ "\\s*" // optional whitespace before 
			+ "(" // group2 
				+ Messages.getString("DependencyType.SS") 
				 + "|" + Messages.getString("DependencyType.SF")
				 + "|" + Messages.getString("DependencyType.FS")
				 + "|" + Messages.getString("DependencyType.FF")
				+  "|" + Messages.getString("DependencyType.SS").toLowerCase(Locale.ROOT)
				 + "|" + Messages.getString("DependencyType.SF").toLowerCase(Locale.ROOT)
				 + "|" + Messages.getString("DependencyType.FS").toLowerCase(Locale.ROOT)
				 + "|" + Messages.getString("DependencyType.FF").toLowerCase(Locale.ROOT)
  		    + ")?"  // End group 2: SS,SF,FS,FF or nothing
			+ "\\s*" // optional white space
		 + ")" // end group 1
		 + ".*" // anything else
		 ;
	
	
	private static Pattern pattern = Pattern.compile(typePatternString);
	
	private static Format formatInstance = null;
	public static class Format extends java.text.Format {
		
		public static Format getInstance() {
			if (formatInstance == null)
				formatInstance = new Format();
			return formatInstance;
		}
		private Format() {
		}
		public Object parseObject(String string, ParsePosition pos) {
			int index = pos.getIndex();
			Matcher matcher = pattern.matcher(string.substring(index));
			if (!matcher.matches()) {
				return null;
			}
			if (matcher.group(2) == null) {// if text was empty use default
				return defaultValue;
			}
			pos.setIndex(pos.getIndex() + matcher.group(1).length());
			return DependencyType.mapStringToValue(matcher.group(2));
		}

		public StringBuffer format(Object type, StringBuffer toAppendTo, FieldPosition pos) {
			String typeName = mapValueToString((Integer)type);
			toAppendTo.append(typeName);
			return toAppendTo;
		}
		
	}	
}
