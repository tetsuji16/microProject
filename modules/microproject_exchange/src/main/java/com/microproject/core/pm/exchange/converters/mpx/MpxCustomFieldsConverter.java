/*******************************************************************************
 * MIT License
 *
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
package com.microproject.core.pm.exchange.converters.mpx;

import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

import com.microproject.datatype.DurationDefaults;
import com.microproject.field.CustomFields;
import com.microproject.server.data.CustomFieldsMapper;

import net.sf.mpxj.FieldContainer;
import net.sf.mpxj.FieldType;

/** Maps the indexed custom-field types shared by task and resource imports. */
public final class MpxCustomFieldsConverter {
	private MpxCustomFieldsConverter() {
	}

	public static void from(FieldContainer source, CustomFields target,
			CustomFieldsMapper.Maps maps, DurationDefaults defaults) {
		copyNumbers(source, target, maps.costMap, 0);
		copyDates(source, target, maps.dateMap, 0);
		copyDurations(source, target, maps.durationMap, defaults);
		copyDates(source, target, maps.finishMap, 1);
		copyFlags(source, target, maps.flagMap);
		copyNumbers(source, target, maps.numberMap, 1);
		copyDates(source, target, maps.startMap, 2);
		copyTexts(source, target, maps.textMap);
	}

	/**
	 * Fails closed for MSPDI custom-field values that this model cannot represent.
	 * FieldID is the stable identity; aliases and display names are definition metadata
	 * and are not persisted by the current custom-field model. ValueGUID lookup values
	 * also need a value-list model, which is not implemented yet.
	 */
	public static void validateMspdiValues(org.w3c.dom.Document document, String namespace) {
		CustomFieldsMapper mapper = CustomFieldsMapper.getInstance();
		Set<Integer> taskFields = fieldIds(mapper.taskMaps);
		Set<Integer> resourceFields = fieldIds(mapper.resourceMaps);
		validateElements(document, namespace, "Task", taskFields);
		validateElements(document, namespace, "Resource", resourceFields);
		validateElements(document, namespace, "Assignment", Set.of());
	}

	private static Set<Integer> fieldIds(CustomFieldsMapper.Maps maps) {
		Set<Integer> result = new HashSet<>();
		addFieldIds(result, maps.costMap);
		addFieldIds(result, maps.dateMap);
		addFieldIds(result, maps.durationMap);
		addFieldIds(result, maps.finishMap);
		addFieldIds(result, maps.flagMap);
		addFieldIds(result, maps.numberMap);
		addFieldIds(result, maps.startMap);
		addFieldIds(result, maps.textMap);
		return result;
	}

	private static void addFieldIds(Set<Integer> ids, FieldType[] fields) {
		for (FieldType field : fields)
			if (field != null) ids.add(Integer.valueOf(net.sf.mpxj.common.FieldTypeHelper.getFieldID(field)));
	}

	private static void validateElements(org.w3c.dom.Document document, String namespace, String elementName,
			Set<Integer> supportedFields) {
		org.w3c.dom.NodeList elements = document.getElementsByTagNameNS(namespace, elementName);
		for (int i = 0; i < elements.getLength(); i++) {
			org.w3c.dom.Element element = (org.w3c.dom.Element) elements.item(i);
			Map<Integer, String> seen = new HashMap<>();
			for (org.w3c.dom.Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
				if (!(child instanceof org.w3c.dom.Element extended)
						|| !"ExtendedAttribute".equals(extended.getLocalName())) continue;
				String rawFieldId = childText(extended, "FieldID", namespace);
				if (rawFieldId == null)
					throw new IllegalArgumentException("MSPDI " + elementName + " ExtendedAttribute is missing FieldID");
				final int fieldId;
				try { fieldId = Integer.parseInt(rawFieldId.trim()); }
				catch (NumberFormatException invalid) {
					throw new IllegalArgumentException("MSPDI custom FieldID is malformed: " + rawFieldId, invalid);
				}
				if (!supportedFields.contains(Integer.valueOf(fieldId)))
					throw new IllegalArgumentException("Unsupported MSPDI " + elementName
						+ " custom-field FieldID " + fieldId + "; import would lose its value");
				String value = childText(extended, "Value", namespace);
				String valueGuid = childText(extended, "ValueGUID", namespace);
				if (valueGuid != null)
					throw new IllegalArgumentException("MSPDI custom-field ValueGUID lookup values are not supported");
				if (value == null)
					throw new IllegalArgumentException("MSPDI custom FieldID " + fieldId + " is missing Value");
				String previous = seen.putIfAbsent(Integer.valueOf(fieldId), value);
				if (previous != null && !previous.equals(value))
					throw new IllegalArgumentException("MSPDI " + elementName
						+ " has conflicting duplicate values for custom FieldID " + fieldId);
			}
		}
	}

	private static String childText(org.w3c.dom.Element parent, String name, String namespace) {
		for (org.w3c.dom.Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (child instanceof org.w3c.dom.Element element && name.equals(element.getLocalName())
					&& java.util.Objects.equals(namespace, element.getNamespaceURI()))
				return element.getTextContent();
		}
		return null;
	}

	private static void copyNumbers(FieldContainer source, CustomFields target, FieldType[] fields, int kind) {
		for (int i = 0; i < fields.length; i++) {
			Object value = source.get(fields[i]);
			if (value instanceof Number number) {
				if (kind == 0) target.setCustomCost(i, number.doubleValue());
				else target.setCustomNumber(i, number.doubleValue());
			}
		}
	}

	private static void copyDates(FieldContainer source, CustomFields target, FieldType[] fields, int kind) {
		for (int i = 0; i < fields.length; i++) {
			Object value = source.get(fields[i]);
			if (value instanceof Date date) {
				long modelTime = com.microproject.core.pm.exchange.converters.type.DateUTCConverter.toModelTime(date);
				switch (kind) {
				case 0 -> target.setCustomDate(i, modelTime);
				case 1 -> target.setCustomFinish(i, modelTime);
				default -> target.setCustomStart(i, modelTime);
				}
			}
		}
	}

	private static void copyDurations(FieldContainer source, CustomFields target, FieldType[] fields,
			DurationDefaults defaults) {
		for (int i = 0; i < fields.length; i++) {
			Object value = source.get(fields[i]);
			if (value instanceof net.sf.mpxj.Duration duration)
				target.setCustomDuration(i, MpxUtils.toProjectDuration(duration, defaults));
		}
	}

	private static void copyFlags(FieldContainer source, CustomFields target, FieldType[] fields) {
		for (int i = 0; i < fields.length; i++) {
			Object value = source.get(fields[i]);
			if (value instanceof Boolean flag) target.setCustomFlag(i, flag.booleanValue());
		}
	}

	private static void copyTexts(FieldContainer source, CustomFields target, FieldType[] fields) {
		for (int i = 0; i < fields.length; i++) {
			Object value = source.get(fields[i]);
			if (value instanceof String text) target.setCustomText(i, text);
		}
	}
}
