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

import java.util.Collection;
import java.util.List;

import com.microproject.company.ApplicationUser;
import com.microproject.job.Job;
import com.microproject.job.JobQueue;
import com.microproject.pm.task.Project;

public interface Session {
	public static final long MASTER = -2;
	public static final String EXPIRED="EXPIRED";
	
	public void init(Object o);
    public JobQueue getJobQueue();
	public void setJobQueue(JobQueue jobQueue);
	public void schedule(Job job);
	public long getId();
	public ApplicationUser getUser() ;
	public void setUser(ApplicationUser user);
	public void logException(Exception e);
	public void logString(String s);
	
	
    public Job getLoadProjectJob(final LoadOptions opt);
    public Job getLoadProjectDescriptorsJob(final boolean includeProjects, final List descriptors, final boolean allowOpenAs);
    public Job getSaveProjectJob(final Project project, final SaveOptions opt);
    public Job getSaveProjectJob(final List<Project> projects, final SaveOptions opt);
    public Job getEmptyJob(String name,Object result);
	
	
//    public Job getLoadProjectJob(final long projectId, final boolean subproject);
//    
//    public Job getSaveProjectJob(final Project project);
//    public Job getSaveProjectJob(final Project project, final boolean cloneMaster);
//    public Job getSaveProjectJob(final List projs,final Consumer<Object> postSaving, final boolean cloneMaster);

    public Job getCloseProjectsJob(final Collection projects);
    
    public boolean isInitialized();
    public boolean projectExists(long id);
}
