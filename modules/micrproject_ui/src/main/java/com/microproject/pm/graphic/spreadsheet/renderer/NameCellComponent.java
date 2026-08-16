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
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.text.JTextComponent;

import com.microproject.pm.graphic.IconManager;
import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetParams;
import com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheetModel;
import com.microproject.graphic.configuration.CellFormat;
import com.microproject.util.FlatUiSupport;
/**
 *
 */
public class NameCellComponent extends JPanel {
	protected JComponent textComponent = null;
	protected JLabel iconLabel = null;
	protected Box.Filler filler = null;
	protected ImageIcon collapsedIcon;
	protected ImageIcon expandedIcon;
	protected ImageIcon leafIcon;
	protected ImageIcon emptyLeafIcon;
	protected String text = null;
	protected ImageIcon icon = null;
	protected boolean lazy = false;
	protected boolean fetched = true;
	protected ImageIcon unfetchedLazyIcon = null;
	protected ImageIcon fetchedLazyExpandedIcon = null;
	protected ImageIcon fetchedLazyCollapsedIcon = null;
	protected boolean offline;
	/**
	 *
	 */
	public NameCellComponent() {
		this(new JLabel(""));
	}
	/**
	 * textComponent is a JLabel or a JTextComponent
	 */
	public NameCellComponent(JComponent textComponent) {
		super();
		this.textComponent = textComponent;
		textComponent.setFont(getFont());
	}
	public void init() {
		setOpaque(true);
		setBackground(FlatUiSupport.spreadsheetBodyBackground());
		setForeground(FlatUiSupport.tableForeground());
		setBorder(FlatUiSupport.tableCellBorder());
		textComponent.setBorder(BorderFactory.createEmptyBorder(0, 1, 0, 0));
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		if (getComponentCount() != 0)
			removeAll();
		leafIcon = IconManager.getIcon("spreadsheet.leaf.icon");
		emptyLeafIcon = IconManager.getIcon("spreadsheet.emptyleaf.icon");
		collapsedIcon = IconManager.getIcon("spreadsheet.collapsed.icon");
		expandedIcon = IconManager.getIcon("spreadsheet.expanded.icon");

		unfetchedLazyIcon = IconManager.getIcon("spreadsheet.unfetchedLazy.icon");
		fetchedLazyExpandedIcon = IconManager.getIcon("spreadsheet.fetchedLazyExpanded.icon");
		fetchedLazyCollapsedIcon = IconManager.getIcon("spreadsheet.fetchedLazyCollapsed.icon");

		filler = (Box.Filler) Box
				.createHorizontalStrut(Math.max(leafIcon.getIconWidth() - 2, 2));
		add(filler);
		iconLabel = new JLabel(leafIcon);
		iconLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
		iconLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		iconLabel.setBorder(BorderFactory.createEmptyBorder(0, 1, 0, 1));
		add(iconLabel);
		add(textComponent);
	}



	public boolean isOffline() {
		return offline;
	}
	public void setOffline(boolean offline) {
		this.offline = offline;
	}
	/**
	 * @return Returns the text component.
	 */
	public JComponent getTextComponent() {
		return textComponent;
	}
	/**
	 * @return Returns the label.
	 */
	public JLabel getIconLabel() {
		return iconLabel;
	}
	public void setText(String text) {
		if (this.text == text)
			return;
		this.text = text;
		if (textComponent instanceof JLabel)
			((JLabel) textComponent).setText(text);
		else if (textComponent instanceof JTextComponent)
			((JTextComponent) textComponent).setText(text);
	}
	public String getText() {
		if (textComponent instanceof JLabel)
			return ((JLabel) textComponent).getText();
		else if (textComponent instanceof JTextComponent)
			return ((JTextComponent) textComponent).getText();
		else
			return null;
	}
	public void setFont(Font font) {
		super.setFont(font);
		if (textComponent != null)
			textComponent.setFont(font);
	}

	public void setLeaf(boolean empty) {
		ImageIcon askedIcon = (empty) ? emptyLeafIcon : leafIcon;
		if (icon == askedIcon)
			return;
		icon = askedIcon;
		iconLabel.setIcon(icon);
	}

	public void setCollapsed(boolean value) {
		ImageIcon askedIcon = null;
		if (isLazy()) {
			if (isFetched())
				askedIcon = (value) ? fetchedLazyCollapsedIcon : fetchedLazyExpandedIcon;
			else
				askedIcon = unfetchedLazyIcon;
		} else {
			askedIcon =(value) ? collapsedIcon : expandedIcon;
		}
		if (icon == askedIcon)
			return;
		icon = askedIcon;
		iconLabel.setIcon(icon);
	}

	public boolean isLeaf() {
		return (icon == leafIcon);
	}
	public boolean isCollapsed() {
		return (icon == collapsedIcon  || icon == unfetchedLazyIcon || icon == fetchedLazyCollapsedIcon);
	}
	public void setLevel(int level) {
		setLevel(level, false);
	}

	int level=-1;
	public void setLevel(int level,boolean offline) {
		if (this.level==level)return;
		this.level=level;
		if (level == 0)
			return;
		int width = (leafIcon == null) ? 0 : (level - 1)
				* leafIcon.getIconWidth();
		filler.changeShape(new Dimension(width, 0), new Dimension(width, 0),
				new Dimension(width, Short.MAX_VALUE));
		if (offline) invalidate(); //needed for offline, otherwise filler don't change
	}
	public static NameCellComponent getInstance() {
		NameCellComponent instance = new NameCellComponent();
		instance.init();
		return instance;
		//problem in icon position in isOnIcon when reusing the same instance
	}
	public static boolean isOnIcon(Point pos, Dimension cellSize, int level) {
		NameCellComponent reference = getInstance();
		reference.setSize(cellSize);
		reference.setLevel(level);
		reference.doLayout();
		Rectangle bounds = reference.getIconLabel().getBounds();
		bounds.grow(4,4); // be more permissive in clicking on +/-
		return bounds.contains(pos);
	}
	public static boolean isOnText(Point pos, Dimension cellSize, int level) {
		NameCellComponent reference = getInstance();
		reference.setSize(cellSize);
		reference.setLevel(level);
		reference.doLayout();
		return reference.getTextComponent().getBounds().contains(pos);
	}


	protected static NameCellComponent rendererComponent;
	protected static NameCellComponent editorComponent;
	protected static Font savedRendererFont,savedEditorFont;
	protected static NameCellComponent getUninitializedComponent(boolean hasFocus){
		if (hasFocus){
			if (editorComponent==null){
				JComponent textComponent=new JTextField();
				editorComponent=new NameCellComponent(textComponent);
				savedEditorFont=editorComponent.getFont();
				textComponent.setBorder(null);
				editorComponent.init();
			}else editorComponent.setFont(savedEditorFont);
			return editorComponent;
		}else{
			if (rendererComponent==null){
				JComponent textComponent=new JLabel();
				rendererComponent=new NameCellComponent(textComponent);
				savedRendererFont=FlatUiSupport.uiFont();
				textComponent.setBorder(null);
				rendererComponent.init();
			}else rendererComponent.setFont(savedRendererFont);
			return rendererComponent;

		}
	}

	public static NameCellComponent getComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		NameCellComponent component = getUninitializedComponent(false);
		//JComponent textComponent=component.getTextComponent();

//		CellUtility.setAppearance(table, value, isSelected, hasFocus, row,
//				column, textComponent);
		component.setOffline(false);
		CellUtility.setAppearance(table, value, isSelected, hasFocus, row,
				column, component);
		component.setBorder(CellUtility.withRowGridOverlay(table, component.getBorder()));
		CommonSpreadSheetModel model = (CommonSpreadSheetModel) table.getModel();
		GraphicNode node = model.getNode(row);
		component.setText(value == null ? "" : value.toString());
		int level=model.getCache().getLevel(node);
		component.setLevel((node.isVoid())?(level+1):level);
		component.setLazy(node.isLazyParent());
		component.setFetched(node.isFetched());
		if (model.getCellProperties(node).isCompositeIcon()) {
			component.setCollapsed(node.isCollapsed());
		} else {
			component.setLeaf(node.isVoid());
		}
		FontManager.setComponentFont(model.getCellProperties(node),component);
		component.doLayout();
		return component;
	}

	public static Component getComponent(Object value, GraphicNode node, SpreadSheetParams params){
		NameCellComponent component = getUninitializedComponent(false);
		CellFormat format=params.getFieldArray().getCellStyle().getCellFormat(node);
		component.getTextComponent().setBorder(null);
		component.setOffline(true);
		CellUtility.setAppearance(format,component);
		String valueS=value == null? " " : value.toString();//to avoid void textComponents with no height
		if (valueS.length()==0) valueS=" ";
		component.setText(valueS);
		int level=params.getCache().getLevel(node);
		component.setLevel((node.isVoid())?(level+1):level,true);
		component.setLazy(node.isLazyParent());
		component.setFetched(node.isFetched());
		if (format.isCompositeIcon()) {
			component.setCollapsed(node.isCollapsed());
		} else {
			component.setLeaf(node.isVoid());
		}
		FontManager.setComponentFont(format,component);
		return component;
	}



	public void doLayout() {
		super.doLayout();
		if (offline){
			textComponent.setSize(getWidth()-textComponent.getX(), textComponent.getHeight());
		}
	}
	public void setBackground(Color bg) {
		super.setBackground(bg);
		if (textComponent!=null)textComponent.setBackground(bg);
	}
	public void setForeground(Color fg) {
		super.setForeground(fg);
		if (textComponent!=null)textComponent.setForeground(fg);
	}




	public void requestFocus() {
		requestTextComponentFocus();
	}

	@Override
	public boolean requestFocusInWindow() {
		return requestTextComponentFocus();
	}

	private boolean requestTextComponentFocus() {
		if (textComponent != null) {
			textComponent.setVisible(true);
			textComponent.setEnabled(true);
			textComponent.setFocusable(true);
			return textComponent.requestFocusInWindow();
		}
		return false;
	}
	/*public boolean requestFocus(boolean temporary) {
		return textComponent.requestFocus(temporary);
	}
	public boolean requestFocusInWindow() {
		return textComponent.requestFocusInWindow();
	}*/
	public final boolean isLazy() {
		return lazy;
	}
	public final void setLazy(boolean lazy) {
		this.lazy = lazy;
	}
	public final boolean isFetched() {
		return fetched;
	}
	public final void setFetched(boolean fetched) {
		this.fetched = fetched;
	}


}

