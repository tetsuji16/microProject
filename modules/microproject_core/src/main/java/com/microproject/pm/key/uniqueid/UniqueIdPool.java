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
package com.microproject.pm.key.uniqueid;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.microproject.pm.time.MutableInterval;
import com.microproject.session.Session;
import com.microproject.session.SessionFactory;

/**
 *
 */
public class UniqueIdPool {
	private static final Logger logger = Logger.getLogger(UniqueIdPool.class.getName());
	protected static int MIN_SIZE=10;
	protected static int DEFAULT_SIZE=500;
	protected static UniqueIdPool instance;
	
	public static UniqueIdPool getInstance(){
		if (instance==null) instance=new UniqueIdPool();
		return instance;
	}
	
	protected List serverIntervals;
	protected int reservationSem;
	
	protected long lastIdReservation=-1;
	
	protected UniqueIdPool(){
		serverIntervals=new LinkedList();
	}
	
	public synchronized long getId(Session session) throws UniqueIdException{
		int idCount=getIdCount();
		if (serverIntervals.size()==0){
			//if (onlyGlobal){
				try{
					makeServerReservationSync(idCount,session);
				}catch(Exception e){
					logger.log(Level.WARNING, "Failed to reserve unique IDs from server", e);
					throw new UniqueIdException("Server exception");
				}
//			}
//			else makeServerReservationAsync(idCount);
		}
		
		MutableInterval interval;
		long id=-1;
		int size=0;
		synchronized(serverIntervals){
			for (Iterator i=serverIntervals.iterator();i.hasNext();){
				interval=(MutableInterval)i.next();
				if (id==-1){
					id=interval.getStart();
					interval.setStart(id+1);
					if (interval.getStart()>interval.getEnd()){
						i.remove();
						continue;
					}
				}
				size+=interval.getEnd()-interval.getStart()+1;
			}
		}
		if (size<getMinIdCount()) makeServerReservationAsync(idCount-size,session);
//		long r=(id==-1&&!onlyGlobal)?getLocalId():id;
//		return r;
		return id;
	}

	
	
	protected int getIdCount(){
		long t=System.currentTimeMillis();
		if (lastIdReservation!=-1&&t-lastIdReservation<10000) return DEFAULT_SIZE*10;
		return DEFAULT_SIZE;
	}
	protected int getMinIdCount(){
		return MIN_SIZE;
	}
	
	protected void makeServerReservationAsync(final int count,final Session session){
		Thread idBookingThread=new Thread(){
			public void run(){
				synchronized(this){
					if (reservationSem>0) return;
					reservationSem++;
				}
				try {
					makeServerReservation(count,session);
				} catch (Exception e) {
					logger.log(Level.WARNING, "Id cannot be retrieved", e);
				}finally{
					synchronized(this){
						reservationSem--;
					}
				}

			}
		};
		idBookingThread.start();
	}
	
	protected void makeServerReservationSync(final int count,Session session) throws Exception{
		synchronized(this){
			reservationSem++;
		}
		try{
			makeServerReservation(count,session);
		}finally{
			synchronized(this){
				reservationSem--;
			}
		}
	}
	
	protected void makeServerReservation(final int count,Session session) throws Exception{
		logger.fine("ID reservation...");
		lastIdReservation=System.currentTimeMillis();
		MutableInterval interval=(MutableInterval)SessionFactory.call(session,"bookUIDInterval",new Class[]{int.class},new Object[]{count});
		synchronized(serverIntervals){serverIntervals.add(interval);}
		logger.fine("ID reservation, new pool: " + dump());
	}
	
	public String dump(){
		StringBuilder buf = new StringBuilder();
		buf.append('{');
		synchronized(serverIntervals){
			for (Iterator i=serverIntervals.iterator();i.hasNext();){
				MutableInterval interval=(MutableInterval)i.next();
				buf.append('[').append(interval.getStart()).append(',').append(interval.getEnd()).append(']');
				if (i.hasNext()) buf.append(',');
			}
		}
		buf.append('}');
		return buf.toString();
	}
}
