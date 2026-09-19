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
package com.microproject.pm.graphic.timescale;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.geom.Line2D;

import javax.swing.JPanel;
import javax.swing.UIManager;

import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.pm.graphic.gantt.GanttParams;
import com.microproject.graphic.configuration.GraphicConfiguration;
import com.microproject.timescale.TimeInterval;
import com.microproject.timescale.TimeIterator;
import com.microproject.util.Environment;
import com.microproject.util.FlatUiSupport;


/**
 *
 */
public class TimeScaleComponent extends JPanel {
	protected CoordinatesConverter coord;
	protected static Color textColor,lineColor;
	/**
	 *
	 */
	public TimeScaleComponent(CoordinatesConverter coord) {
		super();
		this.coord=coord;


		int h=GraphicConfiguration.getInstance().getColumnHeaderHeight();


		refreshThemeColors();

//		setBackground(UIManager.getColor("TableHeader.cellColor"));
//		setBackground(UIManager.getColor("TableHeader.cellBackground"));

		//setBackground(LafUtils.getUnselectedBackgroundColor());

		//setMinimumSize(new Dimension(0,h));
		//setMaximumSize(new Dimension(Integer.MAX_VALUE,h));
		setPreferredSize(new Dimension(0,h));
		FlatUiSupport.applyTableHeaderStyle(this);
		setBorder(UIManager.getBorder ("TableHeader.cellBorder"));
	}


	/**
	 * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
	 */
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2=(Graphics2D)g;
		FlatUiSupport.enableAntialiasing(g2);
        paintTimeScale(g2,coord,getFont(),new Dimension(0,getHeight()),true);
	}

	public static void paintTimeScale(Graphics2D g2,GanttParams params,Font font) {
        paintTimeScale(g2,params.getCoord(),font,new Dimension((int)params.getGanttBounds().getWidth(),params.getConfiguration().getColumnHeaderHeight()),false);
	}

	public static void paintTimeScale(Graphics2D g2,CoordinatesConverter coord,Font font,Dimension d,boolean clipping){
		refreshThemeColors();
		if (font == null)
			font = FlatUiSupport.ganttHeaderFont();
		Rectangle clipBounds = g2.getClipBounds();
		double h=d.getHeight();
		double x0,w;
		if (clipping){
			x0=clipBounds.getX();
			w=clipBounds.getWidth();//getWidth();
			GraphicManager.getInstance().getLafManager().paintTimeScale(g2, clipBounds.x, 0,clipBounds.width,d.height, new Shape[]{
					new Line2D.Double(x0,0,x0+w,0),
					new Line2D.Double(x0,h-1,x0+w,h-1),
			});
		}else{
			x0=0;
			w=d.getWidth();
		}

		g2.setColor(lineColor);
		if (Environment.isMac()){
			g2.draw(new Line2D.Double(x0,h-1,x0+w,h-1));
		}
		g2.draw(new Line2D.Double(x0,h/2,x0+w,h/2));


		TimeIterator i=coord.getTimeIterator(x0,x0+w);
		//g2.getRenderingHints().put(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
		//g2.setFont(new Font("Courrier", Font.PLAIN, 12));
		g2.setFont(/*UIManager.getFont("TableHeader.cellFont")*/font);
		//Font[]  fonts=GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
		//for (int k=0;k<fonts.length;k++)
		FontRenderContext context = g2.getFontRenderContext();

		long start=-1;
		long end=-1;
		double lastBottomLabelEnd = Double.NEGATIVE_INFINITY;
		double lastTopLabelEnd = Double.NEGATIVE_INFINITY;

		while(i.hasNext()){
			TimeInterval interval=i.next();
			if (start==-1) start=interval.getStart1();
			end=interval.getEnd1();

			double x1=coord.toX(interval.getStart1());
			double x2=coord.toX(interval.getEnd1());
			g2.setColor(lineColor);
			if (clipping) g2.draw(new Line2D.Double(x1,h/2,x1,h)); //when scrolling pixel by pixel both lines are needed
			g2.draw(new Line2D.Double(x2,h/2,x2,h));

			String text=interval.getText1();
			LineMetrics metrics=font.getLineMetrics(text,context);
			double bottomLabelX = x1 + 2;
			double bottomLabelWidth = font.getStringBounds(text, context).getWidth();
			if (canPaintLabel(bottomLabelX, bottomLabelWidth, lastBottomLabelEnd)) {
				g2.setColor(textColor);
				g2.drawString(text,(int)bottomLabelX,((int)h)-metrics.getDescent()-metrics.getLeading());
				lastBottomLabelEnd = bottomLabelX + bottomLabelWidth;
			}

			if (interval.getText2()!=null){
				double X1=/*Math.round(*/coord.toX(interval.getStart2())/*)*/; //round for TimeSpreadSheet
				double X2=coord.toX(interval.getEnd2());

				g2.setColor(lineColor);
				if (clipping) g2.draw(new Line2D.Double(X1,0,X1,h/2));//when scrolling pixel by pixel both lines are needed
				g2.draw(new Line2D.Double(X2,0,X2,h/2));
				text=interval.getText2();
				metrics=font.getLineMetrics(text,context);
				double topLabelX = X1 + 2;
				double topLabelWidth = font.getStringBounds(text, context).getWidth();
				if ((clipping||((int)X1+2>=x0)) && canPaintLabel(topLabelX, topLabelWidth, lastTopLabelEnd)){
					g2.setColor(textColor);
					g2.drawString(text,(int)topLabelX,((int)h)/2-metrics.getDescent()-metrics.getLeading());
					lastTopLabelEnd = topLabelX + topLabelWidth;
				}
				//avoids svg clipping. Very slow with firefox or opera
			}


		}

	}

	/** Returns whether a timescale label can be painted without colliding with its predecessor. */
	static boolean canPaintLabel(double x, double width, double previousEnd) {
		return width > 0.0d && x >= previousEnd + 4.0d;
	}

	private static void refreshThemeColors() {
		textColor = FlatUiSupport.headerForeground();
		lineColor = FlatUiSupport.ganttHeaderGridColor();
	}
}
