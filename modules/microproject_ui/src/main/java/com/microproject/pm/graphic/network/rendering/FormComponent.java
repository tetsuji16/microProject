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
package com.microproject.pm.graphic.network.rendering;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.TexturePaint;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.microproject.pm.graphic.ChangeAwareComponent;
import com.microproject.pm.graphic.ChangeAwareTextField;
import com.microproject.configuration.Configuration;
import com.microproject.field.Field;
import com.microproject.field.FieldConverter;
import com.microproject.graphic.configuration.BarFormat;
import com.microproject.graphic.configuration.TexturedShape;
import com.microproject.graphic.configuration.FormBox;
import com.microproject.graphic.configuration.FormBoxLayout;
import com.microproject.graphic.configuration.FormFormat;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.strings.Messages;
import com.microproject.ui.theme.MicroProjectTheme;
import com.microproject.ui.theme.MicroProjectThemeTokens;
import com.microproject.util.Alert;
import com.microproject.util.FlatUiSupport;

public class FormComponent extends JPanel{
	protected int maxRows=3;
	protected int maxCols=2;
	protected Map<String, Component> fieldComponents;
	protected List<BarFormat> selectedFormats;
	protected boolean editor;
	protected int zoom;
	protected boolean texture=true;
	private final Map<TexturedShape, RepresentativeFill> representativeFillCache = new IdentityHashMap<>();
	private final Map<TexturedShape, ThemedShape> themedShapeCache = new IdentityHashMap<>();
	
	
	public FormComponent(List<BarFormat> selectedFormats,int zoom,boolean editor,boolean texture){
		super();
		fieldComponents=new HashMap<>();
		this.selectedFormats=selectedFormats;
		this.editor=editor;
		this.zoom=zoom;
		this.texture=texture;
		setForeground(editor ? FlatUiSupport.tableForeground() : barTextForeground(selectedFormats));
		setBackground(FlatUiSupport.panelBackground());
		BarFormat format;
		if (selectedFormats==null||selectedFormats.size()==0) format=null;
		else format=(BarFormat)selectedFormats.get(0);
		init(format);
		setOpaque(false);
	}
	
	
	public boolean isEditor() {
		return editor;
	}
	public void setEditor(boolean editor) {
		this.editor = editor;
	}
	
	public void init(BarFormat format) {
		if (format==null) return;
		FormFormat form=format.getForm();
		if (form==null) return;
		List<FormBox> boxes=form.getBoxes();
		if (boxes==null||boxes.size()==0){
			return;
		}
		FormBoxLayout formBoxLayout=form.getLayout(zoom);
		FormLayout layout = new FormLayout(
				formBoxLayout.getColumnGrid(),
				formBoxLayout.getRowGrid());
		DefaultFormBuilder builder = new DefaultFormBuilder(this,layout);
		if (formBoxLayout.getBorder()==null) builder.setDefaultDialogBorder();
		else builder.setBorder(Borders.createEmptyBorder(formBoxLayout.getBorder()));
		CellConstraints cc = new CellConstraints();
		for (FormBox box : boxes){
			if (zoom<box.getMinZoom()) return;
			JComponent component;
			if(box.getFieldId()==null) component=new JLabel(Messages.getString(box.getTextId()));
			else{
				if (editor&&!box.getField().isReadOnly()){
					component=new ChangeAwareTextField();
					component.setBorder(null);
					//component.setOpaque(false);
				}else component=new JLabel();
				
				//if (box.getRow()==1&&!editor) ((JLabel)component).setHorizontalAlignment(SwingConstants.CENTER);
				//bug workaround, not possible to center with classic method when rowSpan>1
				
				fieldComponents.put(box.getFieldId(),component);
			}
			component.setForeground(getForeground());
			Font font = NetworkFormFontResolver.resolve(formBoxLayout, box.getFont());
			if (font!=null) component.setFont(font);
			builder.add(component,(box.getAlignment()==null)?
					cc.xywh(box.getColumn(),box.getRow(),box.getColumnSpan(),box.getRowSpan()):
					cc.xywh(box.getColumn(),box.getRow(),box.getColumnSpan(),box.getRowSpan(),box.getAlignment()));
		}
	}

	private Color barTextForeground(List<BarFormat> formats) {
		if (MicroProjectTheme.tokens().appBackground().equals(MicroProjectThemeTokens.light().appBackground()))
			return MicroProjectTheme.tokens().labelForeground();
		if (formats != null && formats.stream().anyMatch(FormComponent::isDefaultDarkSurfaceFormat))
			return MicroProjectTheme.tokens().labelForeground();
		if (formats != null) {
			for (BarFormat format : formats) {
				TexturedShape shape = format.getMiddle();
				if (shape == null) shape = format.getStart();
				if (shape == null) shape = format.getEnd();
				Color background = representativeFill(shape);
				if (background != null) {
					Color darkForeground = new Color(0x202226);
					Color lightForeground = MicroProjectTheme.tokens().labelForeground();
					return contrastRatio(darkForeground, background) >= contrastRatio(lightForeground, background)
						? darkForeground : lightForeground;
				}
			}
		}
		return MicroProjectTheme.tokens().labelForeground();
	}

	private Color representativeFill(TexturedShape shape) {
		if (shape == null) return null;
		Paint paint = shape.getPaint();
		RepresentativeFill cached = representativeFillCache.get(shape);
		if (cached != null && cached.paint == paint) return cached.color;
		Color color = computeRepresentativeFill(shape, paint);
		if (color != null) representativeFillCache.put(shape, new RepresentativeFill(paint, color));
		return color;
	}

	private Color computeRepresentativeFill(TexturedShape shape, Paint paint) {
		if (paint instanceof TexturePaint texturePaint) {
			BufferedImage image = texturePaint.getImage();
			if (texture) {
				Map<Integer, Integer> colorCounts = new HashMap<>();
				int dominantColor = 0;
				int highestCount = 0;
				for (int x = 0; x < image.getWidth(); x++) {
					for (int y = 0; y < image.getHeight(); y++) {
						int rgb = image.getRGB(x, y) & 0x00ffffff;
						int count = colorCounts.merge(rgb, 1, Integer::sum);
						if (count > highestCount) {
							dominantColor = rgb;
							highestCount = count;
						}
					}
				}
				return new Color(dominantColor);
			}
			long red = 0;
			long green = 0;
			long blue = 0;
			int pixels = image.getWidth() * image.getHeight();
			for (int x = 0; x < image.getWidth(); x++) {
				for (int y = 0; y < image.getHeight(); y++) {
					int rgb = image.getRGB(x, y);
					red += (rgb >>> 16) & 0xff;
					green += (rgb >>> 8) & 0xff;
					blue += rgb & 0xff;
				}
			}
			return new Color((int) (red / pixels), (int) (green / pixels), (int) (blue / pixels));
		}
		if (paint instanceof Color color) return color;
		return shape.getColor();
	}

	private record RepresentativeFill(Paint paint, Color color) {}

	private static double contrastRatio(Color foreground, Color background) {
		double foregroundLuminance = relativeLuminance(foreground);
		double backgroundLuminance = relativeLuminance(background);
		double lighter = Math.max(foregroundLuminance, backgroundLuminance);
		double darker = Math.min(foregroundLuminance, backgroundLuminance);
		return (lighter + 0.05d) / (darker + 0.05d);
	}

	private static double relativeLuminance(Color color) {
		return 0.2126d * linearize(color.getRed() / 255d)
			+ 0.7152d * linearize(color.getGreen() / 255d)
			+ 0.0722d * linearize(color.getBlue() / 255d);
	}

	private static double linearize(double channel) {
		return channel <= 0.04045d ? channel / 12.92d : Math.pow((channel + 0.055d) / 1.055d, 2.4d);
	}
	
	
	public Component getComponent(String fieldId){
		return (Component)fieldComponents.get(fieldId);
	}
	
	
	
	public void setFields(Node node,NodeModel model){
		for (String fieldId : fieldComponents.keySet()){
			Field field=Configuration.getFieldFromId(fieldId);
			Object value=field.getValue(node,model,null);
			
			String stringValue="";
			if (value != null)
				stringValue = FieldConverter.toString(value);
			Component textComp=fieldComponents.get(fieldId);
			if (textComp instanceof JLabel)
				((JLabel)textComp).setText(stringValue);
			else{
				((ChangeAwareTextField)textComp).setText(stringValue);
				((ChangeAwareComponent)textComp).resetChange();
			}
		}
	}
	
	
	public List<FieldChange> getChange(){
		ArrayList<FieldChange> change = new ArrayList<>();
		for (String fieldId : fieldComponents.keySet()){
			Component component=fieldComponents.get(fieldId);
			if (component instanceof ChangeAwareComponent&&
					((ChangeAwareComponent)component).hasChanged()){
				String stringValue;
				if (component instanceof JTextField)
					stringValue=((JTextField)component).getText();
				//hangle other components here
				else continue;
				
				Field field=Configuration.getFieldFromId(fieldId);
				try {
					Object value=FieldConverter.fromString(stringValue,field.getDisplayType());
					change.add(new FieldChange(field,value));
				} catch (Exception e) {
					Alert.error(e.getMessage());
				}
			}
		}
		return change;
	}
	
	

	void paintSelectedBars(Graphics2D g2, double width, double height){
		for (BarFormat format : selectedFormats){
			if (format.getMiddle()!=null) drawFormatShape(g2, format, format.getMiddle(),
					width,
					height,
					0,
					+height/2,
					texture);
			if (format.getStart()!=null) drawFormatShape(g2, format, format.getStart(),
					width,
					height,
					0,
					+height/2,
					texture);
			if (format.getEnd()!=null) drawFormatShape(g2, format, format.getEnd(),
					width,
					height,
					0,
					+height/2,
					texture);
		}
		
	}

	private static boolean isDefaultDarkSurfaceFormat(BarFormat format) {
		return format != null
			&& MicroProjectTheme.tokens().appBackground().equals(MicroProjectThemeTokens.dark().appBackground())
			&& isDefaultDiagramFormat(format);
	}

	private static boolean isDefaultDiagramFormat(BarFormat format) {
		if (format == null) return false;
		if (format.getStart() != null || format.getEnd() != null) return false;
		String id = format.getId();
		if (id == null) return false;
		TexturedShape shape = format.getMiddle();
		if (shape == null || !defaultShapeName(id).equals(shape.getShapeName())
			|| !"LARGE_FRAMED".equals(shape.getStrokeName())) return false;
		return switch (id) {
			case "Pert.task" -> "BLUE".equals(shape.getColorName()) && "VERY_SPACED_DOTS".equals(shape.getPaintName());
			case "Pert.critical" -> "RED".equals(shape.getColorName()) && "VERY_SPACED_DOTS".equals(shape.getPaintName());
			case "Pert.milestone", "Pert.summary", "WBS.task", "WBS.milestone", "WBS.summary", "RBS.resource", "RBS.summary" ->
				"BLACK".equals(shape.getColorName()) && "TRANSPARENT".equals(shape.getPaintName());
			default -> false;
		};
	}

	private static String defaultShapeName(String id) {
		return switch (id) {
			case "WBS.task", "RBS.resource" -> "FULL_HEIGHT";
			case "WBS.milestone" -> "HEXAGON";
			case "WBS.summary", "RBS.summary", "Pert.summary" -> "PARALLELOGRAM";
			case "Pert.task", "Pert.critical" -> "FULL_HEIGHT";
			case "Pert.milestone" -> "HEXAGON";
			default -> "";
		};
	}

	private void drawFormatShape(Graphics2D g2, BarFormat format, TexturedShape shape,
		double width, double height, double x, double y, boolean useTexture) {
		if (!isDefaultDiagramFormat(format)) {
			shape.draw(g2, width, height, x, y, useTexture);
			return;
		}
		themedShape(format, shape).draw(g2, width, height, x, y, useTexture);
	}

	private TexturedShape themedShape(BarFormat format, TexturedShape shape) {
		Color surface = MicroProjectTheme.tokens().spreadsheetBodyBackground();
		boolean dark = MicroProjectTheme.tokens().appBackground().equals(MicroProjectThemeTokens.dark().appBackground());
		Color accent = format.getId().equals("Pert.critical") ? MicroProjectTheme.tokens().errorColor()
			: format.getId().equals("Pert.task") ? MicroProjectTheme.tokens().accentColor()
			: dark ? MicroProjectTheme.tokens().borderColor() : MicroProjectTheme.tokens().labelForeground();
		Color fill = dark ? surface : blend(surface, accent, 0.10d);
		Paint sourcePaint = shape.getPaint();
		ThemedShape cached = themedShapeCache.get(shape);
		if (cached != null && cached.sourcePaint == sourcePaint && cached.surface.equals(fill) && cached.accent.equals(accent))
			return cached.shape;
		TexturedShape themed = new TexturedShape();
		themed.setShape(shape.getShape());
		themed.setPaint(fill);
		themed.setColor(accent);
		themed.setStroke(com.microproject.graphic.configuration.shape.PredefinedStroke.find("FRAMED"));
		themed.setShapeScaleX(shape.getShapeScaleX());
		themed.setShapeScaleY(shape.getShapeScaleY());
		themedShapeCache.put(shape, new ThemedShape(sourcePaint, fill, accent, themed));
		return themed;
	}

	private static Color blend(Color background, Color foreground, double amount) {
		return new Color(
			(int) Math.round(background.getRed() * (1d - amount) + foreground.getRed() * amount),
			(int) Math.round(background.getGreen() * (1d - amount) + foreground.getGreen() * amount),
			(int) Math.round(background.getBlue() * (1d - amount) + foreground.getBlue() * amount));
	}

	private record ThemedShape(Paint sourcePaint, Color surface, Color accent, TexturedShape shape) {}
	
	
	
	public void paint(Graphics g) {
		/*CommonGraphCell cell=(CommonGraphCell)view.getCell();
		if (cell.getNode().isVoid()) return;*/
		
		Graphics2D g2=(Graphics2D)g;
		Color textForeground = editor ? FlatUiSupport.tableForeground() : barTextForeground(selectedFormats);
		setForeground(textForeground);
		for (Component component : getComponents()) component.setForeground(textForeground);
		Dimension d=getSize();
		double w=d.getWidth();
		double h=d.getHeight();
		
		
		paintSelectedBars(g2,w-1,h-1);
//		ImageIcon link=IconManager.getIcon("common.link.image");
//		g2.drawImage(link.getImage(),(int)(w-link.getIconWidth()),(int)(h-link.getIconHeight()),this);
		//x=w and y=h are outside
		
		try {
			//if (preview && !isDoubleBuffered)
			//	setOpaque(false);
			super.paint(g);
			//paintSelectionBorder(g);
		} catch (IllegalArgumentException e) {
			// JDK Bug: Zero length string passed to TextLayout constructor
		}
	}
	
	
	
	
	
	
	
	

	/**
	 * Provided for subclassers to paint a selection border.
	 */
	/*protected void paintSelectionBorder(Graphics g) {
		((Graphics2D) g).setStroke(GraphConstants.SELECTION_STROKE);
		if (childrenSelected)
			g.setColor(graph.getGridColor());
		else if (hasFocus && selected)
			g.setColor(graph.getLockedHandleColor());
		else if (selected)
			g.setColor(graph.getHighlightColor());
		if (childrenSelected || selected) {
			Dimension d = getSize();
			g.drawRect(0, 0, d.width - 1, d.height - 1);
		}
	}*/
	/**
	 * Overridden for performance reasons.
	 * See the <a href="#override">Implementation Note</a>
	 * for more information.
	 */
	protected void firePropertyChange(
		String propertyName,
		Object oldValue,
		Object newValue) {
		if ("text".equals(propertyName))
			super.firePropertyChange(propertyName, oldValue, newValue);
	}

	/**
	 * Overridden for performance reasons.
	 * See the <a href="#override">Implementation Note</a>
	 * for more information.
	 */
	public void firePropertyChange(
		String propertyName,
		byte oldValue,
		byte newValue) {
	}

	/**
	 * Overridden for performance reasons.
	 * See the <a href="#override">Implementation Note</a>
	 * for more information.
	 */
	public void firePropertyChange(
		String propertyName,
		char oldValue,
		char newValue) {
	}

	/**
	 * Overridden for performance reasons.
	 * See the <a href="#override">Implementation Note</a>
	 * for more information.
	 */
	public void firePropertyChange(
		String propertyName,
		short oldValue,
		short newValue) {
	}

	/**
	 * Overridden for performance reasons.
	 * See the <a href="#override">Implementation Note</a>
	 * for more information.
	 */
	public void firePropertyChange(
		String propertyName,
		int oldValue,
		int newValue) {
	}

	/**
	 * Overridden for performance reasons.
	 * See the <a href="#override">Implementation Note</a>
	 * for more information.
	 */
	public void firePropertyChange(
		String propertyName,
		long oldValue,
		long newValue) {
	}

	/**
	 * Overridden for performance reasons.
	 * See the <a href="#override">Implementation Note</a>
	 * for more information.
	 */
	public void firePropertyChange(
		String propertyName,
		float oldValue,
		float newValue) {
	}

	/**
	 * Overridden for performance reasons.
	 * See the <a href="#override">Implementation Note</a>
	 * for more information.
	 */
	public void firePropertyChange(
		String propertyName,
		double oldValue,
		double newValue) {
	}

	/**
	 * Overridden for performance reasons.
	 * See the <a href="#override">Implementation Note</a>
	 * for more information.
	 */
	public void firePropertyChange(
		String propertyName,
		boolean oldValue,
		boolean newValue) {
	}


	
	

}

