/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.exchange;

import com.microproject.collaboration.OperationLog;

record ExternalMpo(OperationLog.DocumentLog document, MpoExtensions extensions, String manifestDocumentId, Long manifestProjectId, byte[] taskIdentities, MpoArchiveSnapshot snapshot) { }
