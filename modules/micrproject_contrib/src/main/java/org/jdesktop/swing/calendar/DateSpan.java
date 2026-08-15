/*******************************************************************************
 * Minimal ProjectLibre-compatible replacement for JDNC DateSpan.
 *
 * The original ProjectLibre code only relies on the start/end accessors and
 * the two long-based constructors, so this local implementation keeps that
 * surface area and avoids bundling the legacy JDNC jar.
 *******************************************************************************/
package org.jdesktop.swing.calendar;

import java.io.Serializable;
import java.util.Date;

public class DateSpan implements Serializable {
	private static final long serialVersionUID = 1L;

	private final long start;
	private final long end;

	public DateSpan(Date start, Date end) {
		this(start == null ? -1L : start.getTime(), end == null ? -1L : end.getTime());
	}

	public DateSpan(long start, long end) {
		this.start = start;
		this.end = end;
	}

	public long getStart() {
		return start;
	}

	public long getEnd() {
		return end;
	}

	@Override
	public int hashCode() {
		int result = (int) (start ^ (start >>> 32));
		result = 31 * result + (int) (end ^ (end >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DateSpan)) {
			return false;
		}
		DateSpan other = (DateSpan) obj;
		return start == other.start && end == other.end;
	}

	@Override
	public String toString() {
		return "DateSpan[" + start + ", " + end + "]";
	}
}
