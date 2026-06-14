package com.projectlibre1.application;

import java.io.File;

import com.projectlibre1.collaboration.CollaborationMetadataStore;
import com.projectlibre1.session.LoadOptions;

public final class ProjectLoadWorkflow {
	private ProjectLoadWorkflow() {
	}

	public static LoadOptions prepareLoadOptions(String fileName, boolean localOnlySession, String collaborationUserKey) {
		LoadOptions options = new LoadOptions();
		options.setFileName(fileName);
		options.setLocal(true);
		options.setSync(false);
		options.setCollaborationEnabled(CollaborationMetadataStore.isCollaborationCandidate(fileName));
		options.setCollaborationUserKey(collaborationUserKey);
		if (options.isCollaborationEnabled()) {
			options.setSidecarFileName(CollaborationMetadataStore.buildSidecarFile(new File(fileName)).getAbsolutePath());
		}
		ProjectFilePolicies.configureLoadOptions(options, fileName, localOnlySession);
		return options;
	}
}
