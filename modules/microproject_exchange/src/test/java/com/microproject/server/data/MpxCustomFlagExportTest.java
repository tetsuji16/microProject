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
package com.microproject.server.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.microproject.field.CustomFieldsImpl;

import net.sf.mpxj.ProjectFile;
import net.sf.mpxj.TaskField;

class MpxCustomFlagExportTest {
	@Test
	void exportsTrueCustomFlagsAndOmitsFalseFlags() {
		CustomFieldsImpl fields = new CustomFieldsImpl();
		CustomFieldsMapper mapper = new CustomFieldsMapper();
		CustomFieldsMapper.Maps maps = mapper.new Maps(TaskField.class);
		ProjectFile project = new ProjectFile();
		net.sf.mpxj.Task task = project.addTask();

		MPXConverter.toMpxCustomFields(fields, task, maps);
		assertNull(task.get(TaskField.FLAG1));

		fields.setCustomFlag(0, true);
		MPXConverter.toMpxCustomFields(fields, task, maps);
		assertEquals(Boolean.TRUE, task.get(TaskField.FLAG1));
	}
}
