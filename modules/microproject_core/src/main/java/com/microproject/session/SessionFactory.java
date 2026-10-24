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
package com.microproject.session;

import java.util.logging.Logger;

import com.microproject.job.Job;
import com.microproject.job.JobQueue;

/**
 *
 */
public class SessionFactory {
    private static final Logger logger = Logger.getLogger(SessionFactory.class.getName());
    protected static SessionFactory instance=null;
    protected SessionFactory() {
    }
    public static synchronized SessionFactory getInstance(){
        if (instance==null) instance=new SessionFactory();
        return instance;
    }
    
	private LocalSession localSession;

	/**
	 * microProject is a local desktop application. The local flag describes
	 * legacy model/ID scope, not a separate server implementation. Both scopes
	 * already resolved to LocalSession in the supported desktop configuration.
	 */
	private LocalSession ensureLocalSession() {
		if (localSession == null) {
			localSession = new LocalSession();
			localSession.setJobQueue(jobQueue);
		}
		return localSession;
	}

	public synchronized void clearSessions() {
		localSession = null;
	}
    
	public synchronized LocalSession getLocalSession() {
		LocalSession session = ensureLocalSession();
		if (!session.isInitialized()) session.init(null);
		return session;
	}
    
	protected JobQueue jobQueue=null;
	public JobQueue getJobQueue() {
		return jobQueue;
	}
	public synchronized void setJobQueue(JobQueue jobQueue) {
		if (jobQueue == null && Boolean.getBoolean("microproject.session.traceQueueLifecycle")) {
			String caller = StackWalker.getInstance().walk(frames -> frames.skip(1).limit(8)
				.map(frame -> frame.getClassName() + "." + frame.getMethodName() + ":" + frame.getLineNumber())
				.collect(java.util.stream.Collectors.joining(" <- ")));
			logger.warning("SESSION_QUEUE_CLEAR factory=" + identity(this) + " localSession="
				+ identity(localSession) + " oldFactoryQueue=" + identity(this.jobQueue)
				+ " oldSessionQueue=" + (localSession == null ? "n/a" : identity(localSession.getJobQueue()))
				+ " caller=" + caller);
		}
		this.jobQueue = jobQueue;
		ensureLocalSession().setJobQueue(jobQueue);
	}

	private static String identity(Object value) {
		return value == null ? "null" : value.getClass().getSimpleName() + "@"
			+ Integer.toHexString(System.identityHashCode(value));
	}
	
	public void schedule(Job job){
    	jobQueue.schedule(job);
    }

    
}
