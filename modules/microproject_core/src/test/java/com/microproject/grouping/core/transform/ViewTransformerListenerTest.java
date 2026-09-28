/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.grouping.core.transform;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;

import org.junit.jupiter.api.Test;

class ViewTransformerListenerTest {
	@Test
	void dispatchesInReverseRegistrationOrderFromStableSnapshot() {
		ViewTransformer transformer = new ViewTransformer();
		List<String> calls = new ArrayList<>();
		ViewTransformerListener first = event -> calls.add("first");
		ViewTransformerListener second = event -> {
			calls.add("second");
			transformer.removeViewTransformerListener(first);
		};
		transformer.addViewTransformerListener(first);
		transformer.addViewTransformerListener(second);

		assertArrayEquals(new ViewTransformerListener[] { second, first }, transformer.getTimeScaleListeners());
		assertArrayEquals(new ViewTransformerListener[] { second, first },
			transformer.getListeners(ViewTransformerListener.class));
		assertArrayEquals(new EventListener[0], transformer.getListeners(EventListener.class));
		assertThrows(ClassCastException.class, () -> transformer.getListeners(String.class));

		transformer.fireTransformerChanged(transformer);
		transformer.fireTransformerChanged(transformer);

		assertEquals(List.of("second", "first", "second"), calls);
		assertArrayEquals(new ViewTransformerListener[] { second }, transformer.getTimeScaleListeners());
	}
}
