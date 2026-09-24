package com.microproject.field;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class OptionsFilterTest {
	@Test
	void reflectiveFilterCanRemoveKeysFromMutableCopyWithoutChangingValues() {
		OptionsFilter filter = new OptionsFilter();
		filter.setMethod("removeFirstOption");
		FilterTarget target = new FilterTarget();

		Object[] options = filter.getOptions(new Object[] { "first", "second" }, List.of(1, 2), target);

		assertArrayEquals(new Object[] { "second" }, options);
		assertEquals(List.of(1, 2), target.receivedValues);
	}

	public static final class FilterTarget {
		private List<?> receivedValues;

		public void removeFirstOption(List<Object> keys, List<?> values) {
			receivedValues = values;
			keys.removeFirst();
		}
	}
}
