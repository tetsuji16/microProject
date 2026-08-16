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
package com.microproject.core.pm.exchange.converters.mpx;	import net.sf.mpxj.Relation;

	import com.microproject.association.InvalidAssociationException;
	import com.microproject.core.pm.exchange.converters.mpx.type.MpxDependencyTypeConverter;
	import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.task.Task;

/**
 * Converts an MPXJ relation into a microproject Dependency. The Dependency is
 * created via the DependencyService (microproject has no public Dependency
 * constructor), so this method returns the created instance (or null on failure).
 * @author Laurent Chretienneau
 */
public class MpxDependencyConverter {

	/**
	 * Converts an MPXJ relation lag into the microproject Dependency lag
	 * encoding. Time-based lags are stored as plain milliseconds (see
	 * Dependency.getLeadValue); percent lags keep their percent encoding so
	 * getLeadValue computes them against the predecessor duration (issue #163).
	 */
	static long toDependencyLag(net.sf.mpxj.Duration mpxLag) {
		if (mpxLag == null) {
			return 0L;
		}
		net.sf.mpxj.TimeUnit unit = mpxLag.getUnits();
		if (unit == net.sf.mpxj.TimeUnit.PERCENT) {
			return com.microproject.datatype.Duration.getInstance(mpxLag.getDuration() / 100.0, com.microproject.datatype.TimeUnit.PERCENT);
		}
		if (unit == net.sf.mpxj.TimeUnit.ELAPSED_PERCENT) {
			return com.microproject.datatype.Duration.getInstance(mpxLag.getDuration() / 100.0, com.microproject.datatype.TimeUnit.ELAPSED_PERCENT);
		}
		return MpxUtils.toMillis(mpxLag);
	}

	public Dependency from(net.sf.mpxj.Relation mpxRelation, MpxImportState state) {
		Task predecessor = state.getTask(mpxRelation.getTargetTask());
		Task successor = state.getTask(mpxRelation.getSourceTask());
		if (predecessor == null || successor == null) {
			return null;
		}

		long lag = toDependencyLag(mpxRelation.getLag());

		MpxDependencyTypeConverter dependencyTypeConverter = new MpxDependencyTypeConverter();
		Integer dependencyType = (Integer) dependencyTypeConverter.from(mpxRelation.getType());
		int type = dependencyType == null ? 0 : dependencyType.intValue();

		try {
			return DependencyService.getInstance().newDependency(predecessor, successor, type, lag, null);
		} catch (InvalidAssociationException e) {
			return null;
		}
	}
}
