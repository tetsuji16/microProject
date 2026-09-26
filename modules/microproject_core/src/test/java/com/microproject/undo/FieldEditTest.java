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
package com.microproject.undo;

import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Collection;

import org.junit.jupiter.api.Test;

import com.microproject.field.Field;
import com.microproject.field.FieldContext;
import com.microproject.field.FieldParseException;
import com.microproject.field.ObjectRef;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.model.NodeModelDataFactory;
import com.microproject.grouping.core.model.WalkersNodeModel;

class FieldEditTest {
	@Test
	void undoAndRedoKeepObjectReferenceTargetAndValues() throws Exception {
		ObjectRef target = new TestObjectRef();
		Object source = new Object();
		Object oldValue = new Object();
		Object newValue = new Object();
		RecordingField field = new RecordingField();
		FieldEdit edit = new FieldEdit(field, target, newValue, oldValue, source, null);

		edit.undo();
		assertSame(target, field.target);
		assertSame(oldValue, field.value);
		assertSame(source, field.source);

		edit.redo();
		assertSame(target, field.target);
		assertSame(newValue, field.value);
		assertSame(source, field.source);
	}

	@Test
	void undoAndRedoKeepPlainObjectTargetAndValues() throws Exception {
		Object target = new Object();
		Object source = new Object();
		Object oldValue = new Object();
		Object newValue = new Object();
		RecordingField field = new RecordingField();
		FieldEdit edit = new FieldEdit(field, target, newValue, oldValue, source, null);

		edit.undo();
		assertSame(target, field.target);
		assertSame(oldValue, field.value);
		assertSame(source, field.source);

		edit.redo();
		assertSame(target, field.target);
		assertSame(newValue, field.value);
		assertSame(source, field.source);
	}

	private static final class RecordingField extends Field {
		private Object target;
		private Object source;
		private Object value;

		@Override
		public void setValue(Object object, Object source, Object value, FieldContext context)
				throws FieldParseException {
			this.target = object;
			this.source = source;
			this.value = value;
		}

		@Override
		public void setValue(ObjectRef objectRef, Object source, Object value, FieldContext context)
				throws FieldParseException {
			this.target = objectRef;
			this.source = source;
			this.value = value;
		}
	}

	private static final class TestObjectRef implements ObjectRef {
		public Node getNode() { return null; }
		public WalkersNodeModel getNodeModel() { return null; }
		public Object getObject() { return null; }
		public Collection<?> getCollection() { return null; }
		public NodeModelDataFactory getDataFactory() { return null; }
	}
}
