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
package com.microproject.node.summaries;

/**
 * Integer IDs of the summary kinds.
 *
 * <p>The IDs are persisted: configuration files name a summary ({@code groupSum="Maximum"}) and
 * {@link SummaryVisitorFactory#getSummaryId(String)} maps that name back to these integers, while
 * stored fields keep the number. The values therefore stay as they are, including the aliases
 * ({@code MAXIMUM}/{@code OR}, {@code MINIMUM}/{@code AND}) that share an ID.
 */
public interface SummaryNames {
	/** Greatest value of the group; also the flag OR. */
	int MAXIMUM = 0;
	int OR = 0;
	/** Smallest value of the group; also the flag AND. */
	int MINIMUM = 1;
	int AND = 1;
	int COUNT_ALL = 2;
	int SUM = 3;
	int AVERAGE = 4;
	int AVERAGE_FIRST_SUBLEVEL = 5;
	int COUNT_FIRST_SUBLEVEL = 6;
	int COUNT_NONSUMMARIES = 7;
	int SAME = 8;
	int NONE = -1;
	int LIST = -2;
	int CUSTOM = -3;
	int THIS = -4;
}
