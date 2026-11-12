/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.exchange;

record MpoCapture(SerializedProjectXml serialized, java.util.Map<String, byte[]> entries,
		MpoDocumentMetadata metadata, String fingerprint, MpoOperationState operationState) { }
