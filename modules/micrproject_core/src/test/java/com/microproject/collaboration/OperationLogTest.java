package com.microproject.collaboration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class OperationLogTest {
	private static final String DOCUMENT = "00000000-0000-0000-0000-000000000001";
	private static final String FIRST = "00000000-0000-0000-0000-000000000002";
	private static final String NEXT = "00000000-0000-0000-0000-000000000003";
	private static final String ACTOR_A = "00000000-0000-0000-0000-000000000004";
	private static final String ACTOR_B = "00000000-0000-0000-0000-000000000005";
	private static final String ENTITY = "00000000-0000-0000-0000-000000000006";
	private static final String MISSING = "00000000-0000-0000-0000-000000000007";

	@Test void mergeIsIdempotentAndWaitsForMissingParents() {
		OperationLog log = new OperationLog();
		OperationLog.Operation first = new OperationLog.Operation(FIRST, ACTOR_A, 1, Set.of(), "task.update", ENTITY, Map.of());
		OperationLog.Operation next = new OperationLog.Operation(NEXT, ACTOR_B, 1, Set.of(FIRST), "task.update", ENTITY, Map.of());
		assertEquals(List.of(first, next), log.merge(List.of(next, first, first)).ready());
		assertEquals(List.of(next), log.merge(List.of(next)).pending());
	}

	@Test void jsonRoundTripKeepsOperations() throws Exception {
		OperationLog log = new OperationLog();
		OperationLog.Operation op = new OperationLog.Operation(FIRST, ACTOR_A, 1, Set.of(), "task.update", ENTITY, Map.of("name", "A"));
		assertEquals(List.of(op), log.read(log.write(DOCUMENT, List.of(op))));
		assertEquals(Set.of(FIRST), log.readDocument(log.write(DOCUMENT, List.of(op))).appliedOperationIds());
	}

	@Test void jsonRoundTripKeepsPendingOperations() throws Exception {
		OperationLog log = new OperationLog();
		OperationLog.Operation pending = new OperationLog.Operation(NEXT, ACTOR_B, 2, Set.of(MISSING), "task.update", ENTITY, Map.of());
		assertEquals(List.of(pending), log.read(log.write(DOCUMENT, List.of(pending))));
		assertEquals(Set.of(), log.readDocument(log.write(DOCUMENT, List.of(pending))).appliedOperationIds());
	}

	@Test void detectsConcurrentSameFieldUpdatesAndPersistsConflictMetadata() throws Exception {
		OperationLog log = new OperationLog();
		OperationLog.Operation left = new OperationLog.Operation(FIRST, ACTOR_A, 1, Set.of(), "task.update", ENTITY, Map.of("name", "A"));
		OperationLog.Operation right = new OperationLog.Operation(NEXT, ACTOR_B, 1, Set.of(), "task.update", ENTITY, Map.of("name", "B"));
		assertEquals(1, log.merge(List.of(left, right)).conflicts().size());
		String json = new String(log.write(DOCUMENT, List.of(left, right)), java.nio.charset.StandardCharsets.UTF_8);
		org.junit.jupiter.api.Assertions.assertTrue(json.contains("operationIds"));
	}

	@Test void rejectsStaleConflictMetadata() throws Exception {
		String json = "{\"schemaVersion\":1,\"documentId\":\"" + DOCUMENT
			+ "\",\"operations\":[],\"conflicts\":[{\"entityId\":\"" + ENTITY
			+ "\",\"kind\":\"task.update\",\"operationIds\":[\"" + FIRST
			+ "\",\"" + NEXT + "\"]}]}";
		org.junit.jupiter.api.Assertions.assertThrows(java.io.IOException.class,
			() -> new OperationLog().read(json.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
	}

	@Test void rejectsAppliedGenerationThatNamesPendingOperation() throws Exception {
		String json = "{\"schemaVersion\":1,\"documentId\":\"" + DOCUMENT
			+ "\",\"operations\":[],\"conflicts\":[],\"appliedOperationIds\":[\"" + FIRST + "\"]}";
		org.junit.jupiter.api.Assertions.assertThrows(java.io.IOException.class,
			() -> new OperationLog().read(json.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
	}

	@Test void rejectsLogsThatDoNotConformToTheSchema() throws Exception {
		OperationLog log = new OperationLog();
		org.junit.jupiter.api.Assertions.assertThrows(java.io.IOException.class, () -> log.read("{\"schemaVersion\":1,\"documentId\":\"not-a-uuid\",\"operations\":[],\"conflicts\":[]}".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
		org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> log.write("not-a-uuid", List.of()));
	}
}
