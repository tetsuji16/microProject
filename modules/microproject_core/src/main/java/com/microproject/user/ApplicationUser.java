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
package com.microproject.user;

/**
 * Information about the person running the application
 */
public interface ApplicationUser {
	/** Milliseconds of inactivity after which the user is considered idle. */
	public static final long MAX_IDLE_TIME=3600000L;
	/** Idle timeout for a user that never becomes idle. */
	public static final long INFINITE_IDLE_TIME=Long.MAX_VALUE;

	//roles
	/** Role of a user who works on tasks. */
	public static final int TEAM_MEMBER = 8;
	/** Role of a user who manages the project. */
	public static final int PROJECT_MANAGER = 16;
	/** Role of a resource of the project, used instead of {@link #TEAM_MEMBER} for resources. */
	public static final int TEAM_RESOURCE = 32;

	//licenses
	/** License of a user who may not work on projects. */
	public static final int INACTIVE = 0;
	/** License of a user with the full feature set. */
	public static final int POWER_USER = 1;
	/** License of a user with the reduced feature set. */
	public static final int LITE_USER = 2;
	//options
	/** Option marking a user as an application administrator. */
	public static final int ADMINISTRATOR = 1;
	/** License option marking a user as external to the organization. */
	public static final int EXTERNAL = 2; //license option

	/** Name of the administrator role. */
	public static final String ADMINISTRATOR_NAME = "Administrator";
	/** Name of the {@link #TEAM_MEMBER} role. */
	public static final String TEAM_MEMBER_NAME = "TeamMember";
	/** Name of the {@link #PROJECT_MANAGER} role. */
	public static final String PROJECT_MANAGER_NAME = "ProjectManager";
	/** Name of an internal user. */
	public static final String INTERNAL_NAME = "Internal";

	/** Name of the {@link #LITE_USER} license. */
	public static final String LITE_USER_NAME = "LiteUser";
	/** Name of the {@link #POWER_USER} license. */
	public static final String POWER_USER_NAME = "PowerUser";

	/** Name of an external user. */
	public static final String EXTERNAL_NAME = "External";

	/**
	 * @return true when the user may change application settings
	 */
	boolean isAdministrator();

	/**
	 * @return true when the user is external to the organization
	 */
	boolean isExternal();

	/**
	 * @return identifier of the user, unique within the application
	 */
	long getUniqueId();

	/**
	 * @return identifier of the resource that represents this user, or 0 when
	 *         the user is not a project resource
	 */
	long getResourceId();

	/**
	 * @return display name of the user
	 */
	String getName();
}
