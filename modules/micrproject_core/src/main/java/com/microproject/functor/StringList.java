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
package com.microproject.functor;

import com.microproject.util.DataUtils;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.function.Consumer;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.functors.StringValueTransformer;

import com.microproject.configuration.Settings;
/**
 *
 */
public class StringList implements Consumer<Object> {
	private StringBuilder buffer= new StringBuilder();
	private String separator = Settings.LIST_SEPARATOR;
	private Transformer transformer = null;
	
	private StringList() {
		this(StringValueTransformer.INSTANCE);
	}
	
	private StringList(Transformer transformer) {
		this.transformer = transformer;
	}
	
	/**
	 * @param separator The separator to set.
	 */
	public void setSeparator(String separator) {
		this.separator = separator;
	}
	
	public void accept(Object object) {
		if (object != null) {
			if (buffer.length() > 0)
				buffer.append(separator);
			buffer.append(transformer.transform(object));
		}
	}
	public String toString() {
		return buffer.toString();
	}

	/**
	 * Utility function to dump out a collection separated by ,
	 * @param collection
	 * @return
	 */
	public static String list(Collection collection) {
		return list(collection,StringValueTransformer.INSTANCE);
	}
	public static String commaSeparatedList(Collection collection) {
		StringList l = getInstance(StringValueTransformer.INSTANCE);
		l.setSeparator(",");
		DataUtils.forAllDo(collection.iterator(), l);
		return l.toString();
	}
	public static String brSeparatedList(Collection collection) {
		StringList l = getInstance(StringValueTransformer.INSTANCE);
		l.setSeparator("<br>");
		DataUtils.forAllDo(collection.iterator(), l);
		return l.toString();
	}

	/** for generating prepared statements */
	public static String commaQuestionMarkString(Collection collection) {
		return StringList.list(collection,",", new Transformer(){
		public Object transform(Object arg0) {
			return "?";
		}});
	}

	public static String list(Collection collection, Transformer transformer) {
		StringList l = getInstance(transformer);
		DataUtils.forAllDo(collection.iterator(), l);
		return l.toString();
	}	
	public static String list(Collection collection, String separator, Transformer transformer) {
		StringList l = getInstance(transformer);
		l.setSeparator(separator);
		DataUtils.forAllDo(collection.iterator(), l);
		return l.toString();
	}	
	
	public static String listWithMaxAndMessage(Collection collection, int maxInList, String message, Transformer transformer) {
		if (collection.size() > maxInList)
			return MessageFormat.format( message, new Object[] { Integer.valueOf(collection.size())});
		return list(collection,transformer);
	}
	/**
	 * Utility function to dump out a collection on separate lines
	 * @param collection
	 * @return
	 */
	public static String rows(java.util.Collection collection) {
		StringList l = getInstance();
		l.setSeparator("\n");
		DataUtils.forAllDo(collection.iterator(), l);
		return l.toString();
	}

	public static StringList getInstance() {
		return new StringList();
	}

	public static StringList getInstance(Transformer transformer) {
		return new StringList(transformer);
	}	

/**
 * Repates the String val a number of times. Couldn't find an SDK or commons method for this.
 * @param val
 * @param times
 * @return
 */	public static String repeat(String val, int times) {
		StringBuilder buf = new StringBuilder();
		for (int i=0; i<times; i++)
			buf.append(val);
		return buf.toString();
	}
}
