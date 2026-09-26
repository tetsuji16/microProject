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
package com.microproject.pm.assignment.timesheet;

import java.util.EnumSet;
import java.util.Objects;

public interface AssignmentWorkflowState {
	// they may not be mutually exclusive, so use flags
	enum Kind {
		NEW(0x01),
		NOTIFIED(0x02),
		ACCEPTED(0x04),
		REPLACED(0x08);

		private final int mask;

		Kind(int mask) {
			this.mask = mask;
		}

		public int mask() {
			return mask;
		}
	}

	@Deprecated int UNDEFINED = 0x00;
	@Deprecated int NEW = Kind.NEW.mask();
	@Deprecated int NOTIFIED = Kind.NOTIFIED.mask();
	@Deprecated int ACCEPTED = Kind.ACCEPTED.mask();
	@Deprecated int REPLACED = Kind.REPLACED.mask();

	static EnumSet<Kind> kindsFromMask(int mask) {
		EnumSet<Kind> result = EnumSet.noneOf(Kind.class);
		for (Kind kind : Kind.values()) {
			if ((mask & kind.mask()) != 0) {
				result.add(kind);
			}
		}
		return result;
	}

	static int maskFromKinds(Iterable<Kind> kinds) {
		Objects.requireNonNull(kinds, "kinds");
		int mask = UNDEFINED;
		for (Kind kind : kinds) {
			mask |= Objects.requireNonNull(kind, "kind").mask();
		}
		return mask;
	}
	
}
