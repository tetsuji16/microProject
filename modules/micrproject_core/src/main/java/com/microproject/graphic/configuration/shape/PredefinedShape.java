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

import java.awt.geom.GeneralPath;
import java.util.HashMap;

import com.microproject.util.ArrayUtils;

/**
 * 
 */
public class PredefinedShape {
	private String name;
	private double[][] points = null;
	private double[][][] pointGrid = null; 

	public GeneralPath toGeneralPath(double hScale, double vScale, double hShift, double vShift) {
		GeneralPath path;
		if (points != null) {
			path = new GeneralPath(GeneralPath.WIND_EVEN_ODD, points.length);
			int x, y;
			for (int i = 0; i < points.length; i++) {
				x = r(hScale * points[i][0] + hShift);
				y = r(vScale * points[i][1] + vShift);
				if (i == 0)
					path.moveTo(x, y);
				else
					path.lineTo(x, y);
			}
		} else {
			path = new GeneralPath(GeneralPath.WIND_EVEN_ODD, pointGrid.length);
			int x, y;
			for (int i = 0; i < pointGrid.length; i++) {
				x = r(hScale * pointGrid[i][0][0] + vScale * pointGrid[i][0][1] + hShift * pointGrid[i][0][2] + vShift
						* pointGrid[i][0][3] + hShift);
				y = r(hScale * pointGrid[i][1][0] + vScale * pointGrid[i][1][1] + hShift * pointGrid[i][1][2] + vShift
						* pointGrid[i][1][3] + vShift);
				if (i == 0)
					path.moveTo(x, y);
				else
					path.lineTo(x, y);
			}
		}
		path.closePath();
		return path;
	}

	protected PredefinedShape(String name, double[][] points, double hScale, double vScale, double hShift, double vShift) {
		this.name = name;
		this.points = ArrayUtils.clone(points);
		scale(hScale, vScale);
		translate(hShift, vShift);

	}

	protected PredefinedShape(String name, double[][] points) {
		this(name, points, 1.0, 1.0, 0.0, 0.0);
	}

	protected PredefinedShape(String name, double[][][] matrixPoints) {
		this.name = name;
		this.pointGrid = ArrayUtils.clone(matrixPoints);
	}

	private void scale(double hScale, double vScale) {
		for (int i = 0; i < points.length; i++) {
			points[i][0] *= hScale;
			points[i][1] *= vScale;
		}
	}

	private void translate(double hShift, double vShift) {
		for (int i = 0; i < points.length; i++) {
			points[i][0] += hShift;
			points[i][1] += vShift;
		}
	}
	private static int r(double d) {
		return (int) Math.round(d);
	}

	private static void add(PredefinedShape predefinedShape) {
		predefinedShapeMap.put(predefinedShape.name, predefinedShape);
	}

	private static final double rectPoints[][] = new double[][] { { 1, .5 }, { 0, .5 }, { 0, -.5 }, { 1, -.5 }};

	public static final PredefinedShape FULL_HEIGHT = new PredefinedShape("FULL_HEIGHT", rectPoints);

	public static final PredefinedShape HALF_HEIGHT_TOP = new PredefinedShape("HALF_HEIGHT_TOP", rectPoints, 1, .5, 0, -.25);

	public static final PredefinedShape HALF_HEIGHT_BOTTOM = new PredefinedShape("HALF_HEIGHT_BOTTOM", rectPoints, 1, .5, 0, .25);

	public static final PredefinedShape HALF_HEIGHT_CENTER = new PredefinedShape("HALF_HEIGHT_CENTER", rectPoints, 1, .5, 0, 0);

	public static final PredefinedShape QUARTER_HEIGHT_CENTER = new PredefinedShape("QUARTER_HEIGHT_CENTER", rectPoints, 1, .25, 0, 0);

	public static final PredefinedShape SQUARE = new PredefinedShape("SQUARE", new double[][] { { .5, .5 }, { -.5, .5 }, { -.5, -.5 }, { .5, -.5 }

	});

	public static final PredefinedShape DIAMOND = new PredefinedShape("DIAMOND", new double[][] { { 0, -.5 }, { -.5, 0 }, { 0, .5 }, { .5, 0 } });

	public static final PredefinedShape PENTAGON_UP = new PredefinedShape("PENTAGON_UP", new double[][] { { 0, -.5 }, { -.5, 0 }, { -.5, .5 },
			{ .5, .5 }, { .5, 0 } });

	public static final PredefinedShape PENTAGON_DOWN = new PredefinedShape("PENTAGON_DOWN", new double[][] { { 0, .5 }, { -.5, 0 }, { -.5, -.5 },
			{ .5, -.5 }, { .5, 0 } });

	public static final PredefinedShape TRIANGLE_UP = new PredefinedShape("TRIANGLE_UP", new double[][] { { -.5, .5 }, { 0, -.5 }, { .5, .5 }, });

	public static final PredefinedShape TRIANGLE_DOWN = new PredefinedShape("TRIANGLE_DOWN", new double[][] { { -.5, -.5 }, { 0, .5 }, { .5, -.5 }, });

	public static final PredefinedShape TRIANGLE_RIGHT = new PredefinedShape("TRIANGLE_RIGHT",
			new double[][] { { -.5, -.5 }, { -.5, .5 }, { .5, 0 }, });

	public static final PredefinedShape TRIANGLE_LEFT = new PredefinedShape("TRIANGLE_LEFT", new double[][] { { .5, -.5 }, { .5, .5 }, { -.5, 0 }, });

	public static final PredefinedShape ARROW_UP = new PredefinedShape("ARROW_UP", new double[][] { { -.2, .5 }, { -.2, 0 }, { -.5, 0 }, { 0, -.5 },
			{ .5, 0 }, { .2, 0 }, { .2, .5 }, });

	public static final PredefinedShape ARROW_DOWN = new PredefinedShape("ARROW_DOWN", new double[][] { { -.2, -.5 }, { -.2, 0 }, { -.5, 0 },
			{ 0, .5 }, { .5, 0 }, { .2, 0 }, { .2, -.5 }, });

	public static final PredefinedShape LINK_ARROW1 = new PredefinedShape("LINK_ARROW1", new double[][] { { 0, 0 }, { 1, 1 }, { .7, 0 }, { 1, -1 },
			{ 0, 0 }, });

	// pert shapes
	public static final PredefinedShape HEXAGON = new PredefinedShape("HEXAGON", new double[][][] { { { 1, -.25, 0, 0 }, { 0, .5, 0, 0 } },
			{ { 0, .25, 0, 0 }, { 0, .5, 0, 0 } }, { { 0, 0, 0, 0 }, { 0, 0, 0, 0 } }, { { 0, .25, 0, 0 }, { 0, -.5, 0, 0 } },
			{ { 1, -.25, 0, 0 }, { 0, -.5, 0, 0 } }, { { 1, 0, 0, 0 }, { 0, 0, 0, 0 } }, });

	public static final PredefinedShape PARALLELOGRAM = new PredefinedShape("PARALLELOGRAM", new double[][][] { { { 1, 0, 0, 0 }, { 0, -.5, 0, 0 } },
			{ { 0, .25, 0, 0 }, { 0, -.5, 0, 0 } }, { { 0, 0, 0, 0 }, { 0, .5, 0, 0 } }, { { 1, -.25, 0, 0 }, { 0, .5, 0, 0 } }, });

	public static final PredefinedShape[] MIDDLE_LIST = { FULL_HEIGHT, HALF_HEIGHT_TOP, HALF_HEIGHT_BOTTOM, HALF_HEIGHT_CENTER, QUARTER_HEIGHT_CENTER };

	public static final PredefinedShape[] END_LIST = { SQUARE, DIAMOND, PENTAGON_UP, PENTAGON_DOWN, TRIANGLE_UP, TRIANGLE_DOWN, TRIANGLE_RIGHT,
			TRIANGLE_LEFT, ARROW_UP, ARROW_DOWN, LINK_ARROW1 };

	public static final PredefinedShape[] NETWORK_LIST = { FULL_HEIGHT, HEXAGON, PARALLELOGRAM };

	private static void initialize() {

		add(FULL_HEIGHT);
		add(HALF_HEIGHT_TOP);
		add(HALF_HEIGHT_BOTTOM);
		add(HALF_HEIGHT_CENTER);
		add(QUARTER_HEIGHT_CENTER);

		add(SQUARE);
		add(DIAMOND);
		add(PENTAGON_UP);
		add(PENTAGON_DOWN);
		add(TRIANGLE_UP);
		add(TRIANGLE_DOWN);
		add(TRIANGLE_RIGHT);
		add(TRIANGLE_LEFT);
		add(ARROW_UP);
		add(ARROW_DOWN);

		// pert shapes
		add(HEXAGON);
		add(PARALLELOGRAM);

		// links
		add(LINK_ARROW1);

	}

	private static HashMap predefinedShapeMap = null;

	private static HashMap getPredefinedShapeMap() {
		if (predefinedShapeMap == null) {
			predefinedShapeMap = new HashMap();
			initialize();
		}
		return predefinedShapeMap;
	}

	public static PredefinedShape find(String key) {
		if (key == null) {
			return null;
		}
		PredefinedShape found = (PredefinedShape) getPredefinedShapeMap().get(key);
		return found;
	}
}
