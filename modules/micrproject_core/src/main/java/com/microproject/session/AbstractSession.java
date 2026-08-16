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

import java.util.ArrayList;
import java.util.List;

import com.microproject.company.ApplicationUser;
import com.microproject.job.Job;
import com.microproject.job.JobQueue;
import com.microproject.job.JobRunnable;
import com.microproject.pm.task.Project;

public abstract class AbstractSession implements Session{
	protected JobQueue jobQueue=null;
	public JobQueue getJobQueue() {
		return jobQueue;
	}
	public void setJobQueue(JobQueue jobQueue) {
		this.jobQueue = jobQueue;
	}
	public void schedule(Job job){
    	jobQueue.schedule(job);
    }
	
    protected boolean initialized;
    public boolean isInitialized(){
    	return initialized;
    }
	public void init(Object o){
		initialized=true;
	}

	public ApplicationUser getUser() {
		return null;
	}
	public void setUser(ApplicationUser user){
		
	}
	public void logException(Exception e){}
	public void logString(String s){}

    //public Job getSaveProjectJob(final Project project, final boolean cloneMaster){
    public Job getSaveProjectJob(final Project project, SaveOptions opt){
    	List<Project> projects=new ArrayList();
    	projects.add(project);
    	return getSaveProjectJob(projects,opt);
    }

    public Job getEmptyJob(String name,final Object result){
    	Job job=new Job(jobQueue,name,"Job...",false);
    	job.addRunnable(new JobRunnable(name,0.0f){
    		public Object run() throws Exception{
    			return result;
    		}
    	});
    	return job;
    }


}
