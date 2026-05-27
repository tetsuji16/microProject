/*
 * file:       ProjectListener.java
 * author:     Jon Iles
 * copyright:  (c) Packwood Software 2005
 * date:       Dec 13, 2005
 */

/*
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */

package net.sf.mpxj.listener;

import net.sf.mpxj.ProjectCalendar;
import net.sf.mpxj.Relation;
import net.sf.mpxj.Resource;
import net.sf.mpxj.ResourceAssignment;
import net.sf.mpxj.Task;

/**
 * Classes implementing this interface can be used to receive notification
 * of events occurring within the project file.
 */
public interface ProjectListener
{
   void taskRead(Task task);
   void taskWritten(Task task);
   void resourceRead(Resource resource);
   void resourceWritten(Resource resource);
   void calendarRead(ProjectCalendar calendar);
   void calendarWritten(ProjectCalendar calendar);
   void assignmentRead(ResourceAssignment assignment);
   void assignmentWritten(ResourceAssignment assignment);
   void relationRead(Relation relation);
   void relationWritten(Relation relation);
}
