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
package com.microproject.association;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;

import com.microproject.configuration.Settings;


/**
 * Text formatter used for formatting and parsing lists of assignments or predecessors
 */
public class AssociationListFormat extends Format {
	private final Format associationFormat;
	public static AssociationListFormat getInstance(Format associationFormat) {
		return new AssociationListFormat(associationFormat);
	}
	
	private AssociationListFormat(Format associationFormat) {
		this.associationFormat = associationFormat;
	}

	@Override
	public Object parseObject(String string, ParsePosition position) {
		int inputLength = string.length();
		int start = position.getIndex();
		if (start < 0 || start > inputLength) {
			position.setErrorIndex(start);
			return null;
		}
		String originalList = string.substring(start);
		string = originalList;
		AssociationList newList = new AssociationList();
		string = string.trim(); // trim the string for test if it is empty
		string = string.replace(",", Settings.LIST_SEPARATOR); // allow commas too
		string = string.replace(";", Settings.LIST_SEPARATOR); // allow semicolons too
		if (string.length() != 0) { // if list not empty
		String[] elements = string.split(Settings.LIST_SEPARATOR, -1);
			
			int searchFrom = 0;
			for (String element : elements) {
				ParsePosition elementPosition = new ParsePosition(0);
				Association association = (Association) associationFormat.parseObject(element, elementPosition);
				if (association == null) {
					int elementStart = originalList.indexOf(element, searchFrom);
					position.setErrorIndex(start + Math.max(elementStart, 0));
					return null;
				}
				newList.add(association);
				int elementStart = originalList.indexOf(element, searchFrom);
				searchFrom = Math.max(elementStart, 0) + element.length();
			}
		}
		position.setIndex(inputLength);
		position.setErrorIndex(-1);
		return newList;
	}


	@Override
	public StringBuffer format(Object associationListObject, StringBuffer string, FieldPosition fieldPos) {
		AssociationList associationList = (AssociationList)associationListObject;
		boolean hasFormattedAssociation = false;
		for (Association association : associationList) {
			if (association.isDefault()) // ignore default elements
				continue;
			if (hasFormattedAssociation)
				string.append(Settings.LIST_SEPARATOR);
			associationFormat.format(association,string,fieldPos);
			hasFormattedAssociation = true;
		}
		return string;
	}
	
}
