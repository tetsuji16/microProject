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
package com.microproject.field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.datatype.Hyperlink;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.model.NodeModelDataFactory;
import com.microproject.grouping.core.model.WalkersNodeModel;
import com.microproject.server.data.DataObject;

class FieldSetValueTest {
	@Test
	void preservesParseExceptionThrownByReflectiveSetter() {
		FieldParseException expected = new FieldParseException("invalid field value");
		Field field = new Field();
		field.setClass(ThrowingSetter.class);
		field.setProperty("value");
		field.setId("Field.testValue");
		field.build();

		FieldParseException actual = assertThrows(FieldParseException.class,
			() -> field.setValue(new ThrowingSetter(expected), this, "value", null));

		assertSame(expected, actual);
	}

	@Test
	void marksDataObjectDirtyAfterReflectiveFieldSet() throws Exception {
		Field field = new Field();
		field.setClass(MutableDataObject.class);
		field.setProperty("name");
		field.setId("Field.testName");
		field.build();
		MutableDataObject target = new MutableDataObject();
		target.setDirty(false);

		field.setValue(target, this, "updated", null);

		assertEquals("updated", target.getName());
		assertTrue(target.isDirty());
	}

	@Test
	void appliesValueToEveryObjectInObjectRefCollection() throws Exception {
		Field field = new Field();
		field.setClass(MutableDataObject.class);
		field.setProperty("name");
		field.setId("Field.testName");
		field.build();
		MutableDataObject first = new MutableDataObject();
		MutableDataObject second = new MutableDataObject();

		field.setValue(objectRef(List.of(first, second)), this, "updated", null);

		assertEquals("updated", first.getName());
		assertEquals("updated", second.getName());
	}

	@Test
	void objectRefReadOnlyChecksCollectionElementsAndEmptyCollections() {
		Field field = new Field();
		field.setClass(MutableDataObject.class);
		field.setProperty("name");
		field.setId("Field.testName");
		field.build();
		MutableDataObject target = new MutableDataObject();

		assertFalse(field.isReadOnly(objectRef(List.of(target)), null));
		field.setReadOnly(true);
		assertTrue(field.isReadOnly(objectRef(List.of(target)), null));
		assertFalse(field.isReadOnly(objectRef(List.of()), null));
	}

	private static ObjectRef objectRef(Collection<?> collection) {
		return new ObjectRef() {
			@Override public Node getNode() { return null; }
			@Override public WalkersNodeModel getNodeModel() { return null; }
			@Override public Object getObject() { return null; }
			@Override public Collection<?> getCollection() { return collection; }
			@Override public NodeModelDataFactory getDataFactory() { return null; }
		};
	}

	@Test
	void invokesHyperlinkReturnedByConfiguredField() {
		Field field = new Field();
		field.setClass(HyperlinkHolder.class);
		field.setProperty("link");
		field.setId("Field.testLink");
		field.setAction("open");
		field.build();
		TrackingHyperlink link = new TrackingHyperlink();

		field.invokeAction(new HyperlinkHolder(link));

		assertTrue(link.invoked);
	}

	public static final class ThrowingSetter {
		private final FieldParseException failure;

		ThrowingSetter(FieldParseException failure) {
			this.failure = failure;
		}

		public String getValue() {
			return "initial";
		}

		public void setValue(String value, FieldContext context) throws FieldParseException {
			throw failure;
		}
	}

	public static final class MutableDataObject implements DataObject {
		private String name = "initial";
		private long uniqueId;
		private boolean dirty;

		@Override public String getName() { return name; }
		@Override public void setName(String name) { this.name = name; }
		@Override public long getUniqueId() { return uniqueId; }
		@Override public void setUniqueId(long uniqueId) { this.uniqueId = uniqueId; }
		@Override public boolean isDirty() { return dirty; }
		@Override public void setDirty(boolean dirty) { this.dirty = dirty; }
	}

	public static final class HyperlinkHolder {
		private final Hyperlink link;

		HyperlinkHolder(Hyperlink link) {
			this.link = link;
		}

		public Hyperlink getLink() {
			return link;
		}
	}

	private static final class TrackingHyperlink extends Hyperlink {
		private boolean invoked;

		TrackingHyperlink() {
			super("test", "https://example.invalid");
		}

		@Override
		public void invoke() {
			invoked = true;
		}
	}
}
