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


public class Mutex {
	protected boolean locked=false;
	protected String name;
	
	public Mutex(){
		this("Mutex");
	}
	public Mutex(String name){
		this.name=name;
	}

	public synchronized void waitUntilUnlocked(){
		//System.out.println(name+": wait begin");
		while (locked){
			try {
				wait();
			} catch (InterruptedException e) {
			}
		}
		//System.out.println(name+": wait end");
	}
	
	public synchronized void waitAndLock(){
		//System.out.println(name+": wait begin");
		while (locked){
			try {
				wait();
			} catch (InterruptedException e) {
			}
		}
		locked=true;
		//System.out.println(name+": wait end");
	}
	
	public synchronized void lock(){
		if (!locked){
			locked=true;
			//System.out.println(name+": locked");
		}
	}
	public synchronized void unlock(){
		if (locked){
			locked=false;
			//System.out.println(name+": unlocked");
			notifyAll();
		}
	}

	public synchronized boolean isLocked(){
		return locked;
	}

}
