/*******************************************************************************
 * MIT License
 *
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
		boolean interrupted=false;
		while (locked){
			try {
				wait();
			} catch (InterruptedException e) {
				interrupted=true; // keep waiting, restore the flag once done so the wait does not spin
			}
		}
		if (interrupted)
			Thread.currentThread().interrupt();
	}
	
	public synchronized void waitAndLock(){
		boolean interrupted=false;
		while (locked){
			try {
				wait();
			} catch (InterruptedException e) {
				interrupted=true; // keep waiting, restore the flag once done so the wait does not spin
			}
		}
		locked=true;
		if (interrupted)
			Thread.currentThread().interrupt();
	}
	
	public synchronized void lock(){
		if (!locked){
			locked=true;
		}
	}
	public synchronized void unlock(){
		if (locked){
			locked=false;
			notifyAll();
		}
	}

	public synchronized boolean isLocked(){
		return locked;
	}

}
