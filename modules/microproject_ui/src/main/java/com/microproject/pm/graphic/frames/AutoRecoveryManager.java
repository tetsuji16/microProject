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
import com.microproject.ui.util.PopupDialogSupport;

/** Periodically writes complete, non-destructive recovery snapshots. */
final class AutoRecoveryManager implements AutoSaveControl {
	static final String ENABLED_PREFERENCE = "autoRecovery.enabled";
	static final String INTERVAL_MINUTES_PREFERENCE = "autoRecovery.intervalMinutes";
	static final int DEFAULT_INTERVAL_MINUTES = AutoSaveControl.DEFAULT_INTERVAL_MINUTES;
	static final int MINIMUM_INTERVAL_MINUTES = 1;
	static final int MAXIMUM_INTERVAL_MINUTES = 24 * 60;
	private static final Logger LOGGER = Logger.getLogger(AutoRecoveryManager.class.getName());

	private final ProjectFactory projectFactory;
	private final GraphicManager graphicManager;
	private final AutoRecoveryStore store;
	private final Preferences preferences;
	private final Set<Long> savesInProgress = ConcurrentHashMap.newKeySet();
	private final Object publicationLock = new Object();
	private final Object recoveryIoLock = new Object();
	private final java.util.Map<Long, Long> publicationEpochs = new java.util.HashMap<>();
	private boolean normalShutdown;
	private final java.util.Map<Long, java.util.Set<java.nio.file.Path>> ownedSnapshots = new java.util.HashMap<>();
	private final java.util.Map<Long, java.util.Set<AutoRecoveryStore.OfferLease>> recoveredLeases = new java.util.HashMap<>();
	private final String recoverySeries = java.util.UUID.randomUUID().toString();
	private final ExecutorService recoveryExecutor;
	private final Timer timer;

	AutoRecoveryManager(ProjectFactory projectFactory, GraphicManager graphicManager) {
		this(projectFactory, graphicManager, AutoRecoveryStore.forCurrentUser(),
			Preferences.userNodeForPackage(AutoRecoveryManager.class));
	}

	AutoRecoveryManager(ProjectFactory projectFactory, GraphicManager graphicManager, AutoRecoveryStore store, Preferences preferences) {
		this(projectFactory, graphicManager, store, preferences, Executors.newSingleThreadExecutor(task -> {
			Thread thread = new Thread(task, "microProject-auto-recovery");
			thread.setDaemon(true);
			return thread;
		}));
	}

	AutoRecoveryManager(ProjectFactory projectFactory, GraphicManager graphicManager, AutoRecoveryStore store,
			Preferences preferences, ExecutorService executor) {
		this.projectFactory = projectFactory;
		this.graphicManager = graphicManager;
		this.store = store;
		this.preferences = preferences;
		recoveryExecutor = executor;
		timer = new Timer(recoveryDelayMillis(preferences.getInt(INTERVAL_MINUTES_PREFERENCE,
			DEFAULT_INTERVAL_MINUTES)), event -> saveDirtyProjects());
		timer.setRepeats(true);
		if (isEnabled()) {
			timer.start();
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
		if (project != null) discard(project.getUniqueId());
	}

	void discard(long projectId) {
		synchronized (publicationLock) {
			publicationEpochs.merge(projectId, 1L, Long::sum);
			submitCleanup(() -> {
				releaseRecoveredLeases(projectId);
				java.util.Set<java.nio.file.Path> snapshots = ownedSnapshots.remove(projectId);
				if (snapshots != null) for (var snapshot : snapshots) store.discardGeneration(projectId, snapshot);
			});
		}
	}

	@FunctionalInterface
	private interface Cleanup { void run() throws IOException; }

	private void submitCleanup(Cleanup cleanup) {
		try {
			recoveryExecutor.execute(() -> {
				try { synchronized (recoveryIoLock) { cleanup.run(); } }
				catch (IOException ex) { LOGGER.log(Level.WARNING, "Could not clear recovery state", ex); }
			});
		} catch (RejectedExecutionException ex) {
			LOGGER.log(Level.WARNING, "Could not schedule recovery cleanup", ex);
		}
	}

	void offerRecoveryAtStartup(Consumer<Boolean> completed) {
		try {
			recoveryExecutor.execute(() -> {
				try {
					store.cleanupOrphanGenerations(Instant.now(), AutoRecoveryStore.DEFAULT_RETENTION);
					offerNext(new OfferQueue(store), false, completed);
				}
				catch (IOException ex) {
					LOGGER.log(Level.WARNING, "Could not inspect recovery snapshots", ex);
					javax.swing.SwingUtilities.invokeLater(() -> { showRecoveryError(); completed.accept(false); });
				}
			});
		} catch (RejectedExecutionException ex) { javax.swing.SwingUtilities.invokeLater(() -> completed.accept(false)); }
	}

	record OfferCandidate(AutoRecoveryStore.Entry entry, AutoRecoveryStore.OfferLease lease) { }
	/** One startup scan, with bounded refresh when a scanned generation disappeared. */
	static final class OfferQueue {
		private final AutoRecoveryStore store;
		private java.util.Iterator<AutoRecoveryStore.Entry> entries;
		private final java.util.Set<AutoRecoveryStore.Entry> seen = new java.util.HashSet<>();
		private int refreshes;
		OfferQueue(AutoRecoveryStore store) throws IOException {
			this.store = store;
			entries = store.listRecoverable().iterator();
		}
		OfferCandidate next() throws IOException {
			while (entries.hasNext()) {
				var entry = entries.next();
				if (!seen.add(entry)) continue;
				var lease = store.tryOffer(entry);
				if (lease != null) return new OfferCandidate(entry, lease);
				if (!store.isCurrent(entry) && refreshes++ < 32)
					entries = store.listRecoverable().iterator();
			}
			return null;
		}
	}

	private void offerNext(OfferQueue queue, boolean recovered, Consumer<Boolean> completed) {
		try {
			OfferCandidate candidate = queue.next();
			if (candidate != null) {
				AutoRecoveryStore.Entry entry = candidate.entry();
				AutoRecoveryStore.OfferLease lease = candidate.lease();
				javax.swing.SwingUtilities.invokeLater(() -> {
					String title = entry.displayName() == null ? UsabilityStrings.text("recovery.untitled") : entry.displayName();
					String message = java.text.MessageFormat.format(UsabilityStrings.text("recovery.prompt"), title, entry.savedAt());
					Object[] options = { withMnemonic(UsabilityStrings.text("recovery.recover"), KeyEvent.VK_R),
						withMnemonic(UsabilityStrings.text("recovery.discard"), KeyEvent.VK_D),
						withMnemonic(UsabilityStrings.text("recovery.later"), KeyEvent.VK_L) };
					int choice = PopupDialogSupport.showOptionDialog(graphicManager.getFrame(), message,
						UsabilityStrings.text("recovery.title"), JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
						null, options, options[0], JOptionPane.CLOSED_OPTION);
					if (choice == 0) {
						try {
							graphicManager.loadRecoveryDocument(entry, loaded ->
								finishOffer(lease, entry, queue, recovered || loaded, loaded, false, completed));
						} catch (RuntimeException ex) {
							LOGGER.log(Level.WARNING, "Could not start recovery load", ex);
							finishOffer(lease, entry, queue, recovered, false, false, completed);
						}
					} else finishOffer(lease, entry, queue, recovered, choice == 1, choice == 1, completed);
				});
				return;
			}
		} catch (IOException ex) {
			LOGGER.log(Level.WARNING, "Could not claim recovery snapshot", ex);
			javax.swing.SwingUtilities.invokeLater(() -> { showRecoveryError(); completed.accept(recovered); });
			return;
		}
		javax.swing.SwingUtilities.invokeLater(() -> completed.accept(recovered));
	}

	private void finishOffer(AutoRecoveryStore.OfferLease lease, AutoRecoveryStore.Entry entry,
			OfferQueue queue,
			boolean recovered, boolean confirmed, boolean discard, Consumer<Boolean> completed) {
		try {
			recoveryExecutor.execute(() -> {
				try {
					if (confirmed && !discard) {
						synchronized (recoveryIoLock) {
							ownedSnapshots.computeIfAbsent(entry.projectId(), ignored -> new java.util.HashSet<>()).add(entry.snapshot());
							recoveredLeases.computeIfAbsent(entry.projectId(), ignored -> new java.util.HashSet<>()).add(lease);
						}
					} else try (lease) { if (discard) lease.complete(); }
				}
				catch (IOException ex) {
					LOGGER.log(Level.WARNING, "Could not complete recovery offer", ex);
					javax.swing.SwingUtilities.invokeLater(() -> { showRecoveryError(); completed.accept(recovered); });
					return;
				}
				if (confirmed) offerNext(queue, recovered, completed);
				else javax.swing.SwingUtilities.invokeLater(() -> completed.accept(recovered));
			});
		} catch (RejectedExecutionException ex) {
			Thread cleanup = new Thread(() -> {
				try { lease.close(); } catch (IOException closeError) { LOGGER.log(Level.WARNING, "Could not release recovery lease", closeError); }
			}, "microProject-recovery-lease-cleanup");
			cleanup.setDaemon(true);
			cleanup.start();
			completed.accept(recovered);
		}
	}

	private void releaseRecoveredLeases(long projectId) throws IOException {
		var leases = recoveredLeases.remove(projectId);
		if (leases != null) for (var lease : leases) lease.close();
	}

	private void showRecoveryError() {
		JOptionPane.showMessageDialog(graphicManager.getFrame(), UsabilityStrings.text("recovery.error"),
			UsabilityStrings.text("recovery.title"), JOptionPane.WARNING_MESSAGE);
	}

	@Override
	public int getIntervalMinutes() {
		return Math.clamp(preferences.getInt(INTERVAL_MINUTES_PREFERENCE, DEFAULT_INTERVAL_MINUTES),
			MINIMUM_INTERVAL_MINUTES, MAXIMUM_INTERVAL_MINUTES);
	}

	@Override
	public void setIntervalMinutes(int requestedMinutes) {
		int minutes = Math.clamp(requestedMinutes, MINIMUM_INTERVAL_MINUTES, MAXIMUM_INTERVAL_MINUTES);
		preferences.putInt(INTERVAL_MINUTES_PREFERENCE, minutes);
		int delayMillis = recoveryDelayMillis(minutes);
		timer.setDelay(delayMillis);
		timer.setInitialDelay(delayMillis);
		if (isEnabled()) {
			timer.restart();
		}
	}

	/** Converts a user preference to a Swing-safe bounded delay. */
	static int recoveryDelayMillis(int requestedMinutes) {
		int minutes = Math.clamp(requestedMinutes, MINIMUM_INTERVAL_MINUTES, MAXIMUM_INTERVAL_MINUTES);
		return Math.toIntExact(TimeUnit.MINUTES.toMillis(minutes));
	}

	void completeNormalShutdown() {
		synchronized (publicationLock) { normalShutdown = true; }
		// Existing quit callers perform this on their completion worker. Wait for
		// any publication already in progress before removing recovery metadata.
		try {
			synchronized (recoveryIoLock) {
				for (long projectId : java.util.List.copyOf(recoveredLeases.keySet())) releaseRecoveredLeases(projectId);
				for (var owned : ownedSnapshots.entrySet())
					for (var snapshot : owned.getValue()) store.discardGeneration(owned.getKey(), snapshot);
				ownedSnapshots.clear();
			}
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
		long epoch;
		synchronized (publicationLock) {
			if (normalShutdown || !beginSave(projectId)) return;
			epoch = publicationEpochs.getOrDefault(projectId, 0L);
		}
		try {
			recoveryExecutor.execute(() -> saveOnWorker(project, projectId, epoch));
		} catch (RejectedExecutionException ex) {
			// stop() may race with a timer event.  A rejected task must not leave
			// the project permanently claimed for a recovery save.
			completeSave(projectId);
			LOGGER.log(Level.FINE, "Recovery save skipped after shutdown", ex);
		}
	}

	/** Builds and schedules the complete save job away from the Swing EDT. */
	private void saveOnWorker(Project project, long projectId, long epoch) {
		java.util.concurrent.atomic.AtomicBoolean released = new java.util.concurrent.atomic.AtomicBoolean();
		java.util.concurrent.atomic.AtomicBoolean publicationStarted = new java.util.concurrent.atomic.AtomicBoolean();
		Runnable release = () -> { if (released.compareAndSet(false, true)) completeSave(projectId); };
		try {
			String originalFileName = project.getFileName();
			boolean mpoSnapshot = FileHelper.isMpoFile(originalFileName)
					|| new CriticalChainService().requiresMpo(project);
			java.nio.file.Path snapshotPath = store.newSnapshotPath(projectId, mpoSnapshot);
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
					String projectName = project.getName();
					publicationStarted.set(true);
					recordSnapshotAsync(projectId, projectName, originalFileName, snapshotPath, epoch, release);
				}
			});
			// Building the job updates project dates; never interleave that with an
			// in-flight EDT model command on this worker thread.
			Job job = com.microproject.transaction.ProjectMutationState.coordinated(
					() -> projectFactory.getSaveProjectJob(project, options));
			if (job == null) {
				release.run();
				return;
			}
			// A successful job keeps the claim until its metadata is published.
			// The post-saving callback schedules publication before job completion.
			job.addCompletionRunnable(() -> {
				if (!publicationStarted.get() && (job.isCanceled() || job.isInterrupted() || job.getFailureException() != null))
					release.run();
			});
			SessionFactory.getInstance().getLocalSession().schedule(job);
		} catch (RuntimeException | IOException ex) {
			// Covers job construction and queue admission failures.  If admission
			// succeeded, Job's completion callback owns the release; this path is
			// only reached before that callback can run.
			release.run();
			LOGGER.log(Level.WARNING, "Could not save recovery snapshot", ex);
		}
	}

	void recordSnapshotAsync(long projectId, String projectName, String originalFileName,
			java.nio.file.Path snapshotPath, long epoch, Runnable release) {
		try {
			recoveryExecutor.execute(() -> {
				try {
					synchronized (recoveryIoLock) {
						boolean current;
						synchronized (publicationLock) {
							current = !normalShutdown && publicationEpochs.getOrDefault(projectId, 0L) == epoch;
						}
						if (current) {
							store.recordCompletedSnapshot(projectId, projectName, originalFileName, Instant.now(), snapshotPath, recoverySeries);
							ownedSnapshots.computeIfAbsent(projectId, ignored -> new java.util.HashSet<>()).add(snapshotPath);
						} else {
							java.nio.file.Files.deleteIfExists(snapshotPath);
						}
					}
				} catch (IOException ex) {
					LOGGER.log(Level.WARNING, "Could not record recovery snapshot", ex);
				} finally {
					release.run();
				}
			});
		} catch (RuntimeException ex) {
			// The callback can arrive after shutdown. Do not leave its unadvertised
			// snapshot behind, and never perform deletion on the Swing callback.
			Thread cleanup = new Thread(() -> {
				try { java.nio.file.Files.deleteIfExists(snapshotPath); }
				catch (IOException failure) { LOGGER.log(Level.WARNING, "Could not remove unpublished snapshot", failure); }
				finally { release.run(); }
			}, "microProject-recovery-late-cleanup");
			cleanup.setDaemon(true);
			cleanup.start();
			LOGGER.log(Level.WARNING, "Could not schedule recovery metadata", ex);
		}
	}

	/** Atomically claims a project for one in-flight recovery save. */
	boolean beginSave(long projectId) {
		return savesInProgress.add(projectId);
	}

	/** Releases a recovery-save claim after success or failure. */
	void completeSave(long projectId) {
		synchronized (publicationLock) {
			savesInProgress.remove(projectId);
			publicationLock.notifyAll();
		}
	}
}
