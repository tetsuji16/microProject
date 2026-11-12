/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.exchange;

record ManifestData(String documentId, Long projectUniqueId, Long statusDate, Long sharedResourcePoolProjectId, String sharedResourcePoolPath,
		java.util.List<EmbeddedProjectReference> embeddedProjects, java.util.Map<String, String> checksums) { }
