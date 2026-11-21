/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.exchange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InvalidClassException;
import java.io.ObjectOutputStream;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.microproject.exchange.relocationfixture.after.Moved;
import com.microproject.util.SafeObjectInput;

class ClassRelocationsTest {
	private static final String BEFORE = "com.microproject.exchange.relocationfixture.before.Moved";
	private static final String AFTER = "com.microproject.exchange.relocationfixture.after.Moved";

	@Test
	void unknownNamesAreReturnedUnchanged() {
		assertEquals(BEFORE, ClassRelocations.relocate(BEFORE));
		assertEquals(BEFORE, PodFormatIdentifiers.remapClassName(BEFORE));
	}

	@Test
	void relocatesTopLevelMemberAndArrayNames() throws Exception {
		try (AutoCloseable ignored = ClassRelocations.overrideForTesting(Map.of(BEFORE, AFTER))) {
			assertEquals(AFTER, ClassRelocations.relocate(BEFORE));
			assertEquals(AFTER + "$Part", ClassRelocations.relocate(BEFORE + "$Part"));
			assertEquals("[[L" + AFTER + ";", ClassRelocations.relocate("[[L" + BEFORE + ";"));
			assertEquals("[I", ClassRelocations.relocate("[I"));
			assertEquals(BEFORE + "Other", ClassRelocations.relocate(BEFORE + "Other"));
		}
		assertEquals(BEFORE, ClassRelocations.relocate(BEFORE));
	}

	@Test
	void legacyPrefixIsReplacedBeforeTheTableIsConsulted() throws Exception {
		String legacy = "com.projectlibre1.exchange.relocationfixture.before.Moved";
		try (AutoCloseable ignored = ClassRelocations.overrideForTesting(Map.of(BEFORE, AFTER))) {
			assertEquals(AFTER, PodFormatIdentifiers.remapClassName(legacy));
			assertEquals("[L" + AFTER + "$Part;", PodFormatIdentifiers.remapClassName("[L" + legacy + "$Part;"));
		}
	}

	@Test
	void readsAStreamWrittenBeforeTheMoveAsTheMovedClass() throws Exception {
		byte[] stream = streamWrittenBeforeMove();
		try (AutoCloseable ignored = ClassRelocations.overrideForTesting(Map.of(BEFORE, AFTER));
				var input = SafeObjectInput.create(new ByteArrayInputStream(stream))) {
			Object[] read = (Object[]) input.readObject();
			Moved moved = assertInstanceOf(Moved.class, read[0]);
			assertEquals("task", moved.name);
			assertEquals(42, moved.part.value);
			Moved[] array = assertInstanceOf(Moved[].class, read[1]);
			assertEquals("second", array[0].name);
		}
	}

	@Test
	void withoutARelocationTheOldClassIsUsed() throws Exception {
		byte[] stream = streamWrittenBeforeMove();
		try (var input = SafeObjectInput.create(new ByteArrayInputStream(stream))) {
			Object[] read = (Object[]) input.readObject();
			assertInstanceOf(com.microproject.exchange.relocationfixture.before.Moved.class, read[0]);
		}
	}

	@Test
	void relocationToAClassWithADifferentSimpleNameIsRejected() throws Exception {
		byte[] stream = streamWrittenBeforeMove();
		try (AutoCloseable ignored = ClassRelocations.overrideForTesting(
				Map.of(BEFORE, "com.microproject.exchange.ClassRelocationsTest"));
				var input = SafeObjectInput.create(new ByteArrayInputStream(stream))) {
			assertThrows(InvalidClassException.class, input::readObject);
		}
	}

	private static byte[] streamWrittenBeforeMove() throws Exception {
		var first = new com.microproject.exchange.relocationfixture.before.Moved();
		first.name = "task";
		first.part = new com.microproject.exchange.relocationfixture.before.Moved.Part();
		first.part.value = 42;
		var second = new com.microproject.exchange.relocationfixture.before.Moved();
		second.name = "second";
		var array = new com.microproject.exchange.relocationfixture.before.Moved[] { second };
		var bytes = new ByteArrayOutputStream();
		try (var output = new ObjectOutputStream(bytes)) {
			output.writeObject(new Object[] { first, array });
		}
		return bytes.toByteArray();
	}
}
