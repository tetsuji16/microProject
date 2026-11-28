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
package com.microproject.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.digester3.Digester;
import org.junit.jupiter.api.Test;
import org.xml.sax.helpers.AttributesImpl;

public class ConfigurationBeanContractTest {
	@Test
	void digesterMapsChartAndScriptClassXmlAttributesToExistingBeanProperties() throws Exception {
		BeanRoot root = new BeanRoot();
		Digester digester = new Digester();
		digester.push(root);
		ChartDefinition.addDigesterEvents(digester);
		ScriptConfiguration.addDigesterEvents(digester);

		digester.parse(new StringReader("""
				<configuration>
					<charts>
						<chart name="forecast" title="Forecast" type="bar" x="start" y="work" z="finish"
								scaled="true" timeBased="true"/>
					</charts>
					<script><class name="com.example.AllowedScriptType"/></script>
				</configuration>
				"""));

		assertEquals(1, root.charts.size());
		ChartDefinition chart = (ChartDefinition) root.charts.getFirst();
		assertEquals("forecast", chart.getName());
		assertEquals("Forecast", chart.getTitle());
		assertEquals("bar", chart.getType());
		assertEquals("start", chart.getX());
		assertEquals("work", chart.getY());
		assertEquals("finish", chart.getZ());
		assertTrue(chart.isScaled());
		assertTrue(chart.isTimeBased());
		assertTrue(root.scriptConfiguration.canBeResoved("com.example.AllowedScriptType"));
	}

	@Test
	void fieldDictionaryFactoryReusesConfiguredDictionaryAndCreatesWhenUnset() throws Exception {
		Configuration configuration = Configuration.getInstance();
		FieldDictionary original = configuration.getFieldDictionary();
		FieldDictionaryFactory factory = new FieldDictionaryFactory();

		try {
			assertSame(original, factory.createObject(new AttributesImpl()));

			configuration.setFieldDictionary(null);
			FieldDictionary created = (FieldDictionary) factory.createObject(new AttributesImpl());
			assertNotNull(created);
			assertNotSame(original, created);
		} finally {
			configuration.setFieldDictionary(original);
		}
	}

	public static final class BeanRoot {
		private final List<NamedItem> charts = new ArrayList<>();
		private ScriptConfiguration scriptConfiguration;

		public void add(NamedItem chart) {
			charts.add(chart);
		}

		public void setScriptConfiguration(ScriptConfiguration scriptConfiguration) {
			this.scriptConfiguration = scriptConfiguration;
		}
	}
}
