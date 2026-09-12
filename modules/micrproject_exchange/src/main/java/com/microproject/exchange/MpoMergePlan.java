/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.exchange;

import java.util.List;
import java.util.Objects;

/** Immutable plan produced before a writer mutates a project or archive. */
public record MpoMergePlan(MpoArchiveSnapshot.MspProjection mspProjection,
		MpoArchiveSnapshot.MpofMetadata metadata, List<Operation> operations,
		List<Conflict> conflicts) {
	public MpoMergePlan {
		mspProjection = Objects.requireNonNull(mspProjection, "mspProjection");
		metadata = Objects.requireNonNull(metadata, "metadata");
		operations = List.copyOf(Objects.requireNonNull(operations, "operations"));
		conflicts = List.copyOf(Objects.requireNonNull(conflicts, "conflicts"));
	}

	public record Operation(String entryName, Action action) {
		public Operation {
			entryName = Objects.requireNonNull(entryName, "entryName");
			action = Objects.requireNonNull(action, "action");
		}
	}

	public enum Action {
		ADD, UPDATE, REMOVE, KEEP
	}

	public record Conflict(String entryName, String reason) {
		public Conflict {
			entryName = Objects.requireNonNull(entryName, "entryName");
			reason = Objects.requireNonNull(reason, "reason");
		}
	}
}
