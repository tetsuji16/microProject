/*******************************************************************************
 * The contents of this file are subject to the Common Public Attribution License 
 * Version 1.0 (the "License"); you may not use this file except in compliance with 
 * the License. You may obtain a copy of the License at 
 * http://www.projectlibre.com/license . The License is based on the Mozilla Public 
 * License Version 1.1 but Sections 14 and 15 have been added to cover use of 
 * software over a computer network and provide for limited attribution for the 
 * Original Developer. In addition, Exhibit A has been modified to be consistent 
 * with Exhibit B. 
 *
 * Software distributed under the License is distributed on an "AS IS" basis, 
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for the 
 * specific language governing rights and limitations under the License. The 
 * Original Code is ProjectLibre. The Original Developer is the Initial Developer 
 * and is ProjectLibre Inc. All portions of the code written by ProjectLibre are 
 * Copyright (c) 2012-2019. All Rights Reserved. All portions of the code written by 
 * ProjectLibre are Copyright (c) 2012-2019. All Rights Reserved. Contributor 
 * ProjectLibre, Inc.
 *
 * Alternatively, the contents of this file may be used under the terms of the 
 * ProjectLibre End-User License Agreement (the ProjectLibre License) in which case 
 * the provisions of the ProjectLibre License are applicable instead of those above. 
 * If you wish to allow use of your version of this file only under the terms of the 
 * ProjectLibre License and not to allow others to use your version of this file 
 * under the CPAL, indicate your decision by deleting the provisions above and 
 * replace them with the notice and other provisions required by the ProjectLibre 
 * License. If you do not delete the provisions above, a recipient may use your 
 * version of this file under either the CPAL or the ProjectLibre Licenses. 
 *
 *
 * [NOTE: The text of this Exhibit A may differ slightly from the text of the notices 
 * in the Source Code files of the Original Code. You should use the text of this 
 * Exhibit A rather than the text found in the Original Code Source Code for Your 
 * Modifications.] 
 *
 * EXHIBIT B. Attribution Information for ProjectLibre required
 *
 * Attribution Copyright Notice: Copyright (c) 2012-2019, ProjectLibre, Inc.
 * Attribution Phrase (not exceeding 10 words): 
 * ProjectLibre, open source project management software.
 * Attribution URL: http://www.projectlibre.com
 * Graphic Image as provided in the Covered Code as file: projectlibre-logo.png with 
 * alternatives listed on http://www.projectlibre.com/logo 
 *
 * Display of Attribution Information is required in Larger Works which are defined 
 * in the CPAL as a work which combines Covered Code or portions thereof with code 
 * not governed by the terms of the CPAL. However, in addition to the other notice 
 * obligations, all copies of the Covered Code in Executable and Source Code form 
 * distributed must, as a form of attribution of the original author, include on 
 * each user interface screen the "ProjectLibre" logo visible to all users. 
 * The ProjectLibre logo should be located horizontally aligned with the menu bar 
 * and left justified on the top left of the screen adjacent to the File menu. The 
 * logo must be at least 144 x 31 pixels. When users click on the "ProjectLibre" 
 * logo it must direct them back to http://www.projectlibre.com. 
 *******************************************************************************/
package com.projectlibre1.pm.task;

import java.util.Collection;
import java.util.ArrayList;

import com.projectlibre1.grouping.core.Node;

public class DefaultSubprojectHandler implements SubprojectHandler {
	private final Project dummyProject;
	private Task containingSubprojectTask;
	private Collection referringSubprojectTasks = new ArrayList();

	public DefaultSubprojectHandler(Project dummy) {
		this.dummyProject = dummy;
	}
	public Task getContainingSubprojectTask() {
		return containingSubprojectTask;
	}

	public long getReferringSubprojectTaskDependencyDate() {
		long result = 0L;
		for (Object value : referringSubprojectTasks) {
			if (value instanceof Task)
				result = Math.max(result, ((Task) value).getDependencyStart());
		}
		return result;
	}

	public Collection getReferringSubprojectTasks() {
		return referringSubprojectTasks;
	}

	public String getSubprojectOf() {
		return dummyProject == null ? null : dummyProject.getName();
	}

	public void setContainingSubprojectTask(Task containingSubprojectTask) {
		this.containingSubprojectTask = containingSubprojectTask;
	}

	public void setReferringSubprojectTasks(Collection referringSubprojectTasks) {
		this.referringSubprojectTasks = referringSubprojectTasks == null
				? new ArrayList()
				: new ArrayList(referringSubprojectTasks);
	}

	public void switchToResourcesOfProject(Project useMe) {
		if (dummyProject != null && useMe != null)
			dummyProject.setResourcePool(useMe.getResourcePool());
	}

	public void addSubproject(Project subproject, Node subprojectNode, boolean creating, boolean currentlyOpen) {
		if (subproject == null)
			throw new IllegalArgumentException("Subproject cannot be null");
		subproject.setOpenedAsSubproject(true);
		if (subprojectNode != null && subprojectNode.getImpl() instanceof Task)
			subproject.setContainingSubprojectTask((Task) subprojectNode.getImpl());
	}

	public boolean canInsertProject(long projectId) {
		if (projectId <= 0L)
			return false;
		Project candidate = ProjectFactory.getInstance().findFromId(projectId);
		return candidate != null && candidate != dummyProject && !candidate.isOpenedAsSubproject();
	}

	public SubProj createSubProj(long subprojectUniqueId) {
		return new DefaultSubProj(dummyProject, subprojectUniqueId);
	}

}
