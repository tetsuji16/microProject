/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.exchange;

record MpoDocumentMetadata(String documentId, long uniqueId, String name, String sharedResourcePoolFile,
		long sharedResourcePoolProjectId, long statusDate, java.util.Map<String, String> embeddedSources) { }
