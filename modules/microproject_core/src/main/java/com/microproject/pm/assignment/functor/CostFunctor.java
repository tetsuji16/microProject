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
package com.microproject.pm.assignment.functor;
import com.microproject.algorithm.CollectionIntervalGenerator;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.assignment.contour.AbstractContourBucket;
import com.microproject.pm.assignment.contour.ContourBucketIntervalGenerator;
import com.microproject.pm.calendar.WorkCalendar;
import com.microproject.pm.costing.CostRate;
import com.microproject.pm.time.HasStartAndEnd;


/**
 * A functor which calculates cost (regular, overtime, fixed, total)
 */
public class CostFunctor extends AssignmentFieldOvertimeFunctor {
	CollectionIntervalGenerator costRateGenerator;
	long fixedCostDate;
	boolean proratedCost;
	double fixedValue = 0.0D;
	double regularWork = 0.0D;
	double overtimeWork = 0.0D;
	double work = 0.0D;
	public static CostFunctor getInstance(Assignment assignment, WorkCalendar workCalendar, ContourBucketIntervalGenerator contourBucketIntervalGenerator, double overtimeUnits, com.microproject.algorithm.CollectionIntervalGenerator costRateGenerator, long fixedCostDate, boolean proratedCost) {
		return new CostFunctor(assignment, workCalendar, contourBucketIntervalGenerator, overtimeUnits, costRateGenerator, fixedCostDate, proratedCost);
	}
	private CostFunctor(Assignment assignment, WorkCalendar workCalendar, ContourBucketIntervalGenerator contourBucketIntervalGenerator, double overtimeUnits, CollectionIntervalGenerator costRateGenerator, long fixedCostDate, boolean proratedCost) {
		super(assignment,workCalendar,contourBucketIntervalGenerator, overtimeUnits);
		this.costRateGenerator = costRateGenerator;
		this.fixedCostDate = fixedCostDate;
		this.proratedCost = proratedCost;
	}
	public void accept(Object object) {
		HasStartAndEnd interval = (HasStartAndEnd)object;		
		AbstractContourBucket bucket = (AbstractContourBucket) contourBucketIntervalGenerator.current();
		
		
		if (bucket != null) {
			CostRate costRate = (CostRate)costRateGenerator.current();
			double bucketUnits = bucket.getEffectiveUnits(assignment.getUnits()); 
			if (bucketUnits != 0.0) { // there are never values if there is no normal cost. 
				// calculate regular and overtime
				long bucketDuration = workCalendar.compare(interval.getEnd(),interval.getStart(), false);

				//When we handle overhead, we need to have another interval generator which keeps overhead in sorted order
				// The bucket duration should be multiplied by 1 - overhead.  Code also needs to exist in workFunctor.  maybe others too
				// double overhead = overheadIntervalGenerator.current();
				// bucketDuration *= (1.0 - overhead);
		
				// might as well calculate work too
				regularWork += bucketUnits * bucketDuration;
				overtimeWork += overtimeUnits * bucketDuration;
				work = regularWork + overtimeWork;
				
				
				
				double bucketOvertime = costRate.getOvertimeRate().getValue() * overtimeUnits;
				double bucketRegular = costRate.getStandardRate().getValue() * bucketUnits;
				if (assignment.isTemporal()) { // for work resources or time based material
					bucketRegular *= bucketDuration;
					bucketOvertime *= bucketDuration;
				}
				overtimeValue += bucketOvertime;
				regularValue += bucketRegular;
				value += (bucketOvertime + bucketRegular);
				
				// Below is fixed cost processing.
				double costPerUse = costRate.getCostPerUse();
				if (costPerUse != 0.0D) { 
					double fraction = 1.0D; // fraction of fixed cost to use - only relevant if prorated
					if (proratedCost) { // prorated across duration
						long assignmentDuration = assignment.getDuration();
						if (assignmentDuration != 0) {
							fraction =  ((double)bucketDuration) / assignment.getDuration();
						} 
					} else { // at a certain date - start or end
						// make sure that the start or end date falls within the interval
						if (interval.getStart() > fixedCostDate || interval.getEnd() < fixedCostDate)
							return; // not in range
					}
					// Notice how the cost per use is multiplied by the assignment units, which itself is the peak units used.  
					double bucketFixed = fraction * costPerUse * assignment.getUnits();
					fixedValue += bucketFixed;
					value += bucketFixed;
				}
			}
		}
	}
	public void initialize() {
		super.initialize();
		fixedValue = 0.0D;
		regularWork = 0.0D;
		overtimeWork = 0.0D;
		work = 0.0D;
	}

	/**
	 * @return Returns the fixedValue.
	 */
	public double getFixedValue() {
		return fixedValue;
	}

	public String toString() {
		return " total " + value  + "  regular " + regularValue + "  overtime " + overtimeValue + "  fixed " + fixedValue;
	}
	public final double getOvertimeWork() {
		return overtimeWork;
	}
	public final double getRegularWork() {
		return regularWork;
	}
	public final double getWork() {
		return work;
	}
	
}
