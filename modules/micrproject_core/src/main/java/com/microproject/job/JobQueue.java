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

import java.awt.Component;
import java.awt.Frame;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.EventListener;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ProgressMonitor;
import javax.swing.event.EventListenerList;


import com.microproject.util.Environment;


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
			if (threads[i] instanceof Job) ((Job)threads[i]).cancel();
		}
	}

	private Set<String> executingJobs=Collections.synchronizedSet(new HashSet<String>());

	public void addExecutingJob(Job job) {
		executingJobs.add(job.getName());
	}
	public void removeExecutingJob(Job job) {
		executingJobs.remove(job.getName());
	}

	public void schedule(Job job){
		if (executingJobs.contains(job.getName())) return;//to avoid double click
		job.execute();
	}


	public ExtendedProgressMonitor getProgressMonitor(String name,Component component){
		if (component==null) component=getComponent();
		if (component==null)
			return null;
		ExtendedProgressMonitor progressMonitor = new ExtendedProgressMonitor(component,
	                name,
	                "", 0, MAX_PROGRESS);
	    progressMonitor.setProgress(0);

	    progressMonitor.setMillisToPopup(0);
	    progressMonitor.setMillisToDecideToPopup(0);
	    //progressMonitor.setMillisToDecideToPopup(2000);
	    return progressMonitor;
	}

	protected void enableComponent(boolean enabled){
		if (getComponent()==null)
			return;
		getComponent().setEnabled(enabled);
	}


	protected EventListenerList queueListenerList = new EventListenerList();

	public void addListener(JobQueueListener l) {
		queueListenerList.add(JobQueueListener.class, l);
	}
	public void removeListener(JobQueueListener l) {
		queueListenerList.remove(JobQueueListener.class, l);
	}
	public JobQueueListener[] getListeners() {
		return (JobQueueListener[]) queueListenerList.getListeners(JobQueueListener.class);
	}
    public EventListener[] getListeners(Class listenerType) {
    	return queueListenerList.getListeners(listenerType);
    }

 	protected void fireProgressChanged(Object source,float progress) {
		Object[] listeners = queueListenerList.getListenerList();
		JobQueueEvent e = null;
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == JobQueueListener.class) {
				if (e == null) {
					e = new JobQueueEvent(source,progress);
				}
				((JobQueueListener) listeners[i + 1]).progressChanged(e);

			}
		}
	}

	protected Object criticalSectionMutex=new Object();

 	protected Job criticalSectionOwner;

 	//for free jobs (queued==false)
	public boolean executeCriticalSectionClosure(Job job,Consumer<Object> c,Object arg) {
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

	public void beginCriticalSection(Job job){
		synchronized (criticalSectionMutex) {
			while (criticalSectionOwner!=null&&criticalSectionOwner.isQueued()){
				try {
					criticalSectionMutex.wait();
				} catch (InterruptedException e) {
				}
			}
	 		criticalSectionOwner=job;
	 		job.logBegin("Critical section");
		}
 	}

 	public void endCriticalSection(Job job){
		synchronized (criticalSectionMutex) {
	 		job.logEnd("Critical section");
			if (criticalSectionOwner==job){
				criticalSectionOwner=null;
				criticalSectionMutex.notify();
			}

		}
 	}

	private static final String GRAPHIC_MANAGER="com.microproject.pm.graphic.frames.GraphicManager";
	public Component getComponent(){
		if (!Environment.isVisible())
			return null;
		String methodName = documentBased ? "getDocumentFrameInstance" : "getFrameInstance";
		try {
		    return (Frame)Class.forName(GRAPHIC_MANAGER).getMethod(methodName, new Class<?>[0]).invoke(null, new Object[0]);
		} catch (Exception e) {
			logger.log(Level.WARNING, "Job queue error", e);
			return null;
		}
	}

}
