/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.collaboration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** Deterministic, idempotent merge of mpo collaboration operations. */
public final class OperationLog {
	private static final ObjectMapper JSON = new ObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION).enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
	public record Operation(String id, String actorId, long sequence, Set<String> parents,
		String kind, String entityId, Map<String, Object> payload) {
		public Operation {
			requireUuid(id, "operation id");
			requireUuid(actorId, "actor id");
			requireUuid(entityId, "entity id");
			if (sequence < 1 || !KINDS.contains(kind)) throw new IllegalArgumentException("Invalid operation");
			parents = Set.copyOf(parents == null ? Set.of() : parents);
			for (String parent : parents) requireUuid(parent, "operation parent");
			payload = Map.copyOf(payload == null ? Map.of() : payload);
		}
	}
	public record Conflict(String entityId, String kind, List<String> operationIds) {
		public Conflict {
			operationIds = List.copyOf(operationIds);
		}
	}
	public record MergeResult(List<Operation> ready, List<Operation> pending, List<Conflict> conflicts) {
		public MergeResult(List<Operation> ready, List<Operation> pending) { this(ready, pending, List.of()); }
	}
	/** Parsed log plus the exact operation IDs known to be applied to the snapshot. */
	public record DocumentLog(String documentId, List<Operation> operations, Set<String> appliedOperationIds) {
		public DocumentLog {
			operations = List.copyOf(operations);
			appliedOperationIds = Set.copyOf(appliedOperationIds);
		}
		/** Compatibility constructor for callers that only need the operation list. */
		public DocumentLog(String documentId, List<Operation> operations) {
			this(documentId, operations, operations.stream().map(Operation::id).collect(java.util.stream.Collectors.toUnmodifiableSet()));
		}
	}
	private static final Set<String> KINDS = Set.of("task.create", "task.update", "task.delete", "task.move",
		"dependency.add", "dependency.delete", "assignment.add", "assignment.delete");
	private static final int MAX_PENDING = 1024;

	public MergeResult merge(Collection<Operation> operations) {
		Map<String, Operation> unique = new LinkedHashMap<>();
		for (Operation operation : operations == null ? List.<Operation>of() : operations) {
			Operation existing = unique.putIfAbsent(operation.id(), operation);
			if (existing != null && !existing.equals(operation)) throw new IllegalArgumentException("Operation ID collision: " + operation.id());
		}
		List<Operation> ordered = new ArrayList<>(unique.values());
		ordered.sort(Comparator.comparingLong(Operation::sequence).thenComparing(Operation::actorId).thenComparing(Operation::id));
		Set<String> applied = new LinkedHashSet<>();
		List<Operation> ready = new ArrayList<>();
		boolean advanced;
		do {
			advanced = false;
			for (Operation operation : ordered) if (!applied.contains(operation.id()) && applied.containsAll(operation.parents())) {
				applied.add(operation.id()); ready.add(operation); advanced = true;
			}
		} while (advanced);
		List<Operation> pending = new ArrayList<>();
		for (Operation operation : ordered) if (!applied.contains(operation.id())) pending.add(operation);
		if (pending.size() > MAX_PENDING) throw new IllegalArgumentException("Too many pending mpo operations");
		List<Conflict> conflicts = detectConflicts(ordered);
		return new MergeResult(List.copyOf(ready), List.copyOf(pending), conflicts);
	}

	private static List<Conflict> detectConflicts(List<Operation> operations) {
		List<Conflict> conflicts = new ArrayList<>();
		Map<String, Operation> byId = new LinkedHashMap<>();
		for (Operation operation : operations) byId.put(operation.id(), operation);
		Map<String, Set<String>> ancestorCache = new LinkedHashMap<>();
		for (int i = 0; i < operations.size(); i++) for (int j = i + 1; j < operations.size(); j++) {
			Operation left = operations.get(i), right = operations.get(j);
			if (!left.entityId().equals(right.entityId()) || left.actorId().equals(right.actorId())) continue;
			if (ancestors(left, byId, ancestorCache).contains(right.id()) || ancestors(right, byId, ancestorCache).contains(left.id())) continue;
			if (!overlaps(left, right)) continue;
			conflicts.add(new Conflict(left.entityId(), left.kind(), List.of(left.id(), right.id())));
		}
		return List.copyOf(conflicts);
	}

	private static Set<String> ancestors(Operation operation, Map<String, Operation> byId, Map<String, Set<String>> cache) {
		Set<String> cached = cache.get(operation.id());
		if (cached != null) return cached;
		Set<String> result = new LinkedHashSet<>();
		java.util.ArrayDeque<String> queue = new java.util.ArrayDeque<>(operation.parents());
		while (!queue.isEmpty()) { String id = queue.removeFirst(); if (!result.add(id)) continue; Operation parent = byId.get(id); if (parent != null) queue.addAll(parent.parents()); }
		Set<String> immutable = Set.copyOf(result);
		cache.put(operation.id(), immutable);
		return immutable;
	}

	private static boolean overlaps(Operation left, Operation right) {
		if (left.kind().endsWith(".delete") || right.kind().endsWith(".delete")) return true;
		if (!left.kind().equals(right.kind())) return true;
		if (left.payload().isEmpty() || right.payload().isEmpty()) return true;
		for (String key : left.payload().keySet()) if (right.payload().containsKey(key)) return true;
		return false;
	}
	public byte[] write(String documentId, Collection<Operation> operations) throws java.io.IOException {
		requireUuid(documentId, "document id");
		ObjectNode root = JSON.createObjectNode(); root.put("schemaVersion", 1); root.put("documentId", documentId);
		ArrayNode values = root.putArray("operations"); root.putArray("conflicts");
		MergeResult merged = merge(operations); List<Operation> all = new ArrayList<>(merged.ready()); all.addAll(merged.pending());
		for (Operation op : all) { ObjectNode value = values.addObject(); value.put("id", op.id()); value.put("actorId", op.actorId()); value.put("sequence", op.sequence()); ArrayNode parents = value.putArray("parents"); op.parents().stream().sorted().forEach(parents::add); value.put("kind", op.kind()); value.put("entityId", op.entityId()); value.set("payload", JSON.valueToTree(op.payload())); }
		ArrayNode conflictValues = (ArrayNode) root.withArray("conflicts");
		for (Conflict conflict : merged.conflicts()) { ObjectNode value = conflictValues.addObject(); value.put("entityId", conflict.entityId()); value.put("kind", conflict.kind()); ArrayNode ids = value.putArray("operationIds"); conflict.operationIds().forEach(ids::add); }
		ArrayNode applied = root.putArray("appliedOperationIds");
		merged.ready().stream().map(Operation::id).sorted().forEach(applied::add);
		return JSON.writeValueAsBytes(root);
	}
	public List<Operation> read(byte[] json) throws java.io.IOException {
		return readDocument(json).operations();
	}
	public DocumentLog readDocument(byte[] json) throws java.io.IOException {
		JsonNode root = JSON.readTree(json);
		if (root == null || !root.isObject() || !root.path("schemaVersion").canConvertToInt() || root.path("schemaVersion").asInt(-1) != 1 || !root.path("operations").isArray() || !root.path("conflicts").isArray()) throw new java.io.IOException("Invalid operation log");
		for (JsonNode conflict : root.path("conflicts")) {
			if (!conflict.isObject() || !conflict.path("entityId").isTextual() || !conflict.path("kind").isTextual() || !conflict.path("operationIds").isArray()) throw new java.io.IOException("Invalid operation conflict");
			if (conflict.path("operationIds").size() < 2) throw new java.io.IOException("Invalid operation conflict members");
		}
		String documentId = text(root, "documentId");
		try { requireUuid(documentId, "document id"); } catch (IllegalArgumentException exception) { throw new java.io.IOException("Invalid operation document id", exception); }
		List<Operation> result = new ArrayList<>(); for (JsonNode value : root.path("operations")) {
			if (!value.isObject() || !value.path("parents").isArray() || !value.path("sequence").isIntegralNumber() || !value.path("sequence").canConvertToLong() || !value.path("payload").isObject()) throw new java.io.IOException("Invalid operation");
			Set<String> parents = new LinkedHashSet<>(); for (JsonNode parent : value.path("parents")) { if (!parent.isTextual() || !parents.add(parent.textValue())) throw new java.io.IOException("Invalid operation parent"); }
			@SuppressWarnings("unchecked") Map<String,Object> payload = JSON.convertValue(value.path("payload"), Map.class);
			try { result.add(new Operation(text(value,"id"), text(value,"actorId"), value.path("sequence").longValue(), parents, text(value,"kind"), text(value,"entityId"), payload)); } catch (IllegalArgumentException exception) { throw new java.io.IOException("Invalid operation", exception); }
		}
		MergeResult merged = merge(result);
		Set<String> expectedConflicts = new LinkedHashSet<>();
		for (Conflict conflict : merged.conflicts()) expectedConflicts.add(conflictKey(conflict));
		Set<String> declaredConflicts = new LinkedHashSet<>();
		for (JsonNode conflict : root.path("conflicts")) {
			List<String> ids = new ArrayList<>();
			for (JsonNode id : conflict.path("operationIds")) ids.add(id.textValue());
			ids.sort(String::compareTo);
			declaredConflicts.add(conflict.path("entityId").textValue() + "|" + conflict.path("kind").textValue() + "|" + String.join(",", ids));
		}
		if (!expectedConflicts.equals(declaredConflicts)) throw new java.io.IOException("Operation conflict metadata does not match operations");
		List<Operation> all = new ArrayList<>(merged.ready()); all.addAll(merged.pending());
		Set<String> appliedIds = new LinkedHashSet<>();
		JsonNode appliedNode = root.get("appliedOperationIds");
		if (appliedNode == null) {
			// Logs written before generation tracking are interpreted conservatively:
			// only causally ready operations are considered applied.
			for (Operation operation : merged.ready()) appliedIds.add(operation.id());
		} else {
			if (!appliedNode.isArray()) throw new java.io.IOException("Invalid applied operation generation");
			for (JsonNode id : appliedNode) {
				if (!id.isTextual() || !result.stream().anyMatch(operation -> operation.id().equals(id.textValue())) || !appliedIds.add(id.textValue()))
					throw new java.io.IOException("Invalid applied operation generation");
			}
			Set<String> readyIds = new LinkedHashSet<>();
			for (Operation operation : merged.ready()) readyIds.add(operation.id());
			if (!readyIds.equals(appliedIds)) throw new java.io.IOException("Applied operation generation does not match causal readiness");
		}
		return new DocumentLog(documentId, List.copyOf(all), appliedIds);
	}
	private static String conflictKey(Conflict conflict) {
		List<String> ids = new ArrayList<>(conflict.operationIds());
		ids.sort(String::compareTo);
		return conflict.entityId() + "|" + conflict.kind() + "|" + String.join(",", ids);
	}
	private static String text(JsonNode value, String field) throws java.io.IOException { JsonNode text = value.get(field); if (text == null || !text.isTextual()) throw new java.io.IOException("Invalid operation " + field); return text.textValue(); }
	private static void requireUuid(String value, String description) { try { UUID.fromString(value); } catch (RuntimeException exception) { throw new IllegalArgumentException("Invalid " + description, exception); } }
}
