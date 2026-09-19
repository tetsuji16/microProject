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
package com.microproject.toolbar;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JToolBar;

import com.microproject.help.HelpUtil;
import com.microproject.menu.MenuActionConstants;
import com.microproject.menu.MenuManager;
import com.microproject.grouping.core.transform.ViewConfiguration;

public class FilterToolBarManager implements MenuActionConstants{
	private MenuManager menuManager;
	JToolBar toolBar = null;
	public static FilterToolBarManager create(MenuManager menuManager) {
		return new FilterToolBarManager(menuManager);
	}
	//best place to this?
	protected TransformComboBox filtersComboBox;
	protected TransformComboBox sortersComboBox;
	protected TransformComboBox groupersComboBox;
	protected Component separator1,separator2,separator3,filler;
	

	private FilterToolBarManager(MenuManager menuManager) {
		this.menuManager = menuManager;
		filtersComboBox=new TransformComboBox(menuManager,ACTION_CHOOSE_FILTER,TransformComboBoxModel.FILTER);
		sortersComboBox=new TransformComboBox(menuManager,ACTION_CHOOSE_SORT,TransformComboBoxModel.SORTER);
		groupersComboBox=new TransformComboBox(menuManager,ACTION_CHOOSE_GROUP,TransformComboBoxModel.GROUPER);
//		Border defaultBorder = BorderFactory.createEmptyBorder(8,8,8,8);
//		filtersComboBox.setBorder(defaultBorder);
//		sortersComboBox.setBorder(defaultBorder);
//		groupersComboBox.setBorder(defaultBorder);
		setComboSize(filtersComboBox);
		setComboSize(sortersComboBox);
		setComboSize(groupersComboBox);
		
		separator1=Box.createRigidArea(new Dimension(16,16));
		separator2=Box.createRigidArea(new Dimension(16,16));
		separator3=Box.createRigidArea(new Dimension(20,20));
		filler=new Box.Filler(new Dimension(0,0),new Dimension(0,0),new Dimension(Integer.MAX_VALUE,Integer.MAX_VALUE));
		HelpUtil.addDocHelp(filtersComboBox,"Filters");
		HelpUtil.addDocHelp(sortersComboBox,"Sorts");
		HelpUtil.addDocHelp(groupersComboBox,"Grouping");
				
	}
	
	private void setComboSize(TransformComboBox combo){
		Dimension size = new Dimension(150,28);
		combo.setMinimumSize(size);
		combo.setMaximumSize(size);
		combo.setPreferredSize(size);
	}
	
	public void addButtons(JComponent toolBar) {
		toolBar.add(filler);
		toolBar.add(filtersComboBox,"Center");
		toolBar.add(separator1);
		toolBar.add(sortersComboBox,"Center");
		toolBar.add(separator2);
		toolBar.add(groupersComboBox,"Center");
		toolBar.add(separator3);
		filtersComboBox.addActionListener(menuManager.getActionFromId(filtersComboBox.getActionCommand()));
		sortersComboBox.addActionListener(menuManager.getActionFromId(sortersComboBox.getActionCommand()));
		groupersComboBox.addActionListener(menuManager.getActionFromId(groupersComboBox.getActionCommand()));
	}
	public void addButtonsInRibbonBand(JComponent component) {
		component.add(filtersComboBox);
		component.add(sortersComboBox);
		component.add(groupersComboBox);
		filtersComboBox.addActionListener(menuManager.getActionFromId(filtersComboBox.getActionCommand()));
		sortersComboBox.addActionListener(menuManager.getActionFromId(sortersComboBox.getActionCommand()));
		groupersComboBox.addActionListener(menuManager.getActionFromId(groupersComboBox.getActionCommand()));
	}

	public void removeButtons(JComponent bar) {
		if (bar != null) {
			bar.remove(filler);
			bar.remove(filtersComboBox);
			bar.remove(separator1);
			bar.remove(sortersComboBox);
			bar.remove(separator2);
			bar.remove(groupersComboBox);
			bar.remove(separator3);
		}
		if (filtersComboBox != null)
			filtersComboBox.removeActionListener(menuManager.getActionFromId(filtersComboBox.getActionCommand()));
		if (sortersComboBox != null)
			sortersComboBox.removeActionListener(menuManager.getActionFromId(sortersComboBox.getActionCommand()));
		if (groupersComboBox != null)
			groupersComboBox.removeActionListener(menuManager.getActionFromId(groupersComboBox.getActionCommand()));
	}
	
	
	
	public void setComboBoxesViewName(String viewName){
		ViewConfiguration view=ViewConfiguration.getView(viewName);
		filtersComboBox.setView(view);
		sortersComboBox.setView(view);
		groupersComboBox.setView(view);
	}
	
	public void setEnabled(boolean enable) {
		filtersComboBox.setEnabled(enable);
		sortersComboBox.setEnabled(enable);
		groupersComboBox.setEnabled(enable);
	}
	
	public void transformBasedOnValue() {
		filtersComboBox.transformBasedOnValue();
		sortersComboBox.transformBasedOnValue();
		groupersComboBox.transformBasedOnValue();
	}
	public void clear() {
		if (filtersComboBox.getItemCount() > 0)
			filtersComboBox.setSelectedIndex(0);
		if (sortersComboBox.getItemCount() > 0)
			sortersComboBox.setSelectedIndex(0);
		if (groupersComboBox.getItemCount() > 0)
			groupersComboBox.setSelectedIndex(0);
	}

}

