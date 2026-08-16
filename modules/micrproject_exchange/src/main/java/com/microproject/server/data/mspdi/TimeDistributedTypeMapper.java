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
package com.microproject.server.data.mspdi;

import java.math.BigInteger;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;

import com.microproject.exchange.TimeDistributedTypeMapperConstants;
import com.microproject.pm.assignment.HasTimeDistributedData;

/**
 *
From pjxml help file
The type of timephased data: 
1 Assignment Remaining Work  
2 Assignment Actual Work  
3 Assignment Actual Overtime Work 
4 Assignment Baseline Work  
5 Assignment Baseline Cost 
6 Assignment Actual Cost  
7 Resource Baseline Work 
8 Resource Baseline Cost  
9 Task Baseline Work 
10 Task Baseline Cost  
11 Task Percent Complete 
16 Assignment Baseline 1 Work  
17 Assignment Baseline 1 Cost  
18 Task Baseline 1 Work 
19 Task Baseline 1 Cost  
20 Resource Baseline 1 Work 
21 Resource Baseline 1 Cost  
22 Assignment Baseline 2 Work 
23 Assignment Baseline 2 Cost  
24 Task Baseline 2 Work 
25 Task Baseline 2 Cost  
26 Resource Baseline 2 Work 
27 Resource Baseline 2 Cost  
28 Assignment Baseline 3 Work 
29 Assignment Baseline 3 Cost  
30 Task Baseline 3 Work 
31 Task Baseline 3 Cost  
32 Resource Baseline 3 Work 
33 Resource Baseline 3 Cost  
34 Assignment Baseline 4 Work 
35 Assignment Baseline 4 Cost  
36 Task Baseline 4 Work 
37 Task Baseline 4 Cost  
38 Resource Baseline 4 Work 
39 Resource Baseline 4 Cost  
40 Assignment Baseline 5 Work 
41 Assignment Baseline 5 Cost  
42 Task Baseline 5 Work 
43 Task Baseline 5 Cost  
44 Resource Baseline 5 Work 
45 Resource Baseline 5 Cost  
46 Assignment Baseline 6 Work 
47 Assignment Baseline 6 Cost  
48 Task Baseline 6 Work 
49 Task Baseline 6 Cost  
50 Resource Baseline 6 Work 
51 Resource Baseline 6 Cost  
52 Assignment Baseline 7 Work 
53 Assignment Baseline 7 Cost  
54 Task Baseline 7 Work 
55 Task Baseline 7 Cost  
56 Resource Baseline 7 Work 
57 Resource Baseline 7 Cost  
58 Assignment Baseline 8 Work 
59 Assignment Baseline 8 Cost  
60 Task Baseline 8 Work 
61 Task Baseline 8 Cost  
62 Resource Baseline 8 Work 
63 Resource Baseline 8 Cost  
64 Assignment Baseline 9 Work 
65 Assignment Baseline 9 Cost  
66 Task Baseline 9 Work 
67 Task Baseline 9 Cost 
68 Resource Baseline 9 Work  
69 Resource Baseline 9 Cost 
70 Assignment Baseline 10 Work  
71 Assignment Baseline 10 Cost 
72 Task Baseline 10 Work  
73 Task Baseline 10 Cost 
74 Resource Baseline 10 Work  
75 Resource Baseline 10 Cost 
76 Physical Percent Complete 

ProjectLibre additions
100 Timecard unvalidated
101 Timecard validated

 */
public class TimeDistributedTypeMapper extends TimeDistributedTypeMapperConstants{

	
	public static Object getOPPrField(BigInteger mpxValue) {
		return map.get(mpxValue);
	}
	public static int getMpxValueForField(Object field) {
		return ((Number)map.getKey(field)).intValue();
	}
	
	public static int getBaselineNumber(int mpxValue) {
		if (mpxValue <=10)
			return 0;
		return (mpxValue - 10) / 6;
	}

	public static boolean isCurrent(int mpxValue) {
		return mpxValue == ASSIGNMENT_REMAINING_WORK
				|| mpxValue == ASSIGNMENT_ACTUAL_WORK
				|| mpxValue == ASSIGNMENT_ACTUAL_OVERTIME_WORK;
	}
	
	private static BidiMap map  = new DualHashBidiMap();
	static {
		map.put(BigInteger.valueOf(1),HasTimeDistributedData.REMAINING_WORK);
		map.put(BigInteger.valueOf(2),HasTimeDistributedData.ACTUAL_WORK);
	//	map.put(BigInteger.valueOf(3),HasTimeDistributedData.OVERTIME_WORK);

		map.put(BigInteger.valueOf(4),HasTimeDistributedData.BASELINE_WORK);
		map.put(BigInteger.valueOf(5),HasTimeDistributedData.BASELINE_COST);
		map.put(BigInteger.valueOf(6),HasTimeDistributedData.ACTUAL_COST);
		
		map.put(BigInteger.valueOf(16),HasTimeDistributedData.BASELINE1_WORK);
		map.put(BigInteger.valueOf(17),HasTimeDistributedData.BASELINE1_COST);
		map.put(BigInteger.valueOf(22),HasTimeDistributedData.BASELINE2_WORK);
		map.put(BigInteger.valueOf(23),HasTimeDistributedData.BASELINE2_COST);
		map.put(BigInteger.valueOf(28),HasTimeDistributedData.BASELINE3_WORK);
		map.put(BigInteger.valueOf(29),HasTimeDistributedData.BASELINE3_COST);
		map.put(BigInteger.valueOf(34),HasTimeDistributedData.BASELINE4_WORK);
		map.put(BigInteger.valueOf(35),HasTimeDistributedData.BASELINE4_COST);
		map.put(BigInteger.valueOf(40),HasTimeDistributedData.BASELINE5_WORK);
		map.put(BigInteger.valueOf(41),HasTimeDistributedData.BASELINE5_COST);
		map.put(BigInteger.valueOf(46),HasTimeDistributedData.BASELINE6_WORK);
		map.put(BigInteger.valueOf(47),HasTimeDistributedData.BASELINE6_COST);
		map.put(BigInteger.valueOf(52),HasTimeDistributedData.BASELINE7_WORK);
		map.put(BigInteger.valueOf(53),HasTimeDistributedData.BASELINE7_COST);
		map.put(BigInteger.valueOf(58),HasTimeDistributedData.BASELINE8_WORK);
		map.put(BigInteger.valueOf(59),HasTimeDistributedData.BASELINE8_COST);
		map.put(BigInteger.valueOf(64),HasTimeDistributedData.BASELINE9_WORK);
		map.put(BigInteger.valueOf(65),HasTimeDistributedData.BASELINE9_COST);
		map.put(BigInteger.valueOf(70),HasTimeDistributedData.BASELINE10_WORK);
		map.put(BigInteger.valueOf(71),HasTimeDistributedData.BASELINE10_COST);
	}
	
	
}
