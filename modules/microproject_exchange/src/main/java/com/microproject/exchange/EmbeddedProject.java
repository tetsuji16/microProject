/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.exchange;

record EmbeddedProject(String sourcePath, String entryName, byte[] contents, String referenceId) { }
