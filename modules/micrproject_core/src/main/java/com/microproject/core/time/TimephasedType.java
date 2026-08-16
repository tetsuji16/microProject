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
package com.microproject.core.time;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Laurent Chretienneau
 * 
Used in ProjectLibre
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
public enum TimephasedType {
	REMAINING_WORK(1, 11),
	ACTUAL_WORK(2, 11),
	ACTUAL_OVERTIME_WORK(3, 11),
	BASELINE_WORK(4, 0),
	BASELINE_COST(5, 0),
	ACTUAL_COST(6, 11),
	
	BASELINE1_WORK(16, 1),
	BASELINE1_COST(17, 1),
	BASELINE2_WORK(22, 2),
	BASELINE2_COST(23, 2),
	BASELINE3_WORK(28, 3),
	BASELINE3_COST(29, 3),
	BASELINE4_WORK(34, 4),
	BASELINE4_COST(35, 4),
	BASELINE5_WORK(40, 5),
	BASELINE5_COST(41, 5),
	BASELINE6_WORK(46, 6),
	BASELINE6_COST(47, 6),
	BASELINE7_WORK(52, 7),
	BASELINE7_COST(53, 7),
	BASELINE8_WORK(58, 8),
	BASELINE8_COST(59, 8),
	BASELINE9_WORK(64, 9),
	BASELINE9_COST(65, 9),
	BASELINE10_WORK(70, 10),
	BASELINE10_COST(71, 10);


	protected int id;
	protected int snapshotId;
	protected static Map<Integer,TimephasedType> reverseMap;

	private TimephasedType(int id, int snapshotId){
		this.id=id;
		this.snapshotId=snapshotId;
	}
	public int getId() {
		return id;
	}
	public int getSnapshotId() {
		return snapshotId;
	}
	public static TimephasedType getInstance(int id){
		if (reverseMap==null){
			reverseMap=new HashMap<Integer, TimephasedType>();
			for (TimephasedType ct : values())
				reverseMap.put(ct.getId(),ct);
		}
		return reverseMap.get(id);
	}
	
	public boolean isWork(){
		return 
				this==REMAINING_WORK ||
				this==ACTUAL_WORK ||
				this==ACTUAL_OVERTIME_WORK ||
				this==BASELINE_WORK ||
				this==BASELINE1_WORK ||
				this==BASELINE2_WORK ||
				this==BASELINE3_WORK ||
				this==BASELINE4_WORK ||
				this==BASELINE5_WORK ||
				this==BASELINE6_WORK ||
				this==BASELINE7_WORK ||
				this==BASELINE8_WORK ||
				this==BASELINE9_WORK ||
				this==BASELINE10_WORK;
	}

}
