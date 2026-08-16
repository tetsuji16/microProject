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
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;


import com.microproject.server.access.ErrorLogger;
import com.microproject.util.Alert;


/**
 *
 */
public class Job extends Thread {
//	static Log log = LogFactory.getLog(Job.class);

	protected float progress,progressStart;
	protected float weight=0.0f;
	protected boolean canceled=false;
	protected long jobId;
	protected long t;
	protected ExtendedProgressMonitor progressMonitor=null;
	protected Thread monitorChecker=null;
	protected Consumer<Object> cancelMonitorClosure=null;
	protected JobQueue jobQueue;
	protected String title;
	protected boolean showProgess, sync;
	protected List runnables=new ArrayList();
	private final List<Runnable> completionRunnables = new ArrayList<>();
	private boolean completionRunnablesExecuted;
	protected InternalRunnable exceptionHandlerRunnable;
	protected JobMutex mutex;
	protected Mutex globalMutex,groupMutex;
	protected InternalRunnable previousRunnable=null;
	protected boolean queued=true;
	protected boolean customCriticalSection;
	protected Component monitorComponent;

	public Job(JobQueue jobQueue, String name,String title,boolean showProgress) {
		this(jobQueue,name,title,showProgress,null);
	}
	public Job(JobQueue jobQueue, String name,String title,boolean showProgress,Component monitorComponent) {
		super(jobQueue, name);
		this.jobQueue=jobQueue;
		this.title=title;
		this.showProgess=showProgress;
		this.sync=false;
		jobId=System.currentTimeMillis();
		mutex=new JobMutex("JobMutex");
		globalMutex=new Mutex("GlobalMutex");
		groupMutex=new Mutex("GroupMutex");
		progress=0.0f;
		this.monitorComponent=monitorComponent;
	}

    public void log(String s){
//    	System.out.println("Job "+getName()+": "+s);
//		log.info("Job "+getName()+": "+s);
    }
	private final HashMap times=new HashMap<>();
    public void logBegin(String s){
    	log(s+"...");
		times.put(s,Long.valueOf(System.currentTimeMillis()));
    }
    public void logEnd(String s){
    	Long t=(Long)times.get(s);
    	if (t==null) log(s+"...end");
    	else log(s+"...end, "+(System.currentTimeMillis()-t.longValue())+" ms");
    }


	public long getId(){
		return hashCode(); //return super.getId() for jdk1.5
	}


	public JobQueue getJobQueue() {
		return jobQueue;
	}


	public boolean isQueued() {
		return queued;
	}


	public boolean isCustomCriticalSection() {
		return customCriticalSection;
	}


	public void setCustomCriticalSection(boolean customCriticalSection) {
		this.customCriticalSection = customCriticalSection;
	}


	public void setQueued(boolean queued) {
		this.queued = queued;
	}

	public void setCancelMonitorClosure(Consumer<Object> cancelMonitorClosure){
		this.cancelMonitorClosure=cancelMonitorClosure;
	}

	public synchronized float getProgress() {
		return progress;
	}
	public synchronized void setProgress(float runnableProgress,JobRunnable runnable) {
		setProgress(runnableProgress,null,runnable);
	}

	private JobRunnable lastFinishedRunnable=null;
	public synchronized void setProgress(float runnableProgress, final String note,JobRunnable runnable) {
		log("setProgress("+runnableProgress+","+note+","+runnable.getName()+"): progress="+progress+", weight="+runnable.getWeight()+"/"+weight+", lastFinishedRunnable="+((lastFinishedRunnable==null)?null:lastFinishedRunnable.getName())+", progressStart="+progressStart);
		if (canceled||runnable==lastFinishedRunnable) return;
		float relativeProgress=runnableProgress*runnable.getWeight()/getWeight();
		if (progressStart+relativeProgress>progress){
			progress = progressStart+relativeProgress;
			if (showProgess&&progressMonitor!=null){
				SwingUtilities.invokeLater(new Runnable(){
            		public void run(){
         				progressMonitor.setProgress((int)Math.round(getProgress()*JobQueue.MAX_PROGRESS));
         				progressMonitor.setNote(note);

            		}
				});
			}
			if (runnableProgress==1.0f&&runnable!=lastFinishedRunnable){
				lastFinishedRunnable=runnable;
				progressStart+=relativeProgress;
			}
		}
		log("\tsetProgress: progress="+progress+", lastFinishedRunnable="+((lastFinishedRunnable==null)?null:lastFinishedRunnable.getName())+", progressStart="+progressStart);
	}



	public ProgressMonitor getProgressMonitor() {
		return progressMonitor;
	}
	public synchronized void cancel(){
		canceled=true;
		if (showProgess&&progressMonitor!=null){
			SwingUtilities.invokeLater(new Runnable(){
	    		public void run(){
	    			progressMonitor.close();
	    		}
			});
		}
	}
	public synchronized boolean isCanceled(){
		if (progressMonitor!=null&&progressMonitor.isCanceled()) canceled=true;
		return canceled;
	}

	protected void end(){
		//((JobQueue)getThreadGroup()).startNext();
	}

	ListIterator runnableIterator;
	InternalRunnable freeRunnable=null;
	InternalRunnable lastRunnable=null;
	public void execute(){
		boolean asyncExecuting=false;
		try{
			jobQueue.addExecutingJob(this);
			//detection
			runnableIterator=runnables.listIterator();
			boolean beginSync=false;
			boolean endSync=false;
			boolean async=false;
			InternalRunnable r;
			while (runnableIterator.hasNext()){
				r=(InternalRunnable)runnableIterator.next();
				if (r.isExceptionHandler()) continue;
				if (r.isSync()==true){
					beginSync=true;
					lastRunnable=r;
				}
				else{
					runnableIterator.previous();
					break;
				}
			}
			while (runnableIterator.hasNext()){
				r=(InternalRunnable)runnableIterator.next();
				if (r.isExceptionHandler()) continue;
				if (r.isSync()==false){
					async=true;
					freeRunnable=r;
					if (!r.isSwing()) lastRunnable=r;
				}else{
					runnableIterator.previous();
					freeRunnable=null;
					break;
				}
			}
			if (runnableIterator.hasNext()){
				r=(InternalRunnable)runnableIterator.next();
				if (!r.isExceptionHandler()){
					lastRunnable=r;
					endSync=true;
				}
			}

    		if (lastRunnable!=null&&!Job.this.isCustomCriticalSection()) jobQueue.beginCriticalSection(Job.this);
			globalMutex.lock();
			logBegin("global");

			//run
			runnableIterator=runnables.listIterator();
			//valid sequence: sync...sync thread...thread sync...sync
			if (isCanceled()){
				log("Job canceled");
				return;
			}
			if (beginSync){
				if (async) groupMutex.lock(); //not needed
				run(true);
				if (async) groupMutex.waitUntilUnlocked(); //not needed
			}
			if (async){
				if (isCanceled()){
					log("Job canceled");
					return;
				}
				if (endSync) groupMutex.lock();
				asyncExecuting=true;
				start();
				if (endSync){
					groupMutex.waitUntilUnlocked();
					if (isCanceled()){
						log("Job canceled");
						return;
					}
					run(true);
				}
			}
		}finally{
			//mutex.unlock();//just in case an exception occurs
			logEnd("global");
			globalMutex.unlock();
			if (!asyncExecuting) jobQueue.removeExecutingJob(this); //because it's done in run(false);
			if (!asyncExecuting)
				runCompletionRunnables();
		}
	}


	public void run(boolean sync){
		boolean exceptionHandlerExecuted = false;
		try{
			//System.out.println("run("+sync+")...ok");
			final JobQueue jobQueue=getJobQueue();
			//jobQueue.enableComponent(false);
			if (showProgess){
				progressMonitor=jobQueue.getProgressMonitor(title,monitorComponent);
				if (progressMonitor!=null&&cancelMonitorClosure!=null&&monitorChecker==null){
					monitorChecker=new Thread(getName()+"_cancelMonitor"){
						public void run() {
			    			if (isInterrupted() || isCanceled()) // if thread is not alive, do nothing
			    				return;

							while(true){
								if (progressMonitor.isCanceled()){
									cancelMonitorClosure.accept(null);
									break;
								}else if (progressMonitor.isClosed()) break;
								try {
									sleep(100);
								} catch (InterruptedException e) {
									interrupt();
									return;
								}
							}
						}
					};
					monitorChecker.start();
				}
			}

			while (runnableIterator.hasNext()){
				if (isInterrupted() || (isCanceled()
						&& (previousRunnable == null || previousRunnable.getException() == null))){
					log("Job canceled");
					return;
				}
				InternalRunnable runnable=(InternalRunnable)runnableIterator.next();
				if (previousRunnable!=null&&previousRunnable.getException()!=null){//an exception occured
					if (!runnable.isExceptionHandler()) continue;
				}else{
					if (runnable.isExceptionHandler()) continue;
				}
				if (runnable.isSync()!=sync){
					runnableIterator.previous();
					break;
				}

				boolean lock=!sync&&runnable!=freeRunnable;

				log(runnable.runnable.getName()+": lock="+lock);


				JobMutex runMutex;
				if (lock){
					runMutex=mutex;
					runMutex.lock();
				}
				else{
					runMutex=null;
					groupMutex.unlock();
				}
				runnable.setPrevious(previousRunnable);

				runThread(runnable,runMutex); //will unlock mutex
				previousRunnable=runnable;
				if (runMutex!=null){
					if (lock) runMutex.waitUntilUnlocked();
				}
				if (isInterrupted() || (isCanceled() && runnable.getException() == null)){
					log("Job canceled");
					return;
				}
				if (runnable.getException()!=null){
					log(runnable.runnable.getName()+": Aborting job, exception="+runnable.getException().getMessage());
//					if (exceptionHandlerRunnable!=null){
//						runMutex=mutex;
//						runMutex.lock();
//						runThread(exceptionHandlerRunnable,runMutex);
//						runMutex.waitUntilUnlocked();
//					}
//					break;
				}
				if (runnable.isExceptionHandler()) {
					exceptionHandlerExecuted = true;
					break;
				}
			}
		}finally{
			boolean terminal = exceptionHandlerExecuted || isInterrupted() || isCanceled() || !runnableIterator.hasNext();
			groupMutex.unlock();
			jobQueue.removeExecutingJob(this);
			if (terminal)
				runCompletionRunnables();
		}
	}
	public void run(){
		run(false);
	}
	private void runThread(final InternalRunnable runnable,final JobMutex runMutex){
		if (runnable.isCreateThread()){
			Thread t=new Thread(){
				public void run(){
			    	runSwing(runnable,runMutex);
				}
			};
			t.setDaemon(isDaemon());
			t.start();
		}else{
	    	runSwing(runnable,runMutex);
		}
	}
	private void runSwing(final InternalRunnable runnable,final JobMutex runMutex){
		if (runnable.isSwing()){
			SwingUtilities.invokeLater(new Runnable(){
		    	public void run(){
		    		try{
							if (isInterrupted() || (isCanceled() && !runnable.isExceptionHandler())) // if thread is not alive, do nothing
		    				return;
		    			logBegin("running "+runnable.runnable.getName());
		    			runnable.run();
		    			//if (runMutex!=null&&runMutex.getException()!=null) cancel();
		    			if (runnable.getException()!=null) cancel();
		    		}finally{
		    			logEnd("running "+runnable.runnable.getName());
		    			if (runMutex!=null) runMutex.unlock();
		    			if (!isCustomCriticalSection()&&(runnable==lastRunnable||runnable.getException()!=null)) jobQueue.endCriticalSection(Job.this);
		    		}
		        }
		    });
		}else{
			if (isInterrupted() || (isCanceled() && !runnable.isExceptionHandler())) // if thread is not alive, do nothing
				return;
    		try{
    			logBegin("running "+runnable.runnable.getName());
    			runnable.run();
    			//if (runnable.getException()!=null) cancel();
    			if (runnable.getException()!=null) cancel();
    		}finally{
    			logEnd("running "+runnable.runnable.getName());
    			if (runMutex!=null) runMutex.unlock();
    			//Error lastRunnable swing in case of exception
    			if (!isCustomCriticalSection()&&(runnable==lastRunnable||runnable.getException()!=null)) jobQueue.endCriticalSection(Job.this);
    		}
		}
	}

	public void warm(final String message,boolean wait){
		final Mutex alertMutex=new Mutex();
		if (wait) alertMutex.lock();
		SwingUtilities.invokeLater(new Runnable(){
	    	public void run(){
	    		Alert.warn(message);
	    		alertMutex.unlock();
	        }
		});
		if (wait) alertMutex.waitUntilUnlocked();
	}
	public void error(final String message,boolean wait){
		final Mutex alertMutex=new Mutex();
		if (wait) alertMutex.lock();
		SwingUtilities.invokeLater(new Runnable(){
	    	public void run(){
	    		Alert.error(message);
	    		alertMutex.unlock();
	        }
		});
		if (wait) alertMutex.waitUntilUnlocked();
	}
	private static class IntResultHolder{
		int result;
	};
	public int confirm(final String message,boolean wait){
		final Mutex alertMutex=new Mutex();
		final IntResultHolder result=new IntResultHolder();
		if (wait) alertMutex.lock();
		SwingUtilities.invokeLater(new Runnable(){
	    	public void run(){
	    		result.result=Alert.confirm(message);
	    		alertMutex.unlock();
	        }
		});
		if (wait) alertMutex.waitUntilUnlocked();
		return result.result;
	}
	private static class BooleanResultHolder{
		boolean result;
	};
	public boolean okCancel(final String message,boolean wait){
		final Mutex alertMutex=new Mutex();
		final BooleanResultHolder result=new BooleanResultHolder();
		if (wait) alertMutex.lock();
		SwingUtilities.invokeLater(new Runnable(){
	    	public void run(){
	    		result.result=Alert.okCancel(message);
	    		alertMutex.unlock();
	        }
		});
		if (wait) alertMutex.waitUntilUnlocked();
		return result.result;
	}

	private static class StringResultHolder{
		String result;
	};
	public String renameProject(final String name,final Set projectNames,boolean wait,final boolean saveAs){
		final Mutex alertMutex=new Mutex();
		final StringResultHolder result=new StringResultHolder();
		if (wait) alertMutex.lock();
		SwingUtilities.invokeLater(new Runnable(){
	    	public void run(){
	    		result.result=Alert.renameProject(name,projectNames,saveAs);
	    		alertMutex.unlock();
	        }
		});
		if (wait) alertMutex.waitUntilUnlocked();
		return result.result;
	}


	protected Object getResult(){
		return (previousRunnable==null)?null:previousRunnable.getResult();
	}
//	public Object getResult(int index){
//		return ((InternalRunnable)runnables.get(index)).getResult();
//	}
//	public Exception getException(){
//		return (previousRunnable==null)?null:previousRunnable.getException();
//	}
//	public Object getException(int index){
//		return ((InternalRunnable)runnables.get(index)).getException();
//	}

	public Object waitResult() throws Exception{
		globalMutex.waitUntilUnlocked();
		if (previousRunnable==null) return null;
		if (previousRunnable.getException()!=null) throw previousRunnable.getException();
		return previousRunnable.getResult();
	}



	public void logDuration(String title){
		long t1=System.currentTimeMillis();
		log(title+": "+(t1-t)+"ms");
		t=t1;
	}

	public void addRunnable(JobRunnable runnable, boolean sync, boolean createThread, boolean swing, boolean exceptionHandler){
		runnables.add(new InternalRunnable(runnable,sync,createThread,swing,exceptionHandler));
		runnable.setJob(this);
		weight+=runnable.getWeight();
	}

	public void addRunnable(JobRunnable runnable, boolean sync){
		runnables.add(new InternalRunnable(runnable,sync,false,false,false));
		runnable.setJob(this);
		weight+=runnable.getWeight();
	}
	public void addSwingRunnable(JobRunnable runnable, boolean sync){
		runnables.add(new InternalRunnable(runnable,sync,false,true,false));
		runnable.setJob(this);
		weight+=runnable.getWeight();
	}
	public void addRunnable(JobRunnable runnable){
		addRunnable(runnable,false,false,false,false);
	}
	public void addSwingRunnable(JobRunnable runnable){
		addRunnable(runnable,false,false,true,false);
	}
	public void addSync(){
		addRunnable(new JobRunnable("Sync: "+getName()){
			public Object run() throws Exception {
				return getResult();
			}
		},true,false,false,false);
	}

	public void addExceptionRunnable(JobRunnable runnable){
		runnables.add(new InternalRunnable(runnable,false,false,true,true));
		weight+=runnable.getWeight();
	}

	/** Registers work that runs exactly once after success, failure, or cancellation. */
	public void addCompletionRunnable(Runnable runnable) {
		boolean runNow;
		synchronized (this) {
			runNow = completionRunnablesExecuted;
			if (!runNow)
				completionRunnables.add(runnable);
		}
		if (runNow)
			runnable.run();
	}

	private void runCompletionRunnables() {
		List<Runnable> callbacks;
		synchronized (this) {
			if (completionRunnablesExecuted)
				return;
			completionRunnablesExecuted = true;
			callbacks = new ArrayList<>(completionRunnables);
			completionRunnables.clear();
		}
		for (Runnable callback : callbacks) {
			try {
				callback.run();
			} catch (RuntimeException e) {
				ErrorLogger.log("Job completion callback failed: " + getName(), e);
			}
		}
	}

	public void addJob(Job job){
		for (Iterator i=job.runnables.iterator();i.hasNext();){
			InternalRunnable runnable=(InternalRunnable)i.next();
			runnable.getRunnable().setJob(this);
			runnables.add(runnable);
			weight+=runnable.getRunnable().getWeight();
		}
	}

	private class InternalRunnable implements Runnable{
		protected boolean sync=false;
		protected boolean createThread=false;
		protected boolean swing=false;
		protected boolean exceptionHandler=false;
		protected JobRunnable runnable;
		protected Object result=null;
		protected Exception exception=null;
		protected InternalRunnable previous=null;


		public InternalRunnable(JobRunnable runnable, boolean sync, boolean createThread,  boolean swing, boolean exceptionHandler) {
			super();
			this.sync=sync;
			this.createThread = createThread;
			this.swing = swing;
			this.exceptionHandler=exceptionHandler;
			this.runnable = runnable;
		}

		public boolean isCreateThread() {
			return createThread;
		}

		public void setCreateThread(boolean createThread) {
			this.createThread = createThread;
		}

		public boolean isSwing() {
			return swing;
		}

		public void setSwing(boolean swing) {
			this.swing = swing;
		}

		public boolean isExceptionHandler() {
			return exceptionHandler;
		}

		public void setExceptionHandler(boolean exceptionHandler) {
			this.exceptionHandler = exceptionHandler;
		}

		public JobRunnable getRunnable() {
			return runnable;
		}

		public void setRunnable(JobRunnable runnable) {
			this.runnable = runnable;
		}



		public boolean isSync() {
			return sync;
		}

		public void setSync(boolean sync) {
			this.sync = sync;
		}

		public void run(){
			try {
				//System.out.println("Start: "+getName());
				result=runnable.run();
				//System.out.println("End: "+getName());
			} catch (Exception e) {
				//System.out.println("Exception: "+getName());
				exception=e;
				if (!(e instanceof JobCanceledException)){
					ErrorLogger.log("Job Exception: " + getName(),e);
				}
			}
		}

		public Exception getException() {
			return exception;
		}

		public Object getResult() {
			return result;
		}


		public Object getPreviousResult(){
			return (previousRunnable==null)?null:previousRunnable.getResult();
		}
		public Exception getPreviousException(){
			return (previousRunnable==null)?null:previousRunnable.getException();
		}

		public InternalRunnable getPrevious() {
			return previous;
		}

		public void setPrevious(InternalRunnable previous) {
			this.previous = previous;
			if (previous!=null){
				runnable.setPreviousResult(previous.getResult());
				runnable.setPreviousException(previous.getException());
			}
		}





	}
	public float getWeight() {
		return weight;
	}

	public void setWeight(float weight) {
		this.weight = weight;
	}



}
