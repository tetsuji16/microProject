/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.exchange;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable boundary object between an MPOF reader and the validation/merge
 * pipeline.  MSP data and MPOF metadata deliberately have different types so
 * format-specific fields cannot leak into the MSP projection.
 */
public final class MpoArchiveSnapshot {
	private final Map<String, byte[]> entries;
	private final MspProjection mspProjection;
	private final MpofMetadata metadata;

	public MpoArchiveSnapshot(Map<String, byte[]> entries, MspProjection mspProjection,
			MpofMetadata metadata) {
		Objects.requireNonNull(entries, "entries");
		this.entries = copyEntries(entries);
		this.mspProjection = Objects.requireNonNull(mspProjection, "mspProjection");
		this.metadata = Objects.requireNonNull(metadata, "metadata");
	}

	/** Returns a deep copy: callers cannot mutate the reader's snapshot. */
	public Map<String, byte[]> entries() {
		return copyEntries(entries);
	}

	public MspProjection mspProjection() {
		return mspProjection;
	}

	public MpofMetadata metadata() {
		return metadata;
	}

	private static Map<String, byte[]> copyEntries(Map<String, byte[]> source) {
		var copy = new LinkedHashMap<String, byte[]>(source.size());
		source.forEach((name, data) -> copy.put(Objects.requireNonNull(name, "entry name"),
				Objects.requireNonNull(data, "entry data").clone()));
		return Map.copyOf(copy);
	}

	/** The interoperable MSP projection; no MPOF-only fields belong here. */
	public record MspProjection(String projectId, String title, String scheduleXml) {
		public MspProjection {
			projectId = Objects.requireNonNull(projectId, "projectId");
			title = Objects.requireNonNull(title, "title");
			scheduleXml = Objects.requireNonNull(scheduleXml, "scheduleXml");
		}
	}

	/** MPOF-only metadata retained alongside, but not merged into, MSP data. */
	public record MpofMetadata(String formatVersion, String documentId,
			List<String> embeddedProjectIds) {
		public MpofMetadata {
			formatVersion = Objects.requireNonNull(formatVersion, "formatVersion");
			documentId = Objects.requireNonNull(documentId, "documentId");
			embeddedProjectIds = List.copyOf(Objects.requireNonNull(embeddedProjectIds,
				"embeddedProjectIds"));
		}
	}
}
