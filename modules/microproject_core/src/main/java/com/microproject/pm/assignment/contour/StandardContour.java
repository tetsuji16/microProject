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
package com.microproject.pm.assignment.contour;

import java.util.ArrayList;

import com.microproject.configuration.Configuration;
import com.microproject.pm.time.ImmutableInterval;
import com.microproject.pm.time.MutableInterval;

/**
 * A standard contour represents a work distribution.  There are several predined ones, which correspond to a stepping function.
 * Because the function is the same for each type, instances of this class are final static. 
 * @stereotype strategy
 */
public class StandardContour extends AbstractContour implements ContourTypes {
	private double meanUnits = 0.0;
	private int type;
	
	public static StandardContour getInstance(int type, AbstractContourBucket[] contourBuckets) {
		return new StandardContour(type, contourBuckets);
	}
	public boolean isPersonal() {return false;}	
	/**
	 * @return Returns the meanUnits.
	 */
	public double getMeanUnits() {
		return meanUnits;
	}	
	
	public String getName() {
		return Configuration.getInstance().getFieldDictionary().getFieldFromId("Field.workContour").convertIdToString(Integer.valueOf(type));
	}

	
	public long calcTotalWork(long assignmentDuration) {
		return (long) (meanUnits * assignmentDuration);
	}
	
    private StandardContour(int type, AbstractContourBucket[] contourBuckets) {
    	super(contourBuckets);
    	this.type = type;
    	meanUnits = calcWeightedSum();
    }
    
    private double calcWeightedSum() {
    	double sum = 0;
    	for (int i=0; i < contourBuckets.length; i++)
    		sum += contourBuckets[i].weightedSum();
    	return sum;
    }

    public static final StandardContour FLAT_CONTOUR = getInstance(ContourTypes.FLAT, new StandardContourBucket[] { // mean is 1.0
					  new StandardContourBucket(1.0, 1.0)
	});
 
    public static final StandardContour BACK_LOADED_CONTOUR = getInstance(ContourTypes.BACK_LOADED, new StandardContourBucket[] { // mean is 0.6
					  		new StandardContourBucket(0.1, 0.1), // 10% charge for first 10%
					  		new StandardContourBucket(0.15, 0.1), // 15% charge for next 10%
							new StandardContourBucket(0.25, 0.1), // 25% charge for next 10%
							new StandardContourBucket(0.5, 0.2), // 50% charge for next 20%
							new StandardContourBucket(0.75, 0.2), // 75% charge for next 20%
							new StandardContourBucket(1.0, 0.3) // 100% charge last 30%
	});

    public static final StandardContour FRONT_LOADED_CONTOUR = getInstance(ContourTypes.FRONT_LOADED, new StandardContourBucket[] { // mean is 0.6
					  		new StandardContourBucket(1.0, 0.3), // 100% charge first 30%
					  		new StandardContourBucket(0.75, 0.2), // 75% charge for next 20%
							new StandardContourBucket(0.5, 0.2), // 50% charge for next 20%
							new StandardContourBucket(0.25, 0.1), // 25% charge for next 10%
							new StandardContourBucket(0.15, 0.1), // 15% charge for next 10%
							new StandardContourBucket(0.1, 0.1) // 10% charge for last 10%					  
	});

    public static final StandardContour DOUBLE_PEAK_CONTOUR = getInstance(ContourTypes.DOUBLE_PEAK, new StandardContourBucket[] { // mean is 0.5
					  		new StandardContourBucket(0.25, 0.1), // 25% charge first 10%
					  		new StandardContourBucket(0.5, 0.1), // 50% charge for next 10%
							new StandardContourBucket(1.0, 0.1), // 100% charge for next 10%
							new StandardContourBucket(0.5, 0.1), // 50% charge for next 10%			
							new StandardContourBucket(0.25, 0.2), // 25% charge next 20%			
							new StandardContourBucket(0.5, 0.1), // 50% charge for next 10%
							new StandardContourBucket(1.0, 0.1), // 100% charge for next 10%
							new StandardContourBucket(0.5, 0.1), // 50% charge for next 10%			
							new StandardContourBucket(0.25, 0.1), // 25% charge last 10%			
	});
    
    public static final StandardContour EARLY_PEAK_CONTOUR = getInstance(ContourTypes.EARLY_PEAK, new StandardContourBucket[] { // mean is 0.5	
					  		new StandardContourBucket(0.25, 0.1), // 25% charge first 10%
					  		new StandardContourBucket(0.5, 0.1), // 50% charge for next 10%
							new StandardContourBucket(1.0, 0.2), // 100% charge for next 20%
							new StandardContourBucket(0.75, 0.1), // 75% charge for next 10%			
							new StandardContourBucket(0.5, 0.2), // 50% charge next 20%			
							new StandardContourBucket(0.25, 0.1), // 25% charge for next 10%
							new StandardContourBucket(0.15, 0.1), // 15% charge for next 10%
							new StandardContourBucket(0.1, 0.1), // 10% charge for last 10%			

	});

    public static final StandardContour LATE_PEAK_CONTOUR = getInstance(ContourTypes.LATE_PEAK, new StandardContourBucket[] { // mean is 0.5
					  		new StandardContourBucket(0.1, 0.1), // 10% charge for first 10%			
					  		new StandardContourBucket(0.15, 0.1), // 15% charge for next 10%
							new StandardContourBucket(0.25, 0.1), // 25% charge for next 10%
							new StandardContourBucket(0.5, 0.2), // 50% charge next 20%			
							new StandardContourBucket(0.75, 0.1), // 75% charge for next 10%			
							new StandardContourBucket(1.0, 0.2), // 100% charge for next 20%
							new StandardContourBucket(0.5, 0.1), // 50% charge for next 10%																		
							new StandardContourBucket(0.25, 0.1) // 25% charge last 10%
	});

    public static final StandardContour BELL_CONTOUR = getInstance(ContourTypes.BELL, new StandardContourBucket[] { // mean is 0.5	
					  		new StandardContourBucket(0.1, 0.1), // 10% charge for first 10%			
					  		new StandardContourBucket(0.2, 0.1), // 20% charge for next 10%
							new StandardContourBucket(0.4, 0.1), // 40% charge for next 10%
							new StandardContourBucket(0.8, 0.1), // 80% charge next 10%			
							new StandardContourBucket(1.0, 0.2), // 100% charge for next 20%		
							new StandardContourBucket(0.8, 0.1), // 80% charge next 10%			
							new StandardContourBucket(0.4, 0.1), // 40% charge for next 10%
							new StandardContourBucket(0.2, 0.1), // 20% charge for next 10%			
							new StandardContourBucket(0.1, 0.1) // 10% charge for last 10%									
	});

    public static final StandardContour PLATEAU_CONTOUR = getInstance(ContourTypes.PLATEAU, new StandardContourBucket[] { // mean is 0.7
					  		new StandardContourBucket(0.25, 0.1), // 25% charge for first 10%			
					  		new StandardContourBucket(0.5, 0.1), // 50% charge for next 10%
							new StandardContourBucket(0.75, 0.1), // 75% charge for next 10%
							new StandardContourBucket(1.0, 0.4), // 100% charge next 40%		
							new StandardContourBucket(0.75, 0.1), // 75% charge for next 10%				
							new StandardContourBucket(0.5, 0.1), // 50% charge for next 10%			
							new StandardContourBucket(0.25, 0.1), // 25% charge for last 10%						
	});
    

    public static StandardContour getStandardContour(int type){
		switch (type) {
			case FLAT: return FLAT_CONTOUR;
			case BACK_LOADED: return BACK_LOADED_CONTOUR;
			case FRONT_LOADED: return FRONT_LOADED_CONTOUR;
			case DOUBLE_PEAK: return DOUBLE_PEAK_CONTOUR;
			case EARLY_PEAK: return EARLY_PEAK_CONTOUR;
			case LATE_PEAK: return LATE_PEAK_CONTOUR;
			case BELL: return BELL_CONTOUR;
			case PLATEAU: return PLATEAU_CONTOUR;
			default: throw new IllegalArgumentException("Unknown contour type: " + type);
		}
    }


//	public Object clone() throws CloneNotSupportedException {
//		return this; //since this is immutable, no need to clone it
//	}
	public Object clone() {
		return super.clone();
}

	/**
	 * @return Returns the type.
	 */
	public int getType() {
		return type;
	}
	public AbstractContour extend(long end, long extendDuration) {
		return this;
	}
	public AbstractContour extendBefore(long startOffset, long extendDuration) {
		return this;
	}
	
	public MutableInterval getRangeThatIntervalCanBeMoved(long start, long end) {
		return new MutableInterval(start,Long.MAX_VALUE); // by default unbounded 
	}	
}
