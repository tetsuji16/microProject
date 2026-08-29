/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2012-2019 ProjectLibre, Inc.  (Previous Copyright Holder)
 * Copyright (c) 2026 microProject
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
package com.microproject.application;

import java.util.function.Consumer;


import com.microproject.collaboration.CollaborationSession;
import com.microproject.pm.task.Project;
import com.microproject.session.SaveOptions;

public final class ProjectDocumentWorkflow {
	private ProjectDocumentWorkflow() {
	}

	public interface SaveCallbacks {
		void persistWorkspace(Project project);

		int resolveSaveDecision(Project project, CollaborationSession collaborationSession);

		String chooseSaveAsCopyFileName(Project project);

		void afterSave(Project project, boolean saveAs, boolean fileNameChanged, boolean collaborationEnabled);
	}

	public static SaveOptions prepareSaveOptions(
		final Project project,
		String requestedFileName,
		boolean saveAs,
		boolean local,
		final CollaborationSession collaborationSession,
		final String collaborationUserKey,
		final String sidecarFileName,
		final SaveCallbacks callbacks) {
		if (project == null) {
			return null;
		}

		boolean collaborationEnabled = collaborationSession != null;
		if (collaborationEnabled && callbacks != null) {
			callbacks.persistWorkspace(project);
			int saveDecision = callbacks.resolveSaveDecision(project, collaborationSession);
			if (saveDecision == CollaborationSession.SAVE_CANCEL) {
				return null;
			}
			if (saveDecision == CollaborationSession.SAVE_AS_COPY) {
				String copyFileName = callbacks.chooseSaveAsCopyFileName(project);
				if (copyFileName == null) {
					return null;
				}
				requestedFileName = copyFileName;
				saveAs = true;
			}
		}

		final boolean fileNameChanged = isDifferent(project.getFileName(), requestedFileName);
		final boolean finalSaveAs = saveAs;
		SaveOptions options = new SaveOptions();
		options.setLocal(local);
		options.setSaveAs(saveAs);
		options.setFileName(requestedFileName);
		ProjectFilePolicies.configureSaveOptions(options, requestedFileName);
		if (callbacks != null) {
			options.setPostSaving(new Consumer<Object>() { public void accept(Object arg0) {
					callbacks.afterSave(project, finalSaveAs, fileNameChanged, collaborationEnabled);
				}
			});
		}
		if (collaborationEnabled) {
			options.setCollaborationEnabled(true);
			options.setCollaborationUserKey(collaborationUserKey);
			options.setSidecarFileName(sidecarFileName);
		}
		return options;
	}

	private static boolean isDifferent(String left, String right) {
		if (left == null) {
			return right != null;
		}
		return !left.equals(right);
	}
}
