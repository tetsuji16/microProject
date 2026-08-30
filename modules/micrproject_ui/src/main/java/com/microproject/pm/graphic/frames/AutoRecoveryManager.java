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
import java.util.function.Consumer;
import java.util.HashSet;
import java.util.Set;
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
import com.microproject.session.LocalSession;
import com.microproject.session.SaveOptions;
import com.microproject.util.PopupDialogSupport;

/** Periodically writes complete, non-destructive recovery snapshots. */
final class AutoRecoveryManager implements AutoSaveControl {
	static final String ENABLED_PREFERENCE = "autoRecovery.enabled";
	static final String INTERVAL_MINUTES_PREFERENCE = "autoRecovery.intervalMinutes";
	static final int DEFAULT_INTERVAL_MINUTES = 5;
	static final int MINIMUM_INTERVAL_MINUTES = 1;
	private static final Logger LOGGER = Logger.getLogger(AutoRecoveryManager.class.getName());

	private final ProjectFactory projectFactory;
	private final GraphicManager graphicManager;
	private final AutoRecoveryStore store;
	private final Preferences preferences;
	private final Set<Long> savesInProgress = new HashSet<>();
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
		int intervalMinutes = Math.max(MINIMUM_INTERVAL_MINUTES,
			preferences.getInt(INTERVAL_MINUTES_PREFERENCE, DEFAULT_INTERVAL_MINUTES));
		timer = new Timer((int) TimeUnit.MINUTES.toMillis(intervalMinutes), event -> saveDirtyProjects());
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
					store.discard(entry.projectId());
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

	private static JButton withMnemonic(String label, int keyEvent) {
		JButton button = new JButton(label);
		button.setMnemonic(keyEvent);
		return button;
	}

	private void save(Project project) {
		long projectId = project.getUniqueId();
		if (!savesInProgress.add(projectId)) {
			return;
		}
		try {
			String originalFileName = project.getFileName();
			SaveOptions options = new SaveOptions();
			options.setLocal(true);
			options.setSaveAs(true);
			options.setRecoverySnapshot(true);
			options.setImporter(LocalSession.LOCAL_PROJECT_IMPORTER);
			options.setFileName(store.snapshotPath(projectId).toString());
			options.setPostSaving(new Consumer<Object>() { public void accept(Object ignored) {
					try {
						store.recordCompletedSnapshot(projectId, project.getName(), originalFileName, Instant.now());
					} catch (IOException ex) {
						LOGGER.log(Level.WARNING, "Could not record recovery snapshot", ex);
					} finally {
						savesInProgress.remove(projectId);
					}
				}
			});
			projectFactory.saveProject(project, options);
		} catch (RuntimeException | IOException ex) {
			savesInProgress.remove(projectId);
			LOGGER.log(Level.WARNING, "Could not save recovery snapshot", ex);
		}
	}
}
