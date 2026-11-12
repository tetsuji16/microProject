/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.exchange;

import com.microproject.pm.assignment.Assignment;

record SerializedProjectXml(byte[] xml, java.util.Map<Assignment, Integer> assignmentUids) { }
