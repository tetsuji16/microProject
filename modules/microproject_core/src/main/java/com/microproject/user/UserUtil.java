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

import com.microproject.i18n.Messages;

/**
 * Utility class for user roles
 */
public class UserUtil {

	/**
	 * Expands a role for the field combo box, where a team member that is a
	 * resource is shown separately.
	 *
	 * @param role the stored role
	 * @param user true when the role belongs to a user rather than a resource
	 * @return {@link ApplicationUser#TEAM_RESOURCE} for a resource team member,
	 *         otherwise the unchanged role
	 */
	public static int toExtendedRole(int role,boolean user){ //more information for field combo
		if (role==ApplicationUser.TEAM_MEMBER&&!user) return ApplicationUser.TEAM_RESOURCE;
		else return role;
	}

	/**
	 * Collapses a role back to the role that is stored with the user.
	 *
	 * @param role the role shown in the field combo box
	 * @return {@link ApplicationUser#TEAM_MEMBER} for {@link ApplicationUser#TEAM_RESOURCE},
	 *         otherwise the unchanged role
	 */
	public static int toNormalRole(int role){
		if (role==ApplicationUser.TEAM_RESOURCE) return ApplicationUser.TEAM_MEMBER;
		return role;
	}

	/**
	 * @param license one of {@link ApplicationUser#LITE_USER},
	 *                {@link ApplicationUser#POWER_USER} or {@link ApplicationUser#INACTIVE}
	 * @return the localized license name, or an empty string for any other value
	 */
	public static String licenseToLabel(int license) {
		return switch (license) {
		case ApplicationUser.LITE_USER -> Messages.getString("License.LiteUser");
		case ApplicationUser.POWER_USER -> Messages.getString("License.PowerUser");
		case ApplicationUser.INACTIVE -> Messages.getString("License.Inactive");
		default -> "";
		};
	}
}
