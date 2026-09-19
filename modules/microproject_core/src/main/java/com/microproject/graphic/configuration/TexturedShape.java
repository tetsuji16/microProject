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
package com.microproject.graphic.configuration;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;

import com.microproject.graphic.configuration.shape.Colors;
import com.microproject.graphic.configuration.shape.PredefinedPaint;
import com.microproject.graphic.configuration.shape.PredefinedShape;
import com.microproject.graphic.configuration.shape.PredefinedStroke;

public class TexturedShape {
	PredefinedShape shape = null;

	Color color = null;

	Paint paint = null;

	Stroke stroke = null;

	String paintName = null;

	String colorName = null;

	String strokeName = null;

	String shapeName = null;

	double shapeScaleX = 1.0d;

	double shapeScaleY = 1.0d;

	public TexturedShape() {
	}

	void build() {
		shape = PredefinedShape.find(shapeName);
		color = Colors.findColor(colorName);
		stroke = PredefinedStroke.find(strokeName);
		paint = new PredefinedPaint(PredefinedPaint.find(paintName), color, Colors.findColor("WHITE"));
	}

	public void setShapeName(String shapeName) {
		this.shapeName = shapeName;
	}

	public void setColorName(String colorName) {
		this.colorName = colorName;
	}

	public void setStrokeName(String strokeName) {
		this.strokeName = strokeName;
	}

	public void setPaintName(String paintName) {
		this.paintName = paintName;
	}

	public Color getColor() {
		return color;
	}

	public Paint getPaint() {
		return paint;
	}

	public PredefinedShape getShape() {
		return shape;
	}

	public Stroke getStroke() {
		return stroke;
	}

	public void setColor(Color color) {
		this.color = color;
	}

	public void setPaint(Paint paint) {
		this.paint = paint;
	}

	public void setShape(PredefinedShape shape) {
		this.shape = shape;
	}

	public void setStroke(Stroke stroke) {
		this.stroke = stroke;
	}

	public Shape draw(Graphics2D g2, double w, double h, double dw, double dh, boolean texture) {
		return draw(g2, w, h, dw, dh, null, texture);
	}

	public double getShapeScaleX() {
		return shapeScaleX;
	}

	public void setShapeScaleX(double shapeScaleX) {
		this.shapeScaleX = shapeScaleX;
	}

	public double getShapeScaleY() {
		return shapeScaleY;
	}

	public void setShapeScaleY(double shapeScaleY) {
		this.shapeScaleY = shapeScaleY;
	}

	public Shape draw(Graphics2D g2, double dw, double dh, AffineTransform transform, boolean texture) {
		return draw(g2, shapeScaleX, shapeScaleY, dw, dh, transform, texture);
	}

	public GeneralPath toGeneralPath(double w, double h, double dw, double dh, AffineTransform transform) {
		GeneralPath theShape = getShape().toGeneralPath(w - 1, h, dw, dh);// -1
																			// to
																			// have
																			// edge
																			// inside
																			// bounds;
		if (transform != null)
			theShape.transform(transform);
		return theShape;
	}

	public Shape draw(Graphics2D g2, double w, double h, double dw, double dh, AffineTransform transform, boolean texture) {
		Shape theShape = toGeneralPath(w, h, dw, dh, transform);
		paintShape(g2, theShape, texture);
		return theShape;
	}

	public void paintShape(Graphics2D g2, Shape theShape, boolean texture) {
		Stroke oldStroke = null;
		Paint oldPaint = null;
		Color oldColor = null;

		Paint myPaint = getPaint(); // can be null
		Stroke myStroke = getStroke();
		if (myPaint == null) { // if no paint, then just set color and draw
								// using stroke
			oldColor = g2.getColor();
			g2.setColor(getColor()); // no paint, so just set color
			if (myStroke != PredefinedStroke.SOLID) {
				oldStroke = g2.getStroke();
				g2.setStroke(myStroke);
			}
			g2.draw(theShape);
		} else { // use paint
			oldPaint = g2.getPaint();
			applyPaint(g2, texture);
			g2.fill(theShape);
			if (myStroke != PredefinedStroke.SOLID) { // if also specified a
														// stroke, use it too
				oldColor = g2.getColor();
				g2.setColor(getColor());
				oldStroke = g2.getStroke();
				g2.setStroke(myStroke);
				g2.draw(theShape);
			}
		}
		if (oldColor != null)
			g2.setColor(oldColor);
		if (oldPaint != null)
			g2.setPaint(oldPaint);
		if (oldStroke != null)
			g2.setStroke(oldStroke);
	}

	protected void applyPaint(Graphics2D g2, boolean texture) {
		// if ("SVGGraphics2D".equals(g2.getClass().getSimpleName()))
		if (texture)
			g2.setPaint(paint); // the paint already has the color set
		else {
			if (paint instanceof PredefinedPaint) {
				PredefinedPaint p = (PredefinedPaint) paint;
				p.applyPaint(g2, texture);
			} else
				g2.setPaint(paint);
		}

	}

}
