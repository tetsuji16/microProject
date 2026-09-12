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
package com.microproject.pm.graphic.frames;

import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import java.awt.event.KeyEvent;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.Timer;


import com.microproject.ui.shell.AutoSaveControl;
import com.microproject.application.AutoRecoveryStore;
import com.microproject.dialog.UsabilityStrings;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.ProjectFactory;
import com.microproject.pm.ccpm.CriticalChainService;
import com.microproject.session.FileHelper;
import com.microproject.session.LocalSession;
import com.microproject.session.SaveOptions;
import com.microproject.session.SessionFactory;
import com.microproject.job.Job;
import com.microproject.util.PopupDialogSupport;

/** Periodically writes complete, non-destructive recovery snapshots. */
final class AutoRecoveryManager implements AutoSaveControl {
	static final String ENABLED_PREFERENCE = "autoRecovery.enabled";
	static final String INTERVAL_MINUTES_PREFERENCE = "autoRecovery.intervalMinutes";
	static final int DEFAULT_INTERVAL_MINUTES = 5;
	static final int MINIMUM_INTERVAL_MINUTES = 1;
	static final int MAXIMUM_INTERVAL_MINUTES = 24 * 60;
	private static final Logger LOGGER = Logger.getLogger(AutoRecoveryManager.class.getName());

	private final ProjectFactory projectFactory;
	private final GraphicManager graphicManager;
	private final AutoRecoveryStore store;
	private final Preferences preferences;
	private final Set<Long> savesInProgress = ConcurrentHashMap.newKeySet();
	private final ExecutorService recoveryExecutor;
	private final Timer timer;

	AutoRecoveryManager(ProjectFactory projectFactory, GraphicManager graphicManager) {
		this(projectFactory, graphicManager, AutoRecoveryStore.forCurrentUser(),
			Preferences.userNodeForPackage(AutoRecoveryManager.class));
	}

	AutoRecoveryManager(ProjectFactory projectFactory, GraphicManager graphicManager, AutoRecoveryStore store, Preferences preferences) {
		this.projectFactory = projectFactory;
		this.graphicManager = graphicManager;
		this.store = store;
		this.preferences = preferences;
		recoveryExecutor = Executors.newSingleThreadExecutor(task -> {
			Thread thread = new Thread(task, "microProject-auto-recovery");
			thread.setDaemon(true);
			return thread;
		});
		timer = new Timer(recoveryDelayMillis(preferences.getInt(INTERVAL_MINUTES_PREFERENCE,
			DEFAULT_INTERVAL_MINUTES)), event -> saveDirtyProjects());
		timer.setRepeats(true);
		if (isEnabled()) {
			timer.start();
		}
		try {
			store.cleanup(Instant.now(), AutoRecoveryStore.DEFAULT_RETENTION);
		} catch (IOException ex) {
			LOGGER.log(Level.WARNING, "Could not clean old recovery snapshots", ex);
		}
	}

	@Override
	public boolean isEnabled() {
		return preferences.getBoolean(ENABLED_PREFERENCE, true);
	}

	@Override
	public void setEnabled(boolean enabled) {
		preferences.putBoolean(ENABLED_PREFERENCE, enabled);
		if (enabled) {
			timer.restart();
			saveDirtyProjects();
		} else {
			timer.stop();
		}
	}

	void stop() {
		timer.stop();
		// Do not interrupt an export which may currently be replacing an MPO
		// snapshot.  Already queued saves are allowed to finish and the daemon
		// worker cannot keep the application alive during shutdown.
		recoveryExecutor.shutdown();
	}

	@SuppressWarnings("unchecked")
	void saveDirtyProjects() {
		if (!isEnabled()) {
			return;
		}
		Collection<Project> projects = projectFactory.getDirtyProjectList();
		for (Project project : projects) {
			save(project);
		}
	}

	void discard(Project project) {
		if (project == null) {
			return;
		}
		try {
			store.discard(project.getUniqueId());
		} catch (IOException ex) {
			LOGGER.log(Level.WARNING, "Could not discard recovery snapshot", ex);
		}
	}

	boolean offerRecoveryAtStartup() {
		try {
			boolean recovered = false;
			for (AutoRecoveryStore.Entry entry : store.listRecoverable()) {
				// Consume the round before displaying the dialog.  A later choice,
				// including closing the dialog, must not repeat on the next launch.
				store.markOffered(entry.projectId());
				String title = entry.displayName() == null ? UsabilityStrings.text("recovery.untitled") : entry.displayName();
				String message = java.text.MessageFormat.format(UsabilityStrings.text("recovery.prompt"), title, entry.savedAt());
				Object[] options = {
					withMnemonic(UsabilityStrings.text("recovery.recover"), KeyEvent.VK_R),
					withMnemonic(UsabilityStrings.text("recovery.discard"), KeyEvent.VK_D),
					withMnemonic(UsabilityStrings.text("recovery.later"), KeyEvent.VK_L) };
				int choice = PopupDialogSupport.showOptionDialog(graphicManager.getFrame(), message,
						UsabilityStrings.text("recovery.title"), JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
						null, options, options[0], JOptionPane.CLOSED_OPTION);
				if (choice == 0) {
					Project project = graphicManager.loadRecoveryDocument(entry);
					recovered |= project != null;
				} else if (choice == 1) {
					// The recovery round is already consumed.  Keep the candidate
					// untouched so the user can inspect it or recover it manually.
				} else {
					break;
				}
			}
			return recovered;
		} catch (IOException ex) {
			LOGGER.log(Level.WARNING, "Could not inspect recovery snapshots", ex);
			return false;
		}
	}

	/** Converts a user preference to a Swing-safe bounded delay. */
	static int recoveryDelayMillis(int requestedMinutes) {
		int minutes = Math.clamp(requestedMinutes, MINIMUM_INTERVAL_MINUTES, MAXIMUM_INTERVAL_MINUTES);
		return Math.toIntExact(TimeUnit.MINUTES.toMillis(minutes));
	}

	void completeNormalShutdown() {
		try {
			store.discardAll();
		} catch (IOException ex) {
			LOGGER.log(Level.WARNING, "Could not clear recovery state after normal shutdown", ex);
		}
	}

	private static JButton withMnemonic(String label, int keyEvent) {
		JButton button = new JButton(label);
		button.setMnemonic(keyEvent);
		return button;
	}

	private void save(Project project) {
		if (project == null) {
			return;
		}
		long projectId = project.getUniqueId();
		if (!beginSave(projectId)) {
			return;
		}
		try {
			recoveryExecutor.execute(() -> saveOnWorker(project, projectId));
		} catch (RejectedExecutionException ex) {
			// stop() may race with a timer event.  A rejected task must not leave
			// the project permanently claimed for a recovery save.
			completeSave(projectId);
			LOGGER.log(Level.FINE, "Recovery save skipped after shutdown", ex);
		}
	}

	/** Builds and schedules the complete save job away from the Swing EDT. */
	private void saveOnWorker(Project project, long projectId) {
		try {
			String originalFileName = project.getFileName();
			boolean mpoSnapshot = FileHelper.isMpoFile(originalFileName)
					|| new CriticalChainService().requiresMpo(project);
			java.nio.file.Path snapshotPath = store.snapshotPath(projectId, mpoSnapshot);
			SaveOptions options = new SaveOptions();
			options.setLocal(true);
			options.setSaveAs(true);
			options.setRecoverySnapshot(true);
			options.setImporter(mpoSnapshot ? LocalSession.MPO_PROJECT_IMPORTER
					: LocalSession.LOCAL_PROJECT_IMPORTER);
			options.setFileName(snapshotPath.toString());
			options.setPostSaving(new Consumer<Object>() { public void accept(Object ignored) {
					// LocalSession invokes postSaving from a Swing completion runnable.
					// Metadata is file I/O too, so keep it off the EDT (especially for
					// MPO snapshots, whose replacement may otherwise stall repainting).
					recordSnapshotAsync(projectId, project.getName(), originalFileName, snapshotPath);
				}
			});
			Job job = projectFactory.getSaveProjectJob(project, options);
			if (job == null) {
				completeSave(projectId);
				return;
			}
			// Job completion runs once for success, failure, and cancellation.  It
			// is the sole release point for the in-flight claim after scheduling.
			job.addCompletionRunnable(() -> completeSave(projectId));
			SessionFactory.getInstance().getSession(options.isLocal()).schedule(job);
		} catch (RuntimeException | IOException ex) {
			// Covers job construction and queue admission failures.  If admission
			// succeeded, Job's completion callback owns the release; this path is
			// only reached before that callback can run.
			completeSave(projectId);
			LOGGER.log(Level.WARNING, "Could not save recovery snapshot", ex);
		}
	}

	private void recordSnapshotAsync(long projectId, String projectName, String originalFileName,
			java.nio.file.Path snapshotPath) {
		try {
			recoveryExecutor.execute(() -> {
				try {
					store.recordCompletedSnapshot(projectId, projectName, originalFileName, Instant.now(), snapshotPath);
				} catch (IOException ex) {
					LOGGER.log(Level.WARNING, "Could not record recovery snapshot", ex);
				}
			});
		} catch (RejectedExecutionException ex) {
			LOGGER.log(Level.FINE, "Recovery metadata skipped after shutdown", ex);
		}
	}

	/** Atomically claims a project for one in-flight recovery save. */
	boolean beginSave(long projectId) {
		return savesInProgress.add(projectId);
	}

	/** Releases a recovery-save claim after success or failure. */
	void completeSave(long projectId) {
		savesInProgress.remove(projectId);
	}
}
