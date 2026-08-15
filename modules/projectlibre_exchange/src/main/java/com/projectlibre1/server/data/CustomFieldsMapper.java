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

package com.projectlibre1.server.data;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.projectlibre1.field.CustomFieldsImpl;

import net.sf.mpxj.FieldType;
import net.sf.mpxj.ResourceField;
import net.sf.mpxj.TaskField;

public class CustomFieldsMapper {
	private static final Logger logger = Logger.getLogger(CustomFieldsMapper.class.getName());
	public class Maps {
		public FieldType[] costMap;
		public FieldType[] dateMap;
		public FieldType[] durationMap;
		public FieldType[] finishMap;
		public FieldType[] flagMap;
		public FieldType[] numberMap;
		public FieldType[] startMap;
		public FieldType[] textMap;

		Maps(Class clazz) {
			costMap = mapMpxIndexes(clazz,COST,CustomFieldsImpl.NUM_COST);
			dateMap = mapMpxIndexes(clazz,DATE,CustomFieldsImpl.NUM_DATE);
			durationMap = mapMpxIndexes(clazz,DURATION,CustomFieldsImpl.NUM_DURATION);
			finishMap = mapMpxIndexes(clazz,FINISH,CustomFieldsImpl.NUM_FINISH);
			flagMap = mapMpxIndexes(clazz,FLAG,CustomFieldsImpl.NUM_FLAG);
			numberMap = mapMpxIndexes(clazz,NUMBER,CustomFieldsImpl.NUM_NUMBER);
			startMap = mapMpxIndexes(clazz,START,CustomFieldsImpl.NUM_START);
			textMap = mapMpxIndexes(clazz,TEXT,CustomFieldsImpl.NUM_TEXT);
		}
		private FieldType[] mapMpxIndexes(Class clazz, String text, int count) {
			FieldType result[] = new FieldType[count];
			for (int i = 0;i < count ;i++) {
				try {
					result[i] = (FieldType) clazz.getDeclaredField(text+(i+1)).get(null); // look for something like TEXT12
				} catch (Exception e) {
					logger.log(Level.WARNING, "Failed to map MPX custom field", e);
				}
			}
			return result;
		}

	};
	public Maps resourceMaps = new Maps(ResourceField.class);
	public Maps taskMaps = new Maps(TaskField.class);

	public static CustomFieldsMapper getInstance() {
		if (instance == null)
			instance = new CustomFieldsMapper();
		return instance;
	}

	private static final String COST = "COST";
	private static final String DATE = "DATE";
	private static final String DURATION = "DURATION";
	private static final String FINISH = "FINISH";
	private static final String FLAG = "FLAG";
	private static final String NUMBER = "NUMBER";
	private static final String START = "START";
	private static final String TEXT = "TEXT";
	private static CustomFieldsMapper instance = null;

	public static void cleanUp(){
		instance=null;
	}


}
