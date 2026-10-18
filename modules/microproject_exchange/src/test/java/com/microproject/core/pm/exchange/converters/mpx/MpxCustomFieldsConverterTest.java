/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.core.pm.exchange.converters.mpx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Date;
import java.util.TimeZone;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.microproject.datatype.DurationDefaults;
import com.microproject.field.CustomFields;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.server.data.CustomFieldsMapper;
import com.microproject.undo.DataFactoryUndoController;

import net.sf.mpxj.ProjectFile;
import net.sf.mpxj.ResourceField;
import net.sf.mpxj.TaskField;

class MpxCustomFieldsConverterTest {
	private static final long DATE = 1_716_840_000_000L;

	@Test
	void importsAllIndexedCustomFieldTypesForTasksAndResourcesAndPersistsThemInMpo() throws Exception {
		Project project = project();
		NormalTask task = project.createScriptedTask();
		task.setName("Custom values");
		Resource resource = project.getResourcePool().createScriptedResource();
		resource.setName("Custom values");
		ProjectFile mpx = new ProjectFile();
		net.sf.mpxj.Task sourceTask = mpx.addTask();
		net.sf.mpxj.Resource sourceResource = mpx.addResource();
		setValues(sourceTask, TaskField.TEXT1, TaskField.NUMBER1, TaskField.DATE1, TaskField.DURATION1,
			TaskField.FLAG1, TaskField.COST1, TaskField.START1, TaskField.FINISH1);
		setValues(sourceResource, ResourceField.TEXT1, ResourceField.NUMBER1, ResourceField.DATE1,
			ResourceField.DURATION1, ResourceField.FLAG1, ResourceField.COST1, ResourceField.START1,
			ResourceField.FINISH1);

		TimeZone originalZone = TimeZone.getDefault();
		TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
		try {
			DurationDefaults defaults = new DurationDefaults(480, 2400, 20);
			MpxImportState state = new MpxImportState();
			state.setProjectBaseCalendar(project.getWorkCalendar());
			new MpxTaskConverter().from(sourceTask, task, state);
			new MpxResourceConverter().from(sourceResource, resource, state);
			assertAllValues(task.getCustomFields(), defaults);
			assertAllValues(resource, defaults);

			java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
			org.junit.jupiter.api.Assertions.assertTrue(new com.microproject.exchange.MpoFileImporter().saveProject(project, bytes));
			Project reopened = new com.microproject.exchange.MpoFileImporter().loadProject(
				new java.io.ByteArrayInputStream(bytes.toByteArray()));
			assertAllValues(reopened.getTaskList().stream().filter(t -> "Custom values".equals(t.getName()))
				.findFirst().orElseThrow().getCustomFields(), defaults);
			assertAllValues(ResourcePool.findResourceByName("Custom values", reopened.getResourcePool()), defaults);
		} finally {
			TimeZone.setDefault(originalZone);
		}
	}

	@Test
	void absentCustomValuesDoNotClearExistingIndexedValues() {
		ProjectFile source = new ProjectFile();
		net.sf.mpxj.Task task = source.addTask();
		Project target = project();
		NormalTask existing = target.createScriptedTask();
		existing.setCustomText(0, "keep");
		MpxCustomFieldsConverter.from(task, existing.getCustomFields(), CustomFieldsMapper.getInstance().taskMaps,
			target.getDurationDefaults());
		assertEquals("keep", existing.getCustomText(0));
		assertNull(existing.getCustomText(1));
	}

	@Test
	void rejectsNonFiniteNumericValuesAndUnexpectedMpxjValueTypes() throws Exception {
		assertCustomMspdiRejected("<Task><ExtendedAttribute><FieldID>188743786</FieldID><Value>NaN</Value>"
			+ "</ExtendedAttribute></Task>", "Invalid MSPDI COST value");
		assertCustomMspdiRejected("<Task><ExtendedAttribute><FieldID>188743767</FieldID><Value>Infinity</Value>"
			+ "</ExtendedAttribute></Task>", "Invalid MSPDI NUMBER value");
		assertCustomMspdiRejected("<Task><ExtendedAttribute><FieldID>188743767</FieldID><Value>not-a-number</Value>"
			+ "</ExtendedAttribute></Task>", "Invalid MSPDI NUMBER value");

		ProjectFile source = new ProjectFile();
		net.sf.mpxj.Task sourceTask = source.addTask();
		Project target = project();
		NormalTask task = target.createScriptedTask();
		sourceTask.set(TaskField.COST1, Double.valueOf(Double.POSITIVE_INFINITY));
		assertThrows(IllegalArgumentException.class, () -> MpxCustomFieldsConverter.from(sourceTask,
			task.getCustomFields(), CustomFieldsMapper.getInstance().taskMaps, target.getDurationDefaults()));
		sourceTask.set(TaskField.COST1, null);
		sourceTask.set(TaskField.NUMBER1, "12.5");
		assertThrows(IllegalArgumentException.class, () -> MpxCustomFieldsConverter.from(sourceTask,
			task.getCustomFields(), CustomFieldsMapper.getInstance().taskMaps, target.getDurationDefaults()));
		sourceTask.set(TaskField.NUMBER1, null);
		sourceTask.set(TaskField.DURATION1,
			net.sf.mpxj.Duration.getInstance(Double.NaN, net.sf.mpxj.TimeUnit.HOURS));
		assertThrows(IllegalArgumentException.class, () -> MpxCustomFieldsConverter.from(sourceTask,
			task.getCustomFields(), CustomFieldsMapper.getInstance().taskMaps, target.getDurationDefaults()));
	}

	@Test
	void importsLiteralMspdiTaskAndResourceTextFieldIdsAndRoundTripsThroughMpo() throws Exception {
		Path fixture = Path.of("testdata", "mpxj", "project2019", "assignment-assignments-project2019-mspdi.xml");
		if (!Files.isRegularFile(fixture))
			fixture = Path.of("modules", "microproject_exchange", "testdata", "mpxj", "project2019",
				"assignment-assignments-project2019-mspdi.xml");
		String xml = Files.readString(fixture, StandardCharsets.UTF_8);
		DurationDefaults defaults = new DurationDefaults(480, 2400, 20);
		assertEquals(188743731, net.sf.mpxj.common.FieldTypeHelper.getFieldID(TaskField.TEXT1),
			"Microsoft MSPDI Task Text1 FieldID");
		assertEquals(205520904, net.sf.mpxj.common.FieldTypeHelper.getFieldID(ResourceField.TEXT1),
			"Microsoft MSPDI Resource Text1 FieldID");
		MspdiField[] taskFields = {
			new MspdiField(188743786, 0, "1250"), // Cost1
			new MspdiField(188743945, 1, "2024-05-27T00:00:00"), // Date1
			new MspdiField(188743783, 2, "PT16H0M0S"), // Duration1
			new MspdiField(188743733, 3, "2024-05-29T00:00:00"), // Finish1
			new MspdiField(188743752, 4, "1"), // Flag1
			new MspdiField(188743767, 5, "2.75"), // Number1
			new MspdiField(188743732, 6, "2024-05-28T00:00:00"), // Start1
			new MspdiField(188743731, 7, "Task source value") // Text1
		};
		MspdiField[] resourceFields = {
			new MspdiField(205521019, 0, "2250"), // Cost1
			new MspdiField(205521069, 1, "2024-05-27T00:00:00"), // Date1
			new MspdiField(205521013, 2, "PT8H0M0S"), // Duration1
			new MspdiField(205521003, 3, "2024-05-29T00:00:00"), // Finish1
			new MspdiField(205521023, 4, "1"), // Flag1
			new MspdiField(205521008, 5, "3.75"), // Number1
			new MspdiField(205520998, 6, "2024-05-28T00:00:00"), // Start1
			new MspdiField(205520904, 7, "Resource source value") // Text1
		};
		assertFieldIds(taskFields, TaskField.COST1, TaskField.DATE1, TaskField.DURATION1, TaskField.FINISH1,
			TaskField.FLAG1, TaskField.NUMBER1, TaskField.START1, TaskField.TEXT1);
		assertFieldIds(resourceFields, ResourceField.COST1, ResourceField.DATE1, ResourceField.DURATION1,
			ResourceField.FINISH1, ResourceField.FLAG1, ResourceField.NUMBER1, ResourceField.START1,
			ResourceField.TEXT1);
		xml = xml.replace("<ExtendedAttributes/>", definitions(taskFields, resourceFields));
		xml = insertCustomValues(xml, "Tasks", "Task", "Task 1", taskFields);
		xml = insertCustomValues(xml, "Resources", "Resource", "Resource 1", resourceFields);
		TimeZone originalZone = TimeZone.getDefault();
		TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
		try {
			net.sf.mpxj.ProjectFile parsedSource = new net.sf.mpxj.mspdi.MSPDIReader()
				.read(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
			net.sf.mpxj.Task parsedTask = parsedSource.getTasks().stream()
				.filter(t -> "Task 1".equals(t.getName())).findFirst().orElseThrow();
			net.sf.mpxj.Resource parsedResource = parsedSource.getResources().stream()
				.filter(r -> "Resource 1".equals(r.getName())).findFirst().orElseThrow();
			long[] taskDateValues = customFieldDates(parsedTask, TaskField.DATE1, TaskField.FINISH1, TaskField.START1);
			long[] resourceDateValues = customFieldDates(parsedResource, ResourceField.DATE1,
				ResourceField.FINISH1, ResourceField.START1);
			Project imported = new com.microproject.core.pm.exchange.MspImporter().importProject(
				new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), "xml", (progress, label) -> {});
			NormalTask task = imported.getTaskList().stream().filter(t -> "Task 1".equals(t.getName()))
				.map(t -> (NormalTask) t).findFirst().orElseThrow();
			Resource importedResource = ResourcePool.findResourceByName("Resource 1", imported.getResourcePool());
			assertMspdiValues(task.getCustomFields(), taskFields, defaults, taskDateValues);
			assertMspdiValues(importedResource, resourceFields, defaults, resourceDateValues);

			java.io.ByteArrayOutputStream archive = new java.io.ByteArrayOutputStream();
			org.junit.jupiter.api.Assertions.assertTrue(new com.microproject.exchange.MpoFileImporter()
				.saveProject(imported, archive));
			Project reopened = new com.microproject.exchange.MpoFileImporter().loadProject(
				new java.io.ByteArrayInputStream(archive.toByteArray()));
			assertMspdiValues(reopened.getTaskList().stream()
				.filter(t -> "Task 1".equals(t.getName())).findFirst().orElseThrow().getCustomFields(),
				taskFields, defaults, taskDateValues);
			assertMspdiValues(ResourcePool.findResourceByName("Resource 1", reopened.getResourcePool()),
				resourceFields, defaults, resourceDateValues);
		} finally {
			TimeZone.setDefault(originalZone);
		}
	}

	@Test
	void customFieldValidationRejectsUnrepresentableValuesAndOnlyAcceptsEqualDuplicates() throws Exception {
		assertCustomMspdiRejected("<Task><ExtendedAttribute><Value>x</Value></ExtendedAttribute></Task>",
			"ExtendedAttribute is missing FieldID");
		assertCustomMspdiRejected("<Task><ExtendedAttribute><FieldID>999999999</FieldID><Value>x</Value>"
			+ "</ExtendedAttribute></Task>", "Unsupported MSPDI Task custom-field FieldID");
		assertCustomMspdiRejected("<Task><ExtendedAttribute><FieldID>bad</FieldID><Value>x</Value>"
			+ "</ExtendedAttribute></Task>", "custom FieldID is malformed");
		assertCustomMspdiRejected("<Task><ExtendedAttribute><FieldID>188743731</FieldID></ExtendedAttribute></Task>",
			"is missing Value");
		assertCustomMspdiRejected("<Task><ExtendedAttribute><FieldID>188743731</FieldID>"
			+ "<ValueGUID>{00000000-0000-0000-0000-000000000001}</ValueGUID></ExtendedAttribute></Task>",
			"ValueGUID lookup values are not supported");
		assertCustomMspdiRejected("<Assignment><ExtendedAttribute><FieldID>188743731</FieldID><Value>x</Value>"
			+ "</ExtendedAttribute></Assignment>", "Unsupported MSPDI Assignment custom-field FieldID");
		assertCustomMspdiRejected("<Task><ExtendedAttribute><FieldID>188743731</FieldID><Value>a</Value>"
			+ "</ExtendedAttribute><ExtendedAttribute><FieldID>188743731</FieldID><Value>b</Value>"
			+ "</ExtendedAttribute></Task>", "conflicting duplicate values");
		assertCustomMspdiAccepted("<Task><ExtendedAttribute><FieldID>188743731</FieldID><Value>a</Value>"
			+ "</ExtendedAttribute><ExtendedAttribute><FieldID>188743731</FieldID><Value>a</Value>"
			+ "</ExtendedAttribute></Task>");
	}

	private static void assertCustomMspdiAccepted(String payload) throws Exception {
		String namespace = "http://schemas.microsoft.com/project";
		String xml = "<Project xmlns=\"" + namespace + "\">" + payload + "</Project>";
		javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		org.w3c.dom.Document document = factory.newDocumentBuilder()
			.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
		MpxCustomFieldsConverter.validateMspdiValues(document, namespace);
	}

	private static void assertCustomMspdiRejected(String payload, String message) throws Exception {
		String namespace = "http://schemas.microsoft.com/project";
		String xml = "<Project xmlns=\"" + namespace + "\">" + payload + "</Project>";
		javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		org.w3c.dom.Document document = factory.newDocumentBuilder()
			.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
		IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
			() -> MpxCustomFieldsConverter.validateMspdiValues(document, namespace));
		org.junit.jupiter.api.Assertions.assertTrue(failure.getMessage().contains(message), failure.getMessage());
	}

	private static String insertCustomValue(String xml, String section, String element, String name,
			int fieldId, String value) {
		int sectionStart = xml.indexOf("<" + section + ">");
		int namedElement = xml.indexOf("<Name>" + name + "</Name>", sectionStart);
		int elementStart = xml.lastIndexOf("<" + element + ">", namedElement);
		int elementEnd = xml.indexOf("</" + element + ">", namedElement);
		if (sectionStart < 0 || namedElement < 0 || elementStart < 0 || elementEnd < 0)
			throw new AssertionError("Could not find MSPDI " + section + " element named " + name);
		String extension = "<ExtendedAttribute><FieldID>" + fieldId + "</FieldID><Value>" + value
			+ "</Value></ExtendedAttribute>";
		return xml.substring(0, elementEnd) + extension + xml.substring(elementEnd);
	}

	private record MspdiField(int id, int type, String value) { }

	private static String definitions(MspdiField[] taskFields, MspdiField[] resourceFields) {
		StringBuilder result = new StringBuilder("<ExtendedAttributes>");
		appendDefinitions(result, taskFields, "Task");
		appendDefinitions(result, resourceFields, "Resource");
		return result.append("</ExtendedAttributes>").toString();
	}

	private static void appendDefinitions(StringBuilder xml, MspdiField[] fields, String scope) {
		for (MspdiField field : fields)
			xml.append("<ExtendedAttribute><FieldID>").append(field.id()).append("</FieldID><FieldName>")
				.append(scope).append(field.type()).append("</FieldName><Alias>").append(scope).append(" custom")
				.append(field.type()).append("</Alias><CFType>").append(field.type())
				.append("</CFType></ExtendedAttribute>");
	}

	private static String insertCustomValues(String xml, String section, String element, String name,
			MspdiField[] fields) {
		for (MspdiField field : fields)
			xml = insertCustomValue(xml, section, element, name, field.id(), field.value());
		return xml;
	}

	private static void assertFieldIds(MspdiField[] fields, net.sf.mpxj.FieldType... mpxFields) {
		assertEquals(fields.length, mpxFields.length);
		for (int i = 0; i < fields.length; i++)
			assertEquals(fields[i].id(), net.sf.mpxj.common.FieldTypeHelper.getFieldID(mpxFields[i]));
	}

	private static void assertMspdiValues(CustomFields target, MspdiField[] fields,
			DurationDefaults defaults, long[] dateValues) {
		boolean task = fields[0].value().equals("1250");
		// MPXJ's pinned MSPDI converter uses hundredths for currency values.
		assertEquals(Double.parseDouble(fields[0].value()) / 100D, target.getCustomCost(0));
		assertEquals(dateValues[0], target.getCustomDate(0));
		assertEquals(MpxUtils.toProjectDuration(net.sf.mpxj.Duration.getInstance(
			task ? 2.0 : 1.0, net.sf.mpxj.TimeUnit.DAYS), defaults), target.getCustomDuration(0));
		assertEquals(dateValues[1], target.getCustomFinish(0));
		assertEquals(true, target.getCustomFlag(0));
		assertEquals(Double.parseDouble(fields[5].value()), target.getCustomNumber(0));
		assertEquals(dateValues[2], target.getCustomStart(0));
		assertEquals(task ? "Task source value" : "Resource source value", target.getCustomText(0));
	}

	private static long[] customFieldDates(net.sf.mpxj.FieldContainer source,
			net.sf.mpxj.FieldType date, net.sf.mpxj.FieldType finish, net.sf.mpxj.FieldType start) {
		// Derive expectations from MPXJ's parsed literal MSPDI values so this test
		// isolates our indexed-field mapping from MPXJ's host-zone parsing rules.
		return java.util.stream.Stream.of(date, finish, start)
			.mapToLong(field -> com.microproject.core.pm.exchange.converters.type.DateUTCConverter
				.toModelTime((Date) source.get(field)))
			.toArray();
	}

	private static void setValues(net.sf.mpxj.FieldContainer fields, net.sf.mpxj.FieldType text,
			net.sf.mpxj.FieldType number, net.sf.mpxj.FieldType date, net.sf.mpxj.FieldType duration,
			net.sf.mpxj.FieldType flag, net.sf.mpxj.FieldType cost, net.sf.mpxj.FieldType start,
			net.sf.mpxj.FieldType finish) {
		fields.set(text, "field text");
		fields.set(number, Double.valueOf(12.5D));
		fields.set(date, new Date(DATE));
		fields.set(duration, net.sf.mpxj.Duration.getInstance(2.0, net.sf.mpxj.TimeUnit.DAYS));
		fields.set(flag, Boolean.TRUE);
		fields.set(cost, Double.valueOf(44.25D));
		fields.set(start, new Date(DATE + 1000));
		fields.set(finish, new Date(DATE + 2000));
	}

	private static void assertAllValues(CustomFields fields, DurationDefaults defaults) {
		assertEquals("field text", fields.getCustomText(0));
		assertEquals(12.5D, fields.getCustomNumber(0));
		assertEquals(com.microproject.core.pm.exchange.converters.type.DateUTCConverter.toModelTime(new Date(DATE)),
			fields.getCustomDate(0));
		assertEquals(MpxUtils.toProjectDuration(net.sf.mpxj.Duration.getInstance(2.0,
			net.sf.mpxj.TimeUnit.DAYS), defaults), fields.getCustomDuration(0));
		assertEquals(true, fields.getCustomFlag(0));
		assertEquals(44.25D, fields.getCustomCost(0));
		assertEquals(com.microproject.core.pm.exchange.converters.type.DateUTCConverter.toModelTime(new Date(DATE + 1000)),
			fields.getCustomStart(0));
		assertEquals(com.microproject.core.pm.exchange.converters.type.DateUTCConverter.toModelTime(new Date(DATE + 2000)),
			fields.getCustomFinish(0));
	}

	private static Project project() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("custom fields", undo), undo);
		project.setDurationDefaults(new DurationDefaults(480, 2400, 20));
		return project;
	}
}
