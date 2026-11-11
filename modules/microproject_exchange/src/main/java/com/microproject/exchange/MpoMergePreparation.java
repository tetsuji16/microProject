/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.exchange;

import com.microproject.collaboration.OperationLog;
import com.microproject.pm.task.Project;

record MpoMergePreparation(java.util.List<OperationLog.Operation> replayOperations,
		java.util.List<OperationLog.Operation> mergedOperations, java.util.Set<String> alreadyAppliedOperationIds,
		java.util.Set<String> appliedOperationIds,
		MpoExtensions extensions, MpoMergePlan plan) {
	MpoMergePreparation {
		replayOperations = java.util.List.copyOf(replayOperations);
		mergedOperations = java.util.List.copyOf(mergedOperations);
		alreadyAppliedOperationIds = java.util.Set.copyOf(alreadyAppliedOperationIds);
		appliedOperationIds = java.util.Set.copyOf(appliedOperationIds);
	}
	void applyExtensions(Project project) {
		if (extensions == null || extensions.entries.isEmpty()) return;
		MpoExtensions target = project.getOrCreateTransientDocumentState(MpoExtensions.class, MpoExtensions::new);
		for (java.util.Map.Entry<String, byte[]> entry : extensions.entries.entrySet())
			target.entries.putIfAbsent(entry.getKey(), entry.getValue().clone());
	}
}
