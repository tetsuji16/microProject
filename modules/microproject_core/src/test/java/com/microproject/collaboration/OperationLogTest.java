package com.microproject.collaboration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

	@Test void mergeTreatsEquivalentJsonNumberRepresentationsAsOneOperation() {
		OperationLog log = new OperationLog();
		OperationLog.Operation fromArchive = new OperationLog.Operation(FIRST, ACTOR_A, 1, Set.of(), "assignment.delete", ENTITY,
			Map.of("taskLegacyUniqueId", Integer.valueOf(1), "resourceUniqueId", Integer.valueOf(1)));
		OperationLog.Operation fromMemory = new OperationLog.Operation(FIRST, ACTOR_A, 1, Set.of(), "assignment.delete", ENTITY,
			Map.of("taskLegacyUniqueId", Long.valueOf(1L), "resourceUniqueId", Long.valueOf(1L)));

		assertEquals(List.of(fromArchive), log.merge(List.of(fromArchive, fromMemory)).ready());
	}

	@Test void mergeStillRejectsAReusedIdWithDifferentPayload() {
		OperationLog log = new OperationLog();
		OperationLog.Operation original = new OperationLog.Operation(FIRST, ACTOR_A, 1, Set.of(), "task.update", ENTITY, Map.of("percentComplete", Integer.valueOf(10)));
		OperationLog.Operation conflicting = new OperationLog.Operation(FIRST, ACTOR_A, 1, Set.of(), "task.update", ENTITY, Map.of("percentComplete", Integer.valueOf(98)));

		org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> log.merge(List.of(original, conflicting)));
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

	@Test void jsonlRoundTripRetainsConcurrentConflictRecordAndBothOperations() throws Exception {
		OperationLog log = new OperationLog();
		OperationLog.Operation left = new OperationLog.Operation(FIRST, ACTOR_A, 1, Set.of(), "task.update", ENTITY, Map.of("name", "A"));
		OperationLog.Operation right = new OperationLog.Operation(NEXT, ACTOR_B, 1, Set.of(), "task.update", ENTITY, Map.of("name", "B"));

		byte[] jsonl = log.writeJsonl(DOCUMENT, List.of(left, right));
		String header = new String(jsonl, java.nio.charset.StandardCharsets.UTF_8).lines().findFirst().orElseThrow();
		assertTrue(header.contains("\"conflicts\":[{\"entityId\":\"" + ENTITY + "\""));
		OperationLog.DocumentLog restored = log.readJsonl(jsonl);
		assertEquals(Set.of(FIRST, NEXT), restored.operations().stream().map(OperationLog.Operation::id).collect(java.util.stream.Collectors.toSet()));
		assertEquals(1, log.merge(restored.operations()).conflicts().size());
	}

	@Test void jsonlRoundTripPreservesAppliedGenerationSubset() throws Exception {
		OperationLog log = new OperationLog();
		OperationLog.Operation first = new OperationLog.Operation(FIRST, ACTOR_A, 1, Set.of(), "task.update", ENTITY, Map.of("name", "A"));
		OperationLog.Operation next = new OperationLog.Operation(NEXT, ACTOR_B, 1, Set.of(FIRST), "task.update", ENTITY, Map.of("notes", "B"));

		byte[] jsonl = log.writeJsonl(DOCUMENT, List.of(first, next), Set.of(FIRST));

		assertEquals(Set.of(FIRST), log.readJsonl(jsonl).appliedOperationIds());
	}

	@Test void jsonlWriterRejectsAppliedGenerationWithoutItsCausalParent() {
		OperationLog log = new OperationLog();
		OperationLog.Operation first = new OperationLog.Operation(FIRST, ACTOR_A, 1, Set.of(), "task.update", ENTITY, Map.of());
		OperationLog.Operation next = new OperationLog.Operation(NEXT, ACTOR_A, 2, Set.of(FIRST), "task.update", ENTITY, Map.of());

		org.junit.jupiter.api.Assertions.assertThrows(java.io.IOException.class,
			() -> log.writeJsonl(DOCUMENT, List.of(first, next), Set.of(NEXT)));
	}

	@Test void jsonlReaderRejectsUnknownAppliedOperationId() throws Exception {
		OperationLog log = new OperationLog();
		OperationLog.Operation operation = new OperationLog.Operation(FIRST, ACTOR_A, 1, Set.of(), "task.update", ENTITY, Map.of());
		String[] lines = new String(log.writeJsonl(DOCUMENT, List.of(operation)), java.nio.charset.StandardCharsets.UTF_8).split("\\n", -1);
		com.fasterxml.jackson.databind.node.ObjectNode header = (com.fasterxml.jackson.databind.node.ObjectNode)
			new com.fasterxml.jackson.databind.ObjectMapper().readTree(lines[0]);
		header.putArray("appliedOperationIds").add(MISSING);
		lines[0] = header.toString();

		org.junit.jupiter.api.Assertions.assertThrows(java.io.IOException.class,
			() -> log.readJsonl(String.join("\n", lines).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
	}

	@Test void jsonlReaderRejectsStaleConflictMetadata() throws Exception {
		OperationLog log = new OperationLog();
		OperationLog.Operation left = new OperationLog.Operation(FIRST, ACTOR_A, 1, Set.of(), "task.update", ENTITY, Map.of("name", "A"));
		OperationLog.Operation right = new OperationLog.Operation(NEXT, ACTOR_B, 1, Set.of(), "task.update", ENTITY, Map.of("name", "B"));
		String[] lines = new String(log.writeJsonl(DOCUMENT, List.of(left, right)), java.nio.charset.StandardCharsets.UTF_8).split("\\n", -1);
		com.fasterxml.jackson.databind.node.ObjectNode header = (com.fasterxml.jackson.databind.node.ObjectNode)
			new com.fasterxml.jackson.databind.ObjectMapper().readTree(lines[0]);
		header.putArray("conflicts");
		lines[0] = header.toString();

		org.junit.jupiter.api.Assertions.assertThrows(java.io.IOException.class,
			() -> log.readJsonl(String.join("\n", lines).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
	}

	@Test void jsonlReaderAcceptsLegacyHeaderWithoutConflictMetadata() throws Exception {
		OperationLog.Operation operation = new OperationLog.Operation(FIRST, ACTOR_A, 1, Set.of(), "task.update", ENTITY, Map.of("name", "A"));
		String jsonl = "{\"type\":\"header\",\"schemaVersion\":1,\"documentId\":\"" + DOCUMENT + "\"}\n"
			+ "{\"id\":\"" + FIRST + "\",\"actorId\":\"" + ACTOR_A + "\",\"sequence\":1,\"parents\":[],\"kind\":\"task.update\",\"entityId\":\"" + ENTITY + "\",\"payload\":{\"name\":\"A\"}}\n";

		assertEquals(List.of(operation), new OperationLog().readJsonl(jsonl.getBytes(java.nio.charset.StandardCharsets.UTF_8)).operations());
	}

	@Test void allowsConcurrentUpdatesToDifferentFieldsOfTheSameTask() {
		OperationLog log = new OperationLog();
		OperationLog.Operation rename = new OperationLog.Operation(FIRST, ACTOR_A, 1, Set.of(), "task.update", ENTITY,
				Map.of("legacyUniqueId", 7L, "name", "Renamed"));
		OperationLog.Operation editNotes = new OperationLog.Operation(NEXT, ACTOR_B, 1, Set.of(), "task.update", ENTITY,
				Map.of("legacyUniqueId", 7L, "notes", "Notes"));

		assertEquals(List.of(), log.merge(List.of(rename, editNotes)).conflicts());
	}

	@Test void identicalConcurrentFieldValuesAreNotConflicts() {
		OperationLog log = new OperationLog();
		OperationLog.Operation left = new OperationLog.Operation(FIRST, ACTOR_A, 1, Set.of(), "task.update", ENTITY,
				Map.of("legacyUniqueId", 7L, "name", "Same"));
		OperationLog.Operation right = new OperationLog.Operation(NEXT, ACTOR_B, 1, Set.of(), "task.update", ENTITY,
				Map.of("legacyUniqueId", 7L, "name", "Same"));

		assertEquals(List.of(), log.merge(List.of(left, right)).conflicts());
	}

	@Test void identicalConcurrentDeletesAreIdempotentInsteadOfConflicting() {
		OperationLog log = new OperationLog();
		Map<String, Object> payload = Map.of("legacyUniqueId", 7L);
		OperationLog.Operation left = new OperationLog.Operation(FIRST, ACTOR_A, 1, Set.of(), "task.delete", ENTITY, payload);
		OperationLog.Operation right = new OperationLog.Operation(NEXT, ACTOR_B, 1, Set.of(), "task.delete", ENTITY, payload);

		assertEquals(List.of(), log.merge(List.of(left, right)).conflicts());
	}

	@Test void dependencyLagChangesConflictAcrossLegacyEntityIds() {
		OperationLog log = new OperationLog();
		OperationLog.Operation left = new OperationLog.Operation(FIRST, ACTOR_A, 1, Set.of(), "dependency.add",
				"00000000-0000-0000-0000-000000000016",
				Map.of("predecessorLegacyUniqueId", 11L, "successorLegacyUniqueId", 12L,
						"dependencyType", 0, "lag", 1000L));
		OperationLog.Operation right = new OperationLog.Operation(NEXT, ACTOR_B, 1, Set.of(), "dependency.add",
				"00000000-0000-0000-0000-000000000017",
				Map.of("predecessorLegacyUniqueId", 11L, "successorLegacyUniqueId", 12L,
						"dependencyType", 0, "lag", 2000L));

		assertEquals(1, log.merge(List.of(left, right)).conflicts().size());
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
