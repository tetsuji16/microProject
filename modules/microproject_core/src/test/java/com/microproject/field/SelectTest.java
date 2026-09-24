package com.microproject.field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

class SelectTest {

	@Test
	void valueListIncludesNullAndAllOptionsInOrderWhenNullIsAllowed() {
		StaticSelect select = new StaticSelect();
		select.add("first", "First");
		select.add("second", "Second");
		select.setAllowNull(true);

		List<Object> values = select.getValueList();

		assertEquals(3, values.size());
		assertNull(values.getFirst());
		assertEquals(List.of("First", "Second"), values.subList(1, values.size()));
	}

	@Test
	void keyArrayCacheIsInvalidatedWhenAnOptionIsAdded() {
		StaticSelect select = new StaticSelect();
		select.add("first", "First");
		assertEquals(List.of("first"), List.of(select.getKeyArrayWithoutNull()));

		select.add("second", "Second");

		assertEquals(List.of("first", "second"), List.of(select.getKeyArrayWithoutNull()));
	}
}
