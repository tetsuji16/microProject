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
package com.microproject.graphic.configuration.shape;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.TexturePaint;
import java.awt.image.BufferedImage;
import java.util.HashMap;

/**
 *
 */
public class PredefinedPaint extends TexturePaint {
	private Color foreground;
	private Color background;
	private int width;
	private int height;
	private int[] points;
	private float alphaValue=1.0f;


	public PredefinedPaint(int width, int height, int[] array) {
		this(width, height, array, Color.black, Color.white);
	}
	public PredefinedPaint(PredefinedPaint pattern, Color foreground, Color background) {
		this(pattern.width, pattern.height, pattern.points, foreground,
				background);
	}

	private PredefinedPaint(int width, int height, int[] points, Color foreground,
			Color background) {
		super(createTexture(width, height, points, foreground, background),
				new Rectangle(0, 0, width, height));
		this.width = width;
		this.height = height;
		this.foreground = foreground;
		this.background = background;
		this.points = points;
		float sum=0;
		for (int i=0; i<points.length;i++){
			sum+=points[i];
		}
		alphaValue = (float) Math.pow(sum/points.length,1.5); //modify brightness
	}

	private static BufferedImage createTexture(int width, int height,
			int[] array, Color foreground, Color background) {
		BufferedImage bufferedImage = new BufferedImage(width, height,BufferedImage.TYPE_INT_ARGB);
		for (int w = 0; w < width; w++) {
			for (int h = 0; h < height; h++) {
				bufferedImage.setRGB(w, h, array[w + h * width] > 0 ? foreground.getRGB() : background.getRGB());
			}
		}
		return bufferedImage;
	}

	public static final PredefinedPaint TRANSPARENT =  new PredefinedPaint(4, 4,
			new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0});
	public static final PredefinedPaint SOLID = new PredefinedPaint(4, 4,
			new int[]{1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1});
	public static final PredefinedPaint DEFAULT = new PredefinedPaint(4, 4,
			new int[]{1, 0, 1, 0, 0, 1, 0, 1, 1, 0, 1, 0, 0, 1, 0, 1});
	public static final PredefinedPaint SPACED_DOTS = new PredefinedPaint(4, 4,
			new int[]{1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0});
	public static final PredefinedPaint DIAGONAL = new PredefinedPaint(4, 4,
			new int[]{0, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 0});
	public static final PredefinedPaint DOT_LINE = new PredefinedPaint(4, 4,
			new int[]{1, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 1, 0});
	public static final PredefinedPaint DASH_LINE = new PredefinedPaint(1, 4,
			new int[]{1, 1, 0, 0});
	public static final PredefinedPaint DOT_LINE2 = new PredefinedPaint(1, 2,
			new int[]{1, 0});
	public static final PredefinedPaint VERY_SPACED_DOTS = new PredefinedPaint(8, 8,
			new int[]{
			1, 0, 0, 0, 0, 0, 0, 0, 
			0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0
			});
	
	private static Object[][] data = {
	  {"TRANSPARENT",TRANSPARENT},
	  {"SOLID",	SOLID},
	  {"DEFAULT",DEFAULT},
	  {"SPACED_DOTS",SPACED_DOTS},
	  {"VERY_SPACED_DOTS",VERY_SPACED_DOTS},
	  {"DASH_LINE",DASH_LINE},
	  {"DIAGONAL",DIAGONAL},
	  {"DOT_LINE2",DOT_LINE2},
	  {"DOT_LINE",DOT_LINE}
	};
	private static HashMap shapePaintMap = null;
	
	public static HashMap getShapePaints() {
		if (shapePaintMap == null) {
			shapePaintMap = new HashMap();
			for (int i = 0; i < data.length; i++) {
				Object row[] = data[i];
				shapePaintMap.put(row[0], row[1]);
			}
		}
		return shapePaintMap;
	}

	public static PredefinedPaint find(String key) {
		if (key == null) {
			return null;
		}
		return(PredefinedPaint) getShapePaints().get(key);
	}


	public void applyPaint(Graphics2D g2,boolean texture){
		//if ("SVGGraphics2D".equals(g2.getClass().getSimpleName()))
		if (texture) {
			g2.setPaint(this); // the paint already has the color set
		} else {
			g2.setColor(new Color(bar(foreground.getRed(),background.getRed(),alphaValue),bar(foreground.getGreen(),background.getGreen(),alphaValue),bar(foreground.getBlue(),background.getBlue(),alphaValue)));
		}
		
	}
	private int bar(float a,float b,float w){
		return Math.round(a*w+(1.0f-w)*b);
	}

    
}
