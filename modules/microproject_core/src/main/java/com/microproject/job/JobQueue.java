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
package com.microproject.job;

import java.util.function.Consumer;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Map;
import java.lang.reflect.Array;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Supplier;



import com.microproject.util.Environment;
import com.microproject.util.ListenerRegistry;


import com.microproject.util.UiDispatch;

/**
 *
 */
public class JobQueue extends ThreadGroup{
	private static final Logger logger = Logger.getLogger(JobQueue.class.getName());
	public final static int MAX_PROGRESS=10000;
	protected boolean documentBased = false;
	public JobQueue(String name,boolean documentBased) {
		super(name);
		this.documentBased=documentBased;
	}

	public boolean hasNext(){
		return activeCount()!=0;
	}

	/** Returns whether a scheduled job is executing, including work dispatched off this ThreadGroup. */
	public boolean hasExecutingJobs() {
		synchronized (executingJobs) {
			return !executingJobs.isEmpty();
		}
	}
	public synchronized void startNext(){
		if (hasNext()){
			Thread[] threads=new Thread[1];
			if (enumerate(threads)==1){
				threads[0].start();
			}
		}
	}
	public synchronized void cancel(){
		int count=activeCount();
		if (count==0) return;
		Thread[] threads=new Thread[count];
		count=enumerate(threads);
		for (int i=0;i<count;i++){
			if (threads[i] instanceof Job job) job.cancel();
		}
	}

	private final Map<String, AdmissionState> executingJobs = new HashMap<>();

	private static final class AdmissionState {
		private final Job job;
		private int retainedRequests;
		private boolean terminal;

		private AdmissionState(Job job) {
			this.job = job;
		}
	}

	/** Holds an admission key until its UI-side request completion has run. */
	public final class AdmissionTicket implements AutoCloseable {
		private final String key;
		private final Job job;
		private final AtomicBoolean released = new AtomicBoolean();

		private AdmissionTicket(String key, Job job) {
			this.key = key;
			this.job = job;
		}

		public Job getJob() {
			return job;
		}

		@Override
		public void close() {
			if (released.compareAndSet(false, true))
				releaseAdmission(this);
		}
	}

	public void addExecutingJob(Job job) {
		synchronized (executingJobs) {
			executingJobs.putIfAbsent(job.getQueueAdmissionKey(), new AdmissionState(job));
		}
	}
	public void removeExecutingJob(Job job) {
		synchronized (executingJobs) {
			AdmissionState state = executingJobs.get(job.getQueueAdmissionKey());
			if (state == null || state.job != job)
				return;
			state.terminal = true;
			if (state.retainedRequests == 0)
				executingJobs.remove(job.getQueueAdmissionKey(), state);
		}
	}

	public void schedule(Job job){
		trySchedule(job);
	}

	/** Schedules a job unless another live job already occupies its queue name. */
	public boolean trySchedule(Job job){
		return tryScheduleOrCoalesce(job, null);
	}

	/**
	 * Schedules a job by admission key, or attaches a completion listener to the
	 * already active job with that key. Ordinary callers can continue using
	 * {@link #schedule(Job)}; this is for workflows that can share one loaded result.
	 *
	 * @return true when this job was admitted; false when it joined an active job
	 */
	public boolean tryScheduleOrCoalesce(Job job, Consumer<Job> coalescedCompletion) {
		Objects.requireNonNull(job, "job");
		String admissionKey = job.getQueueAdmissionKey();
		Job activeJob;
		synchronized (executingJobs) {
			AdmissionState state = executingJobs.get(admissionKey);
			activeJob = state == null ? null : state.job;
			if (state == null) {
				executingJobs.put(admissionKey, new AdmissionState(job));
			}
		}
		if (activeJob != null) {
			logger.fine(() -> "Coalescing duplicate active job request: " + admissionKey);
			if (coalescedCompletion != null)
				activeJob.addCompletionRunnable(() -> coalescedCompletion.accept(activeJob));
			return false;
		}
		// A queued load can own the critical section while a user presses Save.
		// Starting the save synchronously from the EDT would then wait for that
		// load and freeze the document window.  Defer only the queue admission;
		// Job still serializes its work through the same critical section and
		// retains the ordinary Swing completion callbacks.
		if (UiDispatch.isDispatchThread()) {
			Thread scheduler = new Thread(job::execute, job.getName() + "_scheduler");
			scheduler.setDaemon(job.isDaemon());
			scheduler.start();
		} else {
			job.execute();
		}
		return true;
	}

	/**
	 * Schedules a new job or joins the active job while retaining its admission
	 * key until every request has finished its UI-side completion. The callbacks
	 * run before a new job starts, so the admitted owner can register its terminal
	 * listener without racing a fast job.
	 */
	public void scheduleOrJoinRetained(Job job, Consumer<AdmissionTicket> admitted,
			BiConsumer<Job, AdmissionTicket> coalesced) {
		scheduleOrJoinRetained(job, job::getQueueAdmissionKey, admitted, coalesced);
	}

	/** Resolves the canonical admission key away from the EDT before reserving it. */
	public void scheduleOrJoinRetained(Job job, Supplier<String> admissionKeyResolver,
			Consumer<AdmissionTicket> admitted, BiConsumer<Job, AdmissionTicket> coalesced) {
		Objects.requireNonNull(job, "job");
		Objects.requireNonNull(admissionKeyResolver, "admissionKeyResolver");
		Objects.requireNonNull(admitted, "admitted");
		Objects.requireNonNull(coalesced, "coalesced");
		Runnable resolveAndSchedule = () -> {
			String key;
			try {
				key = admissionKeyResolver.get();
			} catch (RuntimeException | Error e) {
				logger.log(Level.WARNING, "Could not resolve canonical admission key for " + job.getName(), e);
				key = job.getQueueAdmissionKey();
			}
			job.setQueueAdmissionKey(key);
			reserveAndSchedule(job, key, admitted, coalesced);
		};
		if (UiDispatch.isDispatchThread()) {
			Thread resolver = new Thread(resolveAndSchedule, job.getName() + "_admission");
			resolver.setDaemon(job.isDaemon());
			resolver.start();
		} else {
			resolveAndSchedule.run();
		}
	}

	private void reserveAndSchedule(Job job, String key, Consumer<AdmissionTicket> admitted,
			BiConsumer<Job, AdmissionTicket> coalesced) {
		AdmissionState state;
		boolean isAdmitted;
		AdmissionTicket ticket;
		synchronized (executingJobs) {
			state = executingJobs.get(key);
			isAdmitted = state == null;
			if (isAdmitted) {
				state = new AdmissionState(job);
				executingJobs.put(key, state);
			}
			state.retainedRequests++;
			ticket = new AdmissionTicket(key, state.job);
		}
		if (!isAdmitted) {
			AdmissionState activeState = state;
			AdmissionTicket joinedTicket = ticket;
			activeState.job.addCompletionRunnable(() -> {
				try {
					coalesced.accept(activeState.job, joinedTicket);
				} catch (RuntimeException | Error e) {
					joinedTicket.close();
					throw e;
				}
			});
			return;
		}
		try {
			admitted.accept(ticket);
		} catch (RuntimeException | Error e) {
			synchronized (executingJobs) {
				AdmissionState current = executingJobs.get(key);
				if (current == state)
					current.terminal = true;
			}
			ticket.close();
			throw e;
		}
		if (UiDispatch.isDispatchThread()) {
			Thread scheduler = new Thread(job::execute, job.getName() + "_scheduler");
			scheduler.setDaemon(job.isDaemon());
			scheduler.start();
		} else {
			job.execute();
		}
	}

	private void releaseAdmission(AdmissionTicket ticket) {
		synchronized (executingJobs) {
			AdmissionState state = executingJobs.get(ticket.key);
			if (state == null || state.job != ticket.job)
				return;
			if (state.retainedRequests > 0)
				state.retainedRequests--;
			if (state.terminal && state.retainedRequests == 0)
				executingJobs.remove(ticket.key, state);
		}
	}
	public JobProgressMonitor getProgressMonitor(String name, Object component) {
		Object parent = component == null ? getComponent() : component;
		if (parent == null)
			return null;
		return JobQueueUiServices.getProvider().createProgressMonitor(name, parent, 0, MAX_PROGRESS);
	}


	private final ListenerRegistry<JobQueueListener> listeners = new ListenerRegistry<>();

	public void addListener(JobQueueListener l) {
		listeners.add(l);
	}
	public void removeListener(JobQueueListener l) {
		listeners.remove(l);
	}
	public JobQueueListener[] getListeners() {
		return listeners.snapshotReverse().toArray(JobQueueListener[]::new);
	}
    public EventListener[] getListeners(Class listenerType) {
		if (listenerType == null)
			throw new NullPointerException("listenerType");
		if (listenerType != JobQueueListener.class)
			return (EventListener[]) Array.newInstance(listenerType, 0);
		return listeners.snapshotReverse().toArray(JobQueueListener[]::new);
    }

 	protected void fireProgressChanged(Object source,float progress) {
		JobQueueEvent e = null;
		for (JobQueueListener listener : listeners.snapshotReverse()) {
			if (e == null)
				e = new JobQueueEvent(source,progress);
			listener.progressChanged(e);
		}
	}

	protected Object criticalSectionMutex=new Object();

 	protected Job criticalSectionOwner;

 	//for free jobs (queued==false)
	public boolean executeCriticalSectionClosure(Job job,Consumer<Object> c,Object arg) {
		Objects.requireNonNull(job, "job");
		Objects.requireNonNull(c, "closure");
		synchronized (criticalSectionMutex) {
			 if (criticalSectionOwner==job){
				 c.accept(arg);
				 return true;
			 }else{
			 	 logger.fine(job.getName() + " can execute, lost critical section");
				 return false;
			 }
		 }
	}

	/**
	 * Compatibility entry point for callers compiled against the original API.
	 * Interrupted/cancelled acquisition is reported explicitly instead of
	 * silently continuing without ownership.
	 */
	public void beginCriticalSection(Job job){
		if (!tryBeginCriticalSection(job))
			throw new CancellationException("Critical section acquisition cancelled: " + job.getName());
	}

	/**
	 * Attempts to acquire the queue-wide critical section for {@code job}.
	 *
	 * <p>The old implementation ignored interruption while waiting and then
	 * granted ownership anyway.  A cancelled/interrupted MPO load could
	 * therefore acquire the section after the user had moved on, starving the
	 * next save or load.  Cancellation is observed between timed waits as well,
	 * so a normal Job.cancel() does not require an unsafe thread stop.</p>
	 *
	 * @return {@code true} when ownership was acquired, otherwise {@code false}
	 */
	public boolean tryBeginCriticalSection(Job job){
		Objects.requireNonNull(job, "job");
		synchronized (criticalSectionMutex) {
			while (criticalSectionOwner!=null&&criticalSectionOwner.isQueued()){
				if (job.isCanceled())
					return false;
				try {
					criticalSectionMutex.wait(250L);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return false;
				}
			}
			if (job.isCanceled())
				return false;
	 		criticalSectionOwner=job;
			job.logBegin("Critical section");
			return true;
		}
	}

	public void endCriticalSection(Job job){
		Objects.requireNonNull(job, "job");
		synchronized (criticalSectionMutex) {
	 		job.logEnd("Critical section");
			if (criticalSectionOwner==job){
				criticalSectionOwner=null;
				criticalSectionMutex.notifyAll();
			}

		}
 	}
	public Object getComponent() {
		if (!Environment.isVisible())
			return null;
		return JobQueueUiServices.getProvider().getComponent(documentBased);
	}

}
