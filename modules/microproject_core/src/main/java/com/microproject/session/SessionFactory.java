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
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.microproject.job.Job;
import com.microproject.job.JobQueue;

/**
 *
 */
public class SessionFactory {
    private static final Logger logger = Logger.getLogger(SessionFactory.class.getName());
    private static final Map<MethodKey, Method> METHOD_CACHE = new ConcurrentHashMap<>();
    private record MethodKey(Class<?> type, String name, List<Class<?>> argumentTypes) { }
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
	public synchronized Session getSession(boolean local) {
		return getLocalSession();
	}

	private LocalSession ensureLocalSession() {
		if (localSession == null) {
			localSession = new LocalSession();
			localSession.setJobQueue(jobQueue);
		}
		return localSession;
	}

    public static Object call(Object object,String method,Class<?>[] argsDesc, Object[] args) throws Exception{
	    	try {
			return resolveMethod(object, method, argsDesc).invoke(object, args);
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
			return resolveMethod(object, method, argsDesc).invoke(object, args);
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
    
    private static Method resolveMethod(Object object, String name, Class<?>[] argumentTypes)
            throws NoSuchMethodException {
        Class<?>[] types = argumentTypes == null ? new Class<?>[0] : argumentTypes;
        MethodKey key = new MethodKey(object.getClass(), name,
                List.copyOf(Arrays.asList(types)));
        Method cached = METHOD_CACHE.get(key);
        if (cached != null)
            return cached;
        Method resolved = object.getClass().getMethod(name, types);
        Method previous = METHOD_CACHE.putIfAbsent(key, resolved);
        return previous == null ? resolved : previous;
    }

	public synchronized void clearSessions() {
		localSession = null;
	}
    
    private final Map<String, String> credentials = new HashMap<>();
    public synchronized void setCredentials(Map<String, String> credentials){
    	if (credentials!=null){
    		this.credentials.clear();
    		this.credentials.putAll(credentials);
    	}
    }
    public String getLogin() {
		return credentials.get("login");
    	
    }
    public String getServerUrl(){
		return credentials.get("serverUrl");
    }

	public synchronized LocalSession getLocalSession() {
		LocalSession session = ensureLocalSession();
		if (!session.isInitialized()) session.init(credentials);
		return session;
	}
    
	protected JobQueue jobQueue=null;
	public JobQueue getJobQueue() {
		return jobQueue;
	}
	public synchronized void setJobQueue(JobQueue jobQueue) {
		this.jobQueue = jobQueue;
		ensureLocalSession().setJobQueue(jobQueue);
	}
	
	public void schedule(Job job){
    	jobQueue.schedule(job);
    }

    
}
