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
package com.microproject.core.pm.exchange.converters.op;

import java.util.logging.Logger;

import com.microproject.pm.calendar.WorkCalendar;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.resource.ResourceImpl;

/**
 * Copies a microproject Resource into a microproject ResourceImpl. Both sides use
 * the same microproject model, so this is a direct typed-field copy. Rates / cost /
 * maximum-units are intentionally skipped (see issue #154).
 * @author Laurent Chretienneau
 */
public class OpResourceConverter {
	protected static Logger log = Logger.getLogger("OpTaskConverter");

	public void to(com.microproject.pm.resource.ResourceImpl opResource, Resource resource, OpImportState state) {
		if (resource.getName() != null)
			opResource.setName(resource.getName());
		if (resource.getNotes() != null)
			opResource.setNotes(resource.getNotes());
		opResource.setGeneric(resource.isGeneric());
		if (resource.getGroup() != null)
			opResource.setGroup(resource.getGroup());
		if (resource.getInitials() != null)
			opResource.setInitials(resource.getInitials());
		if (resource.getEmailAddress() != null)
			opResource.setEmailAddress(resource.getEmailAddress());
		opResource.setId(resource.getId());
		opResource.setExternalId(resource.getExternalId());
		opResource.setAccrueAt(resource.getAccrueAt());

		WorkCalendar calendar = resource.getWorkCalendar();
		if (calendar != null) {
			opResource.setWorkCalendar(calendar);
		}
	}
}
