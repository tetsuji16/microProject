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
import java.text.ParsePosition;
import java.util.Iterator;

import com.microproject.configuration.Settings;


/**
 * Text formatter used for formatting and parsing lists of assignments or predecessors
 */
public class AssociationListFormat extends java.text.Format {
	java.text.Format associationFormat;
	public static AssociationListFormat getInstance(java.text.Format associationFormat) {
		return new AssociationListFormat(associationFormat);
	}
	
	private AssociationListFormat(java.text.Format associationFormat) {
		this.associationFormat = associationFormat;
	}

	/* 
	 * Caution - returns a LinkedList and not a subclass of AssociationLIst
	 */
	public Object parseObject(String string, ParsePosition arg1) { 
		AssociationList newList = new AssociationList();
		string = string.trim(); // trim the string for test if it is empty
		string = string.replace(",", Settings.LIST_SEPARATOR); // allow commas too
		string = string.replace(";", Settings.LIST_SEPARATOR); // allow semicolons too
		if (string.length() != 0) { // if list not empty
		String elements[] = string.split(Settings.LIST_SEPARATOR, -1);
			
			Association association;
			for (int i =0; i < elements.length; i++) {
				association = (Association) associationFormat.parseObject(elements[i],new ParsePosition(0));
				if (association == null)
					return null;
				newList.add(association);
			}
		}
		return newList;
	}


	public StringBuffer format(Object associationListObject, StringBuffer string, FieldPosition fieldPos) {
		AssociationList associationList = (AssociationList)associationListObject;
		Iterator i = associationList.iterator();
		Association association;
	
		while (i.hasNext()) {
			association = (Association) i.next();

			if (association.isDefault()) // ignore default elements
				continue;
			associationFormat.format(association,string,fieldPos);
			if (i.hasNext())
				string.append(Settings.LIST_SEPARATOR);
		}
		return string;
	}
	
}
