/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.exchange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MpoArchiveContractTest {
	@Test
	void snapshotDefensivelyCopiesArchiveBytesAndSeparatesProjections() {
		var bytes = new byte[] {1, 2};
		var entries = new HashMap<String, byte[]>();
		entries.put("project.xml", bytes);
		var projection = new MpoArchiveSnapshot.MspProjection("p1", "Plan", "<project/>");
		var metadata = new MpoArchiveSnapshot.MpofMetadata("1", "doc-1", List.of("p2"));
		var snapshot = new MpoArchiveSnapshot(entries, projection, metadata);
		bytes[0] = 9;
		entries.get("project.xml")[1] = 9;

		assertEquals(1, snapshot.entries().get("project.xml")[0]);
		assertEquals(2, snapshot.entries().get("project.xml")[1]);
		assertEquals(projection, snapshot.mspProjection());
		assertEquals(metadata, snapshot.metadata());
		assertThrows(UnsupportedOperationException.class,
				() -> snapshot.metadata().embeddedProjectIds().add("p3"));
	}

	@Test
	void diagnosticAndMergePlanAreStableAndDefensive() {
		var diagnostic = new MpoValidationDiagnostic(MpoValidationDiagnostic.Severity.ERROR,
				"entry-too-large", "decompressed budget exceeded", "project.xml");
		var operations = new ArrayList<MpoMergePlan.Operation>();
		operations.add(new MpoMergePlan.Operation("project.xml", MpoMergePlan.Action.UPDATE));
		var conflicts = new ArrayList<MpoMergePlan.Conflict>();
		conflicts.add(new MpoMergePlan.Conflict("tasks", "changed on disk"));
		var plan = new MpoMergePlan(new MpoArchiveSnapshot.MspProjection("p", "t", "xml"),
				new MpoArchiveSnapshot.MpofMetadata("1", "d", List.of()), operations, conflicts);
		operations.clear();
		conflicts.clear();

		assertEquals("entry-too-large", diagnostic.code());
		assertTrue(plan.operations().contains(new MpoMergePlan.Operation("project.xml",
				MpoMergePlan.Action.UPDATE)));
		assertEquals(1, plan.conflicts().size());
		assertThrows(UnsupportedOperationException.class,
				() -> plan.operations().add(new MpoMergePlan.Operation("x", MpoMergePlan.Action.ADD)));
	}
}
