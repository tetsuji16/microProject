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
import java.util.Map;
import java.util.HashMap;
import java.math.BigDecimal;

import com.microproject.datatype.DurationDefaults;
import com.microproject.field.CustomFields;
import com.microproject.server.data.CustomFieldsMapper;

import net.sf.mpxj.FieldContainer;
import net.sf.mpxj.FieldType;

import static com.microproject.exchange.MspdiProjectBoundary.childText;

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
		Map<Integer, ValueType> taskFields = fieldTypes(mapper.taskMaps);
		Map<Integer, ValueType> resourceFields = fieldTypes(mapper.resourceMaps);
		validateElements(document, namespace, "Task", taskFields);
		validateElements(document, namespace, "Resource", resourceFields);
		validateElements(document, namespace, "Assignment", Map.of());
	}

	private enum ValueType { COST, DATE, DURATION, FINISH, FLAG, NUMBER, START, TEXT }

	private static Map<Integer, ValueType> fieldTypes(CustomFieldsMapper.Maps maps) {
		Map<Integer, ValueType> result = new HashMap<>();
		addFieldTypes(result, maps.costMap, ValueType.COST);
		addFieldTypes(result, maps.dateMap, ValueType.DATE);
		addFieldTypes(result, maps.durationMap, ValueType.DURATION);
		addFieldTypes(result, maps.finishMap, ValueType.FINISH);
		addFieldTypes(result, maps.flagMap, ValueType.FLAG);
		addFieldTypes(result, maps.numberMap, ValueType.NUMBER);
		addFieldTypes(result, maps.startMap, ValueType.START);
		addFieldTypes(result, maps.textMap, ValueType.TEXT);
		return result;
	}

	private static void addFieldTypes(Map<Integer, ValueType> types, FieldType[] fields, ValueType type) {
		for (FieldType field : fields)
			if (field != null) types.put(Integer.valueOf(net.sf.mpxj.common.FieldTypeHelper.getFieldID(field)), type);
	}

	private static void validateElements(org.w3c.dom.Document document, String namespace, String elementName,
			Map<Integer, ValueType> supportedFields) {
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
				ValueType type = supportedFields.get(Integer.valueOf(fieldId));
				if (type == null)
					throw new IllegalArgumentException("Unsupported MSPDI " + elementName
						+ " custom-field FieldID " + fieldId + "; import would lose its value");
				String value = childText(extended, "Value", namespace);
				String valueGuid = childText(extended, "ValueGUID", namespace);
				if (valueGuid != null)
					throw new IllegalArgumentException("MSPDI custom-field ValueGUID lookup values are not supported");
				if (value == null)
					throw new IllegalArgumentException("MSPDI custom FieldID " + fieldId + " is missing Value");
				validateValueType(elementName, fieldId, type, value);
				String previous = seen.putIfAbsent(Integer.valueOf(fieldId), value);
				if (previous != null && !previous.equals(value))
					throw new IllegalArgumentException("MSPDI " + elementName
						+ " has conflicting duplicate values for custom FieldID " + fieldId);
			}
		}
	}

	private static void validateValueType(String elementName, int fieldId, ValueType type, String value) {
		try {
			switch (type) {
			case COST, NUMBER -> new BigDecimal(value.trim());
			case DATE, FINISH, START -> {
				javax.xml.datatype.XMLGregorianCalendar date = javax.xml.datatype.DatatypeFactory.newInstance()
					.newXMLGregorianCalendar(value.trim());
				if (!date.isValid() || date.getYear() == javax.xml.datatype.DatatypeConstants.FIELD_UNDEFINED
						|| date.getMonth() == javax.xml.datatype.DatatypeConstants.FIELD_UNDEFINED
						|| date.getDay() == javax.xml.datatype.DatatypeConstants.FIELD_UNDEFINED)
					throw new IllegalArgumentException("invalid XML date");
			}
			case DURATION -> {
				// MPXJ parses this using the field's DurationFormat metadata; do not
				// impose a second lexical format here.
			}
			case FLAG -> {
				if (!("0".equals(value.trim()) || "1".equals(value.trim())
						|| "true".equals(value.trim()) || "false".equals(value.trim())))
					throw new IllegalArgumentException("invalid XML boolean");
			}
			case TEXT -> { }
			}
		} catch (javax.xml.datatype.DatatypeConfigurationException | IllegalArgumentException invalid) {
			throw new IllegalArgumentException("Invalid MSPDI " + type + " value for " + elementName
				+ " custom FieldID " + fieldId + ": " + value, invalid);
		}
	}

	private static void copyNumbers(FieldContainer source, CustomFields target, FieldType[] fields, int kind) {
		for (int i = 0; i < fields.length; i++) {
			Number number = (Number) typedValue(source, fields[i], Number.class);
			if (number != null) {
				double value = number.doubleValue();
				if (!Double.isFinite(value))
					throw invalidSourceValue(fields[i], "finite number", number);
				if (kind == 0) target.setCustomCost(i, value);
				else target.setCustomNumber(i, value);
			}
		}
	}

	private static void copyDates(FieldContainer source, CustomFields target, FieldType[] fields, int kind) {
		for (int i = 0; i < fields.length; i++) {
			Date date = (Date) typedValue(source, fields[i], Date.class);
			if (date != null) {
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
			net.sf.mpxj.Duration duration = (net.sf.mpxj.Duration) typedValue(source, fields[i],
				net.sf.mpxj.Duration.class);
			if (duration != null) {
				if (!Double.isFinite(duration.getDuration()))
					throw invalidSourceValue(fields[i], "finite duration", duration);
				target.setCustomDuration(i, MpxUtils.toProjectDuration(duration, defaults));
			}
		}
	}

	private static void copyFlags(FieldContainer source, CustomFields target, FieldType[] fields) {
		for (int i = 0; i < fields.length; i++) {
			Boolean flag = (Boolean) typedValue(source, fields[i], Boolean.class);
			if (flag != null) target.setCustomFlag(i, flag.booleanValue());
		}
	}

	private static void copyTexts(FieldContainer source, CustomFields target, FieldType[] fields) {
		for (int i = 0; i < fields.length; i++) {
			String text = (String) typedValue(source, fields[i], String.class);
			if (text != null) target.setCustomText(i, text);
		}
	}

	private static Object typedValue(FieldContainer source, FieldType field, Class<?> expectedType) {
		if (field == null) return null;
		Object value = source.get(field);
		if (value != null && !expectedType.isInstance(value))
			throw invalidSourceValue(field, expectedType.getSimpleName(), value);
		return value;
	}

	private static IllegalArgumentException invalidSourceValue(FieldType field, String expected, Object value) {
		return new IllegalArgumentException("Invalid MPXJ custom-field value for " + field + ": expected "
			+ expected + ", got " + value.getClass().getSimpleName());
	}
}
