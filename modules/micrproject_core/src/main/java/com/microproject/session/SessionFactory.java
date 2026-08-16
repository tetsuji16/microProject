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

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.microproject.job.Job;
import com.microproject.job.JobQueue;
import com.microproject.strings.Messages;
import com.microproject.util.ClassUtils;

/**
 *
 */
public class SessionFactory {
    private static final Logger logger = Logger.getLogger(SessionFactory.class.getName());
    protected static SessionFactory instance=null;
    protected SessionFactory() {
    }
    public static SessionFactory getInstance(){
        if (instance==null) instance=new SessionFactory();
        return instance;
    }
    
    protected Map<String,Session> sessionImpls=null;
    protected void initSessions(){
    	if (sessionImpls==null){
    		sessionImpls=new HashMap<String, Session>();
    		String impls=Messages.getMetaString("SessionImpls");
    		if (impls!=null){
    			StringTokenizer st=new StringTokenizer(impls,";");
    			while (st.hasMoreTokens()) {
					String key = st.nextToken();
					String implClass=Messages.getMetaString(key);
					if (implClass!=null){
						try {
							Session session = ClassUtils.forName(implClass).asSubclass(Session.class)
								.getDeclaredConstructor().newInstance();
			            	//session.init(credentials);
							if (session.getJobQueue()==null) session.setJobQueue(getJobQueue()); //because this method is called before jobQueue is set
			            	sessionImpls.put(key.substring(key.lastIndexOf('.')+1), session);
						} catch (ReflectiveOperationException | ClassCastException e) {
							logger.log(Level.WARNING, "Failed to create session implementation " + implClass, e);
						}
					}
				}
    		}
    	}
    }  	
    protected Session getSession(String name){
    	initSessions();
    	Session session=sessionImpls.get(name);
    	if (session == null && !"local".equals(name)) {
    		session = sessionImpls.get("local");
    	}
    	if (session == null) {
    		throw new IllegalStateException("No session implementation configured for " + name);
    	}
    	if (!session.isInitialized()) session.init(credentials);
    	return session;
    }
    public Session getSession(boolean local){
    	return local?getSession("local"):getSession("server");
    }
    
    public static Object call(Object object,String method,Class<?>[] argsDesc, Object[] args) throws Exception{
	    	try {
	    		//System.out.println("call, "+method+"..."+object.getClass());
			return object.getClass().getMethod(method, argsDesc).invoke(object, args);
		} catch (IllegalArgumentException e) {
			logger.log(Level.WARNING, "Error", e);
		} catch (SecurityException e) {
			logger.log(Level.WARNING, "Error", e);
		} catch (IllegalAccessException e) {
			logger.log(Level.WARNING, "Error", e);
		} catch (InvocationTargetException e) {
			logger.log(Level.WARNING, "Error", e);
		} catch (NoSuchMethodException e) {
			logger.log(Level.WARNING, "Error", e);
		}
		return null;
    }
    public static Object callNoEx(Object object,String method,Class<?>[] argsDesc, Object[] args){
	    	try {
	    		//System.out.println("callNoEx, "+method+"...");
			return object.getClass().getMethod(method, argsDesc).invoke(object, args);
		} catch (IllegalArgumentException e) {
			logger.log(Level.WARNING, "Error", e);
		} catch (SecurityException e) {
			logger.log(Level.WARNING, "Error", e);
		} catch (IllegalAccessException e) {
			logger.log(Level.WARNING, "Error", e);
		} catch (InvocationTargetException e) {
			logger.log(Level.WARNING, "Error", e);
		} catch (NoSuchMethodException e) {
			logger.log(Level.WARNING, "Error", e);
		}
		return null;
    }
    
    public void clearSessions() {
    	sessionImpls = null;
    }
    
    private final Map<String, Object> credentials = new HashMap<>();
    public void setCredentials(Map credentials){
    	if (credentials!=null){
    		this.credentials.clear();
    		this.credentials.putAll(credentials);
    	}
    }
    public String getLogin() {
    	return (String)credentials.get("login");
    	
    }
    public String getServerUrl(){
    	return (String)credentials.get("serverUrl");
    }

    public LocalSession getLocalSession(){
    	return (LocalSession)getSession("local");
    }
    
	protected JobQueue jobQueue=null;
	public JobQueue getJobQueue() {
		return jobQueue;
	}
	public void setJobQueue(JobQueue jobQueue) {
		this.jobQueue = jobQueue;
		if (sessionImpls==null) initSessions();
		for (Session session : sessionImpls.values())
			session.setJobQueue(jobQueue);
	}
	
	public void schedule(Job job){
    	jobQueue.schedule(job);
    }

    
}
