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
package com.microproject.pm.graphic.spreadsheet.renderer;
import javax.swing.JComponent;
import javax.swing.JLabel;

import com.microproject.pm.graphic.IconManager;
import com.microproject.company.ApplicationUser;
import com.microproject.company.UserUtil;
import com.microproject.graphic.configuration.HasResourceIndicators;
import com.microproject.strings.Messages;
/**
 *  
 */
public class ResourceIndicatorsComponent extends IndicatorsComponent{
	private static final long serialVersionUID = 19290101L;
	protected JLabel team;
	protected JLabel userPowerUser,userLiteUser,userPowerUserExternal,userLiteUserExternal,userAdmin,userInactive;

	public void init() {
		team = new JLabel(" ",IconManager.getIcon("indicator.team"),  JLabel.RIGHT); //$NON-NLS-1$ //$NON-NLS-2$
		team.setOpaque(false);
		userAdmin = new JLabel(" ", IconManager.getIcon("indicator.user.admin"),  JLabel.RIGHT); //$NON-NLS-1$ //$NON-NLS-2$
		userAdmin.setOpaque(false);
		userPowerUser = new JLabel(" ", IconManager.getIcon("indicator.user.pm"),  JLabel.RIGHT); //$NON-NLS-1$ //$NON-NLS-2$
		userPowerUser.setOpaque(false);
		userLiteUser = new JLabel(" ", IconManager.getIcon("indicator.user.tm"),  JLabel.RIGHT); //$NON-NLS-1$ //$NON-NLS-2$
		userLiteUser.setOpaque(false);
		userPowerUserExternal = new JLabel(" ", IconManager.getIcon("indicator.user.external_pm"),  JLabel.RIGHT); //$NON-NLS-1$ //$NON-NLS-2$
		userPowerUserExternal.setOpaque(false);
		userLiteUserExternal = new JLabel(" ", IconManager.getIcon("indicator.user.external_tm"),  JLabel.RIGHT); //$NON-NLS-1$ //$NON-NLS-2$
		userLiteUserExternal.setOpaque(false);
		userInactive = new JLabel(" ", IconManager.getIcon("indicator.user.inactive"),  JLabel.RIGHT); //$NON-NLS-1$ //$NON-NLS-2$
		userInactive.setOpaque(false);
	}
	
	public boolean acceptValue(Object value){
		return acceptResource(value);
	}
	public static boolean acceptResource(Object value){
		return value instanceof HasResourceIndicators;
	}
	
	
	public void setIndicators(Object value,JComponent label,StringBuilder text,boolean isSelected, boolean hasFocus){
		HasResourceIndicators indicators = (HasResourceIndicators)value;
		if (indicators.isInTeam()) {
			label.add(team);
			setLook(team,isSelected,hasFocus);
			text.append(Messages.getString("ResourceIndicatorsComponent.ThisResourceIsOnTheProjectTeam")); //$NON-NLS-1$
		}
		if (indicators.isUser()) {
			int license=indicators.getLicense();
			JLabel l;
			switch (license) {
			case ApplicationUser.POWER_USER: l=indicators.isAdministrator()?userAdmin:(indicators.isExternal()?userPowerUserExternal:userPowerUser);
				break;
			case ApplicationUser.LITE_USER: l=indicators.isExternal()?userLiteUserExternal:userLiteUser;
				break;
			case ApplicationUser.INACTIVE: l=userInactive;
			break;
			default:
				l=null;
				break;
			}
			if (l!=null){
				label.add(l);
				setLook(l,isSelected,hasFocus);
			text.append(Messages.format("Format.threeParts", Messages.getString("ResourceIndicatorsComponent.UserLicense"),
					UserUtil.licenseToLabel(license),
					Messages.format("Format.join",
					indicators.isAdministrator() ? Messages.getString("ResourceIndicatorsComponent.Administrator") : "",
					indicators.isExternal() ? Messages.getString("ResourceIndicatorsComponent.PartnerCustomer") : "") + "<br>")); //$NON-NLS-1$
			}
		}
	}
	
	
}
