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
package com.microproject.pm.scheduling;

import com.microproject.datatype.Duration;
import com.microproject.server.access.ErrorLogger;

public class DelayableImpl implements Delayable, Cloneable {
	private long delay = 0L;
	private long levelingDelay = 0L;
	
	public DelayableImpl(Delayable from) {
		this.delay = from.getDelay();
		this.levelingDelay = from.getLevelingDelay();
	}
	private static final long DELAY_MAX = 1000L*8*60*60*1000L;
	public DelayableImpl(long delay, long levelingDelay) {
		if (delay > DELAY_MAX) {
			ErrorLogger.logOnce("junkDelay","In invalid delay was read in: " + delay,null);
			delay = 0L;
		}
		this.delay = delay;
		this.levelingDelay = levelingDelay;
	}
	
    /**
	 * 
	 */
	public DelayableImpl() {
		delay = 0;
		levelingDelay = 0;
	}

	public long getDelay() {
		return delay;
    }

    public long getLevelingDelay() { 
       return levelingDelay;
    }

	public long calcTotalDelay() {
		return Duration.millis(delay) + Duration.millis(levelingDelay);
	}

	public void setDelay(long delay) {
//		delay = Duration.millis(delay);
//		System.out.println("setting delay" + delay);
		this.delay = delay;
	}

	public void setLevelingDelay(long levelingDelay) {
		this.levelingDelay = levelingDelay;
		
	}
	
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}
	}

}
