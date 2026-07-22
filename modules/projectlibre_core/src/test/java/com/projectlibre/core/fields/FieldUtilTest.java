package com.projectlibre.core.fields;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FieldUtilTest {
	@Test
	void exportSelectsSetterCompatibleWithValueType() {
		TestFields fields = new TestFields(Integer.valueOf(42));
		OverloadedTarget target = new OverloadedTarget();

		FieldUtil.convertField(fields, OverloadedTarget.class, target, "value", -1, "value", -1, null, false);

		assertEquals(Integer.valueOf(42), target.value);
	}

	private static final class TestFields implements HasFields {
		private final Object value;

		private TestFields(Object value) {
			this.value = value;
		}

		public Object getPropertyValue(String property) { return value; }
		public void setPropertyValue(String property, Object value) { }
		public Object getFieldValue(String fieldId) { return null; }
		public void setFieldValue(String fieldId, Object value) { }
	}

	public static final class OverloadedTarget {
		private Number value;

		public void setValue(String value) {
			throw new AssertionError("wrong overload");
		}

		public void setValue(Number value) {
			this.value = value;
		}
	}
}
