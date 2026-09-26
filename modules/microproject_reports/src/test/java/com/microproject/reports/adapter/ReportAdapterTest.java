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
package com.microproject.reports.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.configuration.ReportColumns;
import com.microproject.configuration.ReportDefinition;
import com.microproject.field.Field;
import com.microproject.graphic.configuration.SpreadSheetFieldArray;

class ReportAdapterTest {
	@Test
	void designFieldTraversalPreservesConfiguredFieldOrder() throws Exception {
		ReportDefinition definition = new ReportDefinition();
		definition.setName("field-order");
		definition.add(new ReportColumns());
		SpreadSheetFieldArray fields = new SpreadSheetFieldArray();
		fields.add(field("Field.First", "First"));
		fields.add(field("Field.Second", "Second"));
		ReportAdapter adapter = new ReportAdapter(definition);

		adapter.generateDesign(fields);

		assertEquals(List.of("MODTextFIELDFirst", "MODTextFIELDSecond"),
			adapter.getJasperDesign().getFieldsList().stream().map(field -> field.getName()).toList());
	}

	private static Field field(String id, String name) {
		Field field = new Field();
		field.setId(id);
		field.setName(name);
		field.setColumnWidth(100);
		return field;
	}
}
