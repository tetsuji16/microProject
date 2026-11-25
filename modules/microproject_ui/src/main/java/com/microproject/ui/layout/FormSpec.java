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
package com.microproject.ui.layout;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * One column or row of a {@link FormLayout}: its default alignment, its size and
 * how much of the free space it takes.
 * <p>
 * The encoded form is {@code [alignment:]size[:resize]}, for example
 * {@code right:pref}, {@code 3dlu}, {@code fill:80dlu:grow} or
 * {@code max(50dlu;pref)}.
 */
final class FormSpec {
	enum Align { FILL, LEFT, RIGHT, CENTER, TOP, BOTTOM }

	/** Measures one dimension of a component. */
	interface Measure {
		int sizeOf(Component component);
	}

	/** The size of a column or row before free space is distributed. */
	interface Size {
		int maximumSize(Container container, List<Component> components, Measure minimum, Measure preferred,
				Measure defaultMeasure);

		/** Whether the size may shrink towards the minimum when the container is too small. */
		boolean compressible();
	}

	enum ComponentSize implements Size {
		MINIMUM, PREFERRED, DEFAULT;

		@Override
		public int maximumSize(Container container, List<Component> components, Measure minimum, Measure preferred,
				Measure defaultMeasure) {
			Measure measure = this == MINIMUM ? minimum : this == PREFERRED ? preferred : defaultMeasure;
			int maximum = 0;
			for (Component component : components)
				maximum = Math.max(maximum, measure.sizeOf(component));
			return maximum;
		}

		@Override
		public boolean compressible() {
			return this == DEFAULT;
		}
	}

	enum Unit { PIXEL, DIALOG_UNITS_X, DIALOG_UNITS_Y, POINT }

	record ConstantSize(int value, Unit unit) implements Size, java.io.Serializable {
		int pixels(Component component) {
			return switch (unit) {
				case PIXEL -> value;
				case DIALOG_UNITS_X -> DialogUnits.horizontalToPixels(value, component);
				case DIALOG_UNITS_Y -> DialogUnits.verticalToPixels(value, component);
				case POINT -> DialogUnits.pointsToPixels(value, component);
			};
		}

		@Override
		public int maximumSize(Container container, List<Component> components, Measure minimum, Measure preferred,
				Measure defaultMeasure) {
			return pixels(container);
		}

		@Override
		public boolean compressible() {
			return false;
		}
	}

	/** A component size with a constant lower bound, upper bound, or both. */
	record BoundedSize(Size basis, ConstantSize lower, ConstantSize upper) implements Size {
		@Override
		public int maximumSize(Container container, List<Component> components, Measure minimum, Measure preferred,
				Measure defaultMeasure) {
			int size = basis.maximumSize(container, components, minimum, preferred, defaultMeasure);
			if (lower != null)
				size = Math.max(size, lower.maximumSize(container, components, minimum, preferred, defaultMeasure));
			if (upper != null)
				size = Math.min(size, upper.maximumSize(container, components, minimum, preferred, defaultMeasure));
			return size;
		}

		@Override
		public boolean compressible() {
			return basis.compressible();
		}
	}

	static final double NO_GROW = 0.0;
	static final double DEFAULT_GROW = 1.0;

	final boolean horizontal;
	final Align defaultAlignment;
	final Size size;
	final double resizeWeight;

	private FormSpec(boolean horizontal, Align defaultAlignment, Size size, double resizeWeight) {
		this.horizontal = horizontal;
		this.defaultAlignment = defaultAlignment;
		this.size = size;
		this.resizeWeight = resizeWeight;
	}

	boolean canGrow() {
		return resizeWeight != NO_GROW;
	}

	int maximumSize(Container container, List<Component> components, Measure minimum, Measure preferred,
			Measure defaultMeasure) {
		return size.maximumSize(container, components, minimum, preferred, defaultMeasure);
	}

	/**
	 * Parses a comma-separated list of column ({@code horizontal}) or row specifications.
	 * Commas inside parentheses or brackets do not separate specifications, blanks around
	 * each specification are ignored, and a trailing comma adds nothing.
	 */
	static List<FormSpec> parseList(String encoded, boolean horizontal) {
		List<FormSpec> specs = new ArrayList<>();
		if (encoded == null)
			throw new NullPointerException("The encoded " + kind(horizontal) + " specifications must not be null.");
		int depth = 0;
		int start = 0;
		for (int index = 0; index < encoded.length(); index++) {
			char c = encoded.charAt(index);
			if (c == '(' || c == '[') {
				depth++;
			} else if (c == ')' || c == ']') {
				depth--;
				if (depth < 0)
					throw error(encoded, index, "unbalanced '" + c + "'");
			} else if (c == ',' && depth == 0) {
				add(specs, encoded, start, index, horizontal);
				start = index + 1;
			}
		}
		if (depth != 0)
			throw error(encoded, encoded.length(), "missing closing parenthesis or bracket");
		if (!encoded.substring(start).isBlank())
			add(specs, encoded, start, encoded.length(), horizontal);
		return specs;
	}

	private static void add(List<FormSpec> specs, String encoded, int start, int end, boolean horizontal) {
		String token = encoded.substring(start, end).trim();
		if (token.isEmpty())
			throw error(encoded, start, "empty " + kind(horizontal) + " specification");
		try {
			specs.add(parse(token, horizontal));
		} catch (IllegalArgumentException e) {
			throw error(encoded, start, e.getMessage());
		}
	}

	static FormSpec parse(String encoded, boolean horizontal) {
		String spec = encoded.trim().toLowerCase(Locale.ROOT);
		String[] parts = spec.split(":", -1);
		int next = 0;
		Align alignment = alignment(parts[0], horizontal);
		if (alignment != null) {
			next++;
			if (parts.length < 2)
				throw new IllegalArgumentException("'" + encoded + "' has an alignment but no size");
		} else {
			alignment = horizontal ? Align.FILL : Align.CENTER;
		}
		Size size = size(parts[next++], horizontal);
		double weight = NO_GROW;
		if (next < parts.length)
			weight = resizeWeight(parts[next++]);
		if (next < parts.length)
			throw new IllegalArgumentException("'" + encoded + "' has too many parts");
		return new FormSpec(horizontal, alignment, size, weight);
	}

	private static Align alignment(String token, boolean horizontal) {
		switch (token) {
			case "f", "fill":
				return Align.FILL;
			case "c", "center":
				return Align.CENTER;
			default:
				break;
		}
		if (horizontal) {
			return switch (token) {
				case "l", "left" -> Align.LEFT;
				case "r", "right" -> Align.RIGHT;
				default -> null;
			};
		}
		return switch (token) {
			case "t", "top" -> Align.TOP;
			case "b", "bottom" -> Align.BOTTOM;
			default -> null;
		};
	}

	private static Size size(String token, boolean horizontal) {
		if (token.startsWith("[") && token.endsWith("]"))
			return bracketBounded(token, horizontal);
		if (token.startsWith("max(") && token.endsWith(")"))
			return functionBounded(token, horizontal, false);
		if (token.startsWith("min(") && token.endsWith(")"))
			return functionBounded(token, horizontal, true);
		return atomic(token, horizontal);
	}

	/** {@code max(a;b)} is the larger of a constant and a component size; {@code min(a;b)} the smaller. */
	private static Size functionBounded(String token, boolean horizontal, boolean upperBound) {
		int separator = token.indexOf(';');
		if (separator < 0)
			throw new IllegalArgumentException("'" + token + "' must separate its two sizes with ';'");
		Size first = atomic(token.substring(4, separator), horizontal);
		Size second = atomic(token.substring(separator + 1, token.length() - 1), horizontal);
		ConstantSize constant;
		Size basis;
		if (first instanceof ConstantSize c && second instanceof ComponentSize) {
			constant = c;
			basis = second;
		} else if (second instanceof ConstantSize c && first instanceof ComponentSize) {
			constant = c;
			basis = first;
		} else {
			throw new IllegalArgumentException("'" + token + "' needs one constant size and one component size");
		}
		return upperBound ? new BoundedSize(basis, null, constant) : new BoundedSize(basis, constant, null);
	}

	/** {@code [lower,basis]}, {@code [basis,upper]} or {@code [lower,basis,upper]}. */
	private static Size bracketBounded(String token, boolean horizontal) {
		String[] parts = token.substring(1, token.length() - 1).split("\\s*,\\s*", -1);
		if (parts.length == 2) {
			Size first = atomic(parts[0], horizontal);
			Size second = atomic(parts[1], horizontal);
			if (first instanceof ConstantSize lower && second instanceof ConstantSize both)
				return new BoundedSize(both, lower, both);
			if (first instanceof ConstantSize lower)
				return new BoundedSize(second, lower, null);
			if (second instanceof ConstantSize upper)
				return new BoundedSize(first, null, upper);
		} else if (parts.length == 3) {
			Size lower = atomic(parts[0], horizontal);
			Size basis = atomic(parts[1], horizontal);
			Size upper = atomic(parts[2], horizontal);
			if (lower instanceof ConstantSize l && upper instanceof ConstantSize u)
				return new BoundedSize(basis, l, u);
		}
		throw new IllegalArgumentException("'" + token + "' is not [constant,size], [size,constant] or [constant,size,constant]");
	}

	private static Size atomic(String token, boolean horizontal) {
		String trimmed = token.trim();
		switch (trimmed) {
			case "m", "min":
				return ComponentSize.MINIMUM;
			case "p", "pref":
				return ComponentSize.PREFERRED;
			case "d", "default":
				return ComponentSize.DEFAULT;
			default:
				return constant(trimmed, horizontal);
		}
	}

	static ConstantSize constant(String token, boolean horizontal) {
		int unitStart = token.length();
		while (unitStart > 0 && Character.isLetter(token.charAt(unitStart - 1)))
			unitStart--;
		String number = token.substring(0, unitStart);
		String unitName = token.substring(unitStart);
		Unit unit = switch (unitName) {
			case "", "dlu" -> horizontal ? Unit.DIALOG_UNITS_X : Unit.DIALOG_UNITS_Y;
			case "px" -> Unit.PIXEL;
			case "pt" -> Unit.POINT;
			default -> throw new IllegalArgumentException("'" + token + "' has an unknown unit; use px, dlu or pt");
		};
		double value;
		try {
			value = Double.parseDouble(number);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("'" + token + "' is not a size", e);
		}
		if (value != (int) value)
			throw new IllegalArgumentException("'" + token + "' must be a whole number of " + unitName);
		return new ConstantSize((int) value, unit);
	}

	private static double resizeWeight(String token) {
		switch (token) {
			case "g", "grow":
				return DEFAULT_GROW;
			case "n", "none", "nogrow":
				return NO_GROW;
			default:
				break;
		}
		if ((token.startsWith("grow(") || token.startsWith("g(")) && token.endsWith(")")) {
			String weight = token.substring(token.indexOf('(') + 1, token.length() - 1);
			try {
				double parsed = Double.parseDouble(weight);
				if (!Double.isFinite(parsed) || parsed < 0)
					throw new IllegalArgumentException("Invalid resize weight: " + weight);
				return parsed;
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("'" + token + "' has an invalid weight", e);
			}
		}
		throw new IllegalArgumentException("'" + token + "' is not a resize; use grow, grow(<weight>) or none");
	}

	private static String kind(boolean horizontal) {
		return horizontal ? "column" : "row";
	}

	private static IllegalArgumentException error(String encoded, int position, String message) {
		return new IllegalArgumentException(message + " at position " + position + " in \"" + encoded + "\"");
	}
}
