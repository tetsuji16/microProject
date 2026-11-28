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
package com.microproject.node.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.microproject.node.transform.filtering.NodeFilterFactory;
import com.microproject.persistence.ClassRelocations;

/**
 * The view configuration digester and the stored view settings bind these transform classes by
 * property name, class name and serialized identity, so the compatibility surface has to stay put
 * while the classes themselves are tidied (issue #1513).
 */
class TransformConfigurationCompatibilityTest {
	private static final String REMOVED_EVENT_PACKAGE =
			"com.microproject.grouping.core.transform.ViewTransformerEvent";

	@Test
	void configurationDigesterReadsTheConfiguredTransformIdAndParameters() {
		TransformList filters = TransformList.getInstance("user_filters");
		assertNotNull(filters, "the production view configuration has to load");

		CommonTransformFactory dateRange = filters.getFactory("Filter.DateRange");
		assertNotNull(dateRange, "digester must bind the filter id attribute");
		assertTrue(dateRange instanceof NodeFilterFactory);

		// showEmptySummaries is an attribute on the element, so a changed property name drops it.
		assertEquals(false, dateRange.isShowEmptySummaries(), "digester must bind showEmptySummaries");
		assertEquals(List.of("TransformParameter.StartDate", "TransformParameter.FinishDate"),
				parameterIds(dateRange.getParameters()), "digester must bind <param id=...> children");
		assertNull(dateRange.getParameter("TransformParameter.StartDate"),
				"the XML supplies parameter IDs only; values arrive from the dialog");
	}

	@Test
	void configurationDigesterBindsEveryConfiguredFilterId() {
		TransformList filters = TransformList.getInstance("user_filters");
		assertNotNull(filters);
		assertTrue(filters.getFactories().size() > 1);
		List<String> unbound = new ArrayList<>();
		for (CommonTransformFactory factory : filters.getFactories()) {
			if (factory.getId() == null || filters.getFactory(factory.getId()) != factory) {
				unbound.add(String.valueOf(factory.getId()));
			}
		}
		assertTrue(unbound.isEmpty(), "filters whose id attribute was not bound: " + unbound);
	}

	@Test
	void transformIdKeepsTheBeanPropertiesTheDigesterBinds() throws Exception {
		Set<String> properties = Arrays.stream(Introspector.getBeanInfo(TransformId.class).getPropertyDescriptors())
				.map(PropertyDescriptor::getName).collect(Collectors.toSet());
		assertTrue(properties.containsAll(Set.of("id", "hidden")), "digester binds these attribute names");

		TransformId id = new TransformId();
		assertEquals(false, id.isHidden(), "hidden defaults to false so a missing attribute is visible");
		assertNull(id.getId());
		id.setId("Filter.DateRange");
		id.setHidden(true);
		assertEquals("Filter.DateRange", id.getId());
		assertTrue(id.isHidden());
	}

	@Test
	void transformParameterKeepsHeterogeneousValues() {
		TransformParameter parameter = new TransformParameter();
		assertNull(parameter.getId());
		assertNull(parameter.getValue());
		parameter.setId("TransformParameter.StartDate");

		Date date = new Date(1_700_000_000_000L);
		parameter.setValue(date);
		assertSame(date, parameter.getValue(), "the value is handed through without conversion");
		parameter.setValue("text");
		assertEquals("text", parameter.getValue());
		parameter.setValue(42);
		assertEquals(42, parameter.getValue());
	}

	@Test
	void transformerEventKeepsItsSerializedIdentity() throws Exception {
		assertEquals(6416736838418985755L,
				ObjectStreamClass.lookup(ViewTransformerEvent.class).getSerialVersionUID());
		assertEquals("com.microproject.node.transform.ViewTransformerEvent",
				ViewTransformerEvent.class.getName());
		assertEquals(0, ObjectStreamClass.lookup(ViewTransformerEvent.class).getFields().length,
				"the event adds no serialized fields to EventObject");
		assertEquals(ViewTransformerEvent.class.getName(), ClassRelocations.relocate(REMOVED_EVENT_PACKAGE),
				"stored view state still names the pre-rename class");

		Object source = new Object();
		ViewTransformerEvent event = new ViewTransformerEvent(source);
		assertSame(source, event.getSource());
		try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(serialize(event)))) {
			ViewTransformerEvent read = (ViewTransformerEvent) input.readObject();
			assertNull(read.getSource(), "EventObject keeps its source transient");
		}
	}

	@Test
	void listenerIsAnEventListenerWithASingleNotificationMethod() throws Exception {
		assertTrue(java.util.EventListener.class.isAssignableFrom(ViewTransformerListener.class));
		assertEquals(1, ViewTransformerListener.class.getDeclaredMethods().length);
		assertEquals("transformerChanged", ViewTransformerListener.class.getDeclaredMethods()[0].getName());
	}

	private static List<String> parameterIds(List<TransformParameter> parameters) {
		assertNotNull(parameters);
		return parameters.stream().map(TransformParameter::getId).toList();
	}

	private static byte[] serialize(Object value) throws Exception {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
			output.writeObject(value);
		}
		return bytes.toByteArray();
	}
}
