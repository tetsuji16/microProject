package com.microproject.pm.scheduling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ConstraintTypeKindTest {
	@Test
	void kindRoundTripsPersistedCodes() {
		for (ConstraintType.Kind kind : ConstraintType.Kind.values()) {
			assertEquals(kind, ConstraintType.Kind.fromCode(kind.code()));
		}
		assertEquals(ConstraintType.Kind.SNET, ConstraintType.kind(ConstraintType.SNET));
		assertEquals(ConstraintType.FNLT, ConstraintType.code(ConstraintType.Kind.FNLT));
	}

	@Test
	void unknownCodeIsRejectedInsteadOfSilentlyCoercing() {
		assertThrows(IllegalArgumentException.class, () -> ConstraintType.Kind.fromCode(999));
	}
}
