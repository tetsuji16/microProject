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
package com.microproject.field;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.microproject.configuration.Configuration;
import com.microproject.server.access.ErrorLogger;


/**
 * Used to write an object's field data to a map
 */
public class FieldValues {
	private static final Logger logger = Logger.getLogger(FieldValues.class.getName());
	
	public static HashMap getValues(Collection fields, Object object) {
		Iterator i = fields.iterator();
		FieldContext context = null;
		// LinkedHashMap preserves insertion order so the serialized POD is byte-stable
		// across load/save round-trips (see issue #227: non-deterministic map order caused drift).
		HashMap map = new LinkedHashMap();
		while (i.hasNext()) {
			Field field = (Field)i.next();
			try {
				Object value = field.getValue(object,context);
				if (value != null && value instanceof Serializable)
					map.put(field.getId(), value);
			} catch (Exception e) {
				ErrorLogger.logOnce(field.getName(),"Problem getting field value in FieldValues",e); // a user had a strange java.lang.NumberFormatException: Infinite or NaN on a Money field
			}
		}
		return map;
	}
	
	public static void setValuesFromFieldIds(Map map, Object object) {
		if (map == null)
			return;
		Iterator i = map.keySet().iterator();
		FieldContext context = FieldContext.getNoDirtyInstance();
		while (i.hasNext()) {
			String fieldId = (String)i.next();
			Field f = Configuration.getFieldFromId(fieldId);
			if (f==null) continue; //LC fix
			try {
				boolean readOnly = f.isReadOnly();
				f.setReadOnly(false);
				f.setValue(object,null,map.get(fieldId),context);
				f.setReadOnly(readOnly);
			} catch (FieldParseException e) {
				ErrorLogger.log("Problem setting field value for " + f.getName() + " in FieldValues", e);
			}
		}
	}
	public static void dump(Map map) {
		Iterator i = map.keySet().iterator();
		while (i.hasNext()) {
			String fieldId = (String)i.next();
			logger.log(Level.INFO, "Field {0} value {1}", new Object[] {Configuration.getFieldFromId(fieldId), map.get(fieldId)});
		}
	}
	
}
