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
package com.microproject.grouping.core;
import java.util.ArrayList;
/**
 *
 */

import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;

/**
 * Class used in converting outline codes to hierarchy
 */
public class OutlineCode extends Format {
	static final int NUMBERS = 0;
	static final int UPPERCASE_LETTERS = 1;
	static final int LOWERCASE_LETTERS = 2;
	static final int CHARACTERS = 3;
	private static final int ANY_LENGTH = 0;
	
	private final ArrayList<Mask> masks = new ArrayList<>();
	private transient Pattern pattern = null;

	public Object parseObject(String code, ParsePosition pos) {
		if (code == null || pattern == null) {
			pos.setErrorIndex(pos.getIndex());
			return null;
		}
		String current = code.substring(pos.getIndex());
		Matcher matcher = pattern.matcher(current);
		if (matcher.matches()) {
			pos.setIndex(pos.getIndex() + matcher.end());
			return current;
		}
		else {
			pos.setErrorIndex(pos.getIndex());
			return null;
		}
	}

	public StringBuffer format(Object arg0, StringBuffer arg1, FieldPosition arg2) {
		if (!(arg0 instanceof String code) || !isValid(code))
			throw new IllegalArgumentException("Invalid outline code: " + arg0);
		return arg1.append(code);
	}
	
	public boolean isValid(String code) {
		try {
			parseObject(code);
		} catch (ParseException e) {
			return false;
		}
		return true;
	}
	
	public void addMask(Mask mask) {
		masks.add(mask);
		rebuildPattern();
	}
	
	
	private void rebuildPattern() {
		Iterator<Mask> i = masks.iterator();
		pattern = Pattern.compile(getPattern(i,""));
	}
	
	/**
	 * Recursively build the pattern.  Any given sublevel is made optional for its parent
	 * @param i
	 * @return
	 */
	private String getPattern(Iterator<Mask> i, String previousSeparator) {
		StringBuilder pattern = new StringBuilder();
		Mask mask = i.next();
		pattern.append(mask.getPattern(previousSeparator));
		if (i.hasNext()) {
			pattern.append("(?:" + getPattern(i,mask.getSeparatorRegex()) + ")?"); // add next level as optional
		}
		return pattern.toString();
		
	}
	
	
	public static void main(String[] args) {
		OutlineCode code = new OutlineCode();
		code.addMask(new Mask(NUMBERS,ANY_LENGTH,"."));
		code.addMask(new Mask(UPPERCASE_LETTERS,2,"."));
		code.addMask(new Mask(LOWERCASE_LETTERS,ANY_LENGTH,"."));
		
		boolean res;
		res = code.isValid("12.AA.a");
		res = code.isValid("22212.AA.absdf");
		res = code.isValid("1");
		res = code.isValid("12.AA");
		res = code.isValid("12.");
		res = code.isValid(".AA");
		res = code.isValid("A2");
		res = code.isValid("132");
		res = code.isValid("12.11");
		res = code.isValid("12.AA.");
	}
	

	public static class Mask  {
		static final int NUMBERS = 0;
		static final int UPPERCASE_LETTERS = 1;
		static final int LOWERCASE_LETTERS = 2;
		static final int CHARACTERS = 3;
		private static final int ANY_LENGTH = 0;
	
		int type = NUMBERS;
		int length; //The maximum length in characters of the outline code values, from 1-255. If length is any, the value is zero.
		String separator; // must be non-alphanumeric
		
		/**
		 * Gets a regular expression for this mask
		 * @param previousSeparator - if not empty, this expression is prefixed with previous mask's separator
		 * @return
		 */
		StringBuilder getPattern(String previousSeparator) {
			StringBuilder result = new StringBuilder("(");
			result.append(previousSeparator);
			switch (type) {
				case NUMBERS:
					result.append("\\d");
					break;
				case UPPERCASE_LETTERS:
					result.append("[A-Z]");
					break;
				case LOWERCASE_LETTERS:
					result.append("[a-z]");
					break;
				case CHARACTERS:
					result.append("[^" + separator + "]");
					break;
			}
			if (length == ANY_LENGTH)
				result.append("+"); // at least one
			else
				result.append("{" + length + "}"); // exactly <length> times

			result.append(")");
			return result;
		}
		
		String nextValue(String current) {
			switch (type) {
				case NUMBERS:
					int value = Integer.parseInt(current) + 1;
					if (length == ANY_LENGTH)
						return Integer.toString(value);
					return new DecimalFormat(StringUtils.repeat("0",length), DecimalFormatSymbols.getInstance(Locale.ROOT)).format(value);
				default:
					return current;
			}
		}
		
		public Mask() {}
		public Mask(int type, int length, String separator) {
			this.type = type;
			this.length = length;
			this.separator = separator;
		}
		
		/**
		 * @return Returns the length.
		 */
		public int getLength() {
			return length;
		}
		/**
		 * @param length The length to set.
		 */
		public void setLength(int length) {
			this.length = length;
		}
		/**
		 * @return Returns the separator.
		 */
		public String getSeparator() {
			return separator;
		}
		
		public String getSeparatorRegex() {
			return "\\" + separator; // it is allowed to escape any nonalpha character
		}
		
		/**
		 * @param separator The separator to set.
		 */
		public void setSeparator(String separator) {
			this.separator = separator;
		}
		/**
		 * @return Returns the type.
		 */
		public int getType() {
			return type;
		}
		/**
		 * @param type The type to set.
		 */
		public void setType(int type) {
			this.type = type;
		}
	}

}
