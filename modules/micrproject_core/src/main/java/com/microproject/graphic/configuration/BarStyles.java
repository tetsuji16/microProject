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

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import org.apache.commons.digester.Digester;

import com.microproject.configuration.NamedItem;
import com.microproject.strings.Messages;

/**
 * Styles of bars on the gantt chart.  Holds a collection of bar formats.
 */
public class BarStyles implements NamedItem {
//	static Log log = LogFactory.getLog(BarStyles.class);
	public static final String category="BarStylesCategory";

	public String getCategory() {
		return category;
	}

	String name = null;
	String id = null;
	ArrayList<BarStyle> rows = new IndexedStyleList();
	private transient List<List<BarStyle>> stylesByKind;

	public BarStyles() {}
	/**
	 * Applies a closure to all bars which should be displayed.  The renderer is called back
	 * with the BarFormat to apply for bars which meet their display conditions.
	 * @param ganttable - A task, resource, assignment... whatever can be displayed in gantt
	 * @param action - Callback - The callback parametes are BarFormats
	 */
	public void apply(Object ganttable, Consumer<Object> action) {
		apply(ganttable,action,false,false,false, false);
	}
	public void apply(Object ganttable, Consumer<Object> action,boolean link,boolean annotation,boolean calendar, boolean horizontalGrid) {
		for (BarStyle row : getStyles(link, annotation, calendar, horizontalGrid)) {
			if (row.evaluate(ganttable)) { // see if meets filter
				action.accept(row.getBarFormat());
			}
		}
	}

	private List<BarStyle> getStyles(boolean link, boolean annotation, boolean calendar, boolean horizontalGrid) {
		if (stylesByKind == null) {
			stylesByKind = new ArrayList<>(16);
			for (int i = 0; i < 16; i++)
				stylesByKind.add(new ArrayList<>());
			for (BarStyle style : rows)
				stylesByKind.get(styleKind(style.isLink(), style.isAnnotation(), style.isCalendar(), style.isHorizontalGrid())).add(style);
		}
		return stylesByKind.get(styleKind(link, annotation, calendar, horizontalGrid));
	}

	private static int styleKind(boolean link, boolean annotation, boolean calendar, boolean horizontalGrid) {
		return (link ? 1 : 0) | (annotation ? 2 : 0) | (calendar ? 4 : 0) | (horizontalGrid ? 8 : 0);
	}

	void invalidateStyleIndex() {
		stylesByKind = null;
	}

	private final class IndexedStyleList extends ArrayList<BarStyle> {
		private static final long serialVersionUID = 1L;

		private void attach(BarStyle style) {
			if (style != null)
				style.setBelongsTo(BarStyles.this);
		}

		private void changed() {
			invalidateStyleIndex();
		}

		@Override
		public boolean add(BarStyle style) {
			attach(style);
			boolean changed = super.add(style);
			if (changed) changed();
			return changed;
		}

		@Override
		public void add(int index, BarStyle style) {
			attach(style);
			super.add(index, style);
			changed();
		}

		@Override
		public boolean addAll(Collection<? extends BarStyle> styles) {
			styles.forEach(this::attach);
			boolean changed = super.addAll(styles);
			if (changed) changed();
			return changed;
		}

		@Override
		public boolean addAll(int index, Collection<? extends BarStyle> styles) {
			styles.forEach(this::attach);
			boolean changed = super.addAll(index, styles);
			if (changed) changed();
			return changed;
		}

		@Override
		public BarStyle set(int index, BarStyle style) {
			attach(style);
			BarStyle previous = super.set(index, style);
			changed();
			return previous;
		}

		@Override
		public BarStyle remove(int index) {
			BarStyle removed = super.remove(index);
			changed();
			return removed;
		}

		@Override
		public boolean remove(Object style) {
			boolean changed = super.remove(style);
			if (changed) changed();
			return changed;
		}

		@Override
		public boolean removeAll(Collection<?> styles) {
			boolean changed = super.removeAll(styles);
			if (changed) changed();
			return changed;
		}

		@Override
		public boolean retainAll(Collection<?> styles) {
			boolean changed = super.retainAll(styles);
			if (changed) changed();
			return changed;
		}

		@Override
		public boolean removeIf(Predicate<? super BarStyle> filter) {
			boolean changed = super.removeIf(filter);
			if (changed) changed();
			return changed;
		}

		@Override
		public void replaceAll(UnaryOperator<BarStyle> operator) {
			super.replaceAll(style -> {
				BarStyle replacement = operator.apply(style);
				attach(replacement);
				return replacement;
			});
			changed();
		}

		@Override
		public void sort(Comparator<? super BarStyle> comparator) {
			super.sort(comparator);
			changed();
		}

		@Override
		public void clear() {
			if (isEmpty()) return;
			super.clear();
			changed();
		}

		@Override
		protected void removeRange(int fromIndex, int toIndex) {
			if (fromIndex == toIndex) return;
			super.removeRange(fromIndex, toIndex);
			changed();
		}
	}


	public void addStyle(BarStyle style) {
		style.setBelongsTo(this);
		style.build(); // set references
		rows.add(style);
		invalidateStyleIndex();
	}
	/**
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name The name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}

	public void setId(String id) {
		this.id = id;
		setName(Messages.getString(id));
	}
	/**
	 * @return Returns the id.
	 */
	public String getId() {
		return id;
	}
	/**
	 * @return Returns the rows.
	 */
	public ArrayList<BarStyle> getRows() {
		return rows;
	}


	String zoomX = null;
	public String getZoomX() {
		return zoomX;
	}
	public void setZoomX(String zoomX) {
		this.zoomX = zoomX;
	}
	String zoomY = null;
	public String getZoomY() {
		return zoomY;
	}
	public void setZoomY(String zoomY) {
		this.zoomY = zoomY;
	}

	double[] zoomRatioX=null;
	int defaultZoomIndexX;
	public double getRatioX(int zoom,boolean in){
		initZoomX();
		if (zoomX==null) return 1.0;
		int index=defaultZoomIndexX+zoom-((in)?0:1);
		if (index<0||index>=zoomRatioX.length) return 1.0;
		return (in)?zoomRatioX[index]:1.0/zoomRatioX[index];
	}
	protected void initZoomX(){
		if (zoomRatioX==null){
			if (zoomX==null) return;
			StringTokenizer st=new StringTokenizer(zoomX,",;:|");
			zoomRatioX=new double[st.countTokens()];
			int index=0;
			while (st.hasMoreTokens()){
				String s=st.nextToken();
				if ("*".equals(s)) defaultZoomIndexX=index;
				else zoomRatioX[index++]=Double.parseDouble(s);
			}
		}
	}
	double[] zoomRatioY=null;
	int defaultZoomIndexY;
	public double getRatioY(int zoom,boolean in){
		initZoomY();
		if (zoomY==null) return 1.0;
		int index=defaultZoomIndexY+zoom-((in)?0:1);
		if (index<0||index>=zoomRatioY.length) return 1.0;
		return (in)?zoomRatioY[index]:1.0/zoomRatioY[index];
	}
	protected void initZoomY(){
		if (zoomRatioY==null){
			if (zoomY==null) return;
			StringTokenizer st=new StringTokenizer(zoomY,",;:|");
			zoomRatioY=new double[st.countTokens()];
			int index=0;
			while (st.hasMoreTokens()){
				String s=st.nextToken();
				if ("*".equals(s)) defaultZoomIndexY=index;
				else zoomRatioY[index++]=Double.parseDouble(s);
			}
		}
	}

	public int getMinZoom(){
		initZoomX();
		initZoomY();
		if (zoomRatioX==null||zoomRatioY==null) return 0;
		return Math.min(-defaultZoomIndexX,-defaultZoomIndexY);
	}
	public int getMaxZoom(){
		initZoomX();
		initZoomY();
		if (zoomRatioX==null||zoomRatioY==null) return 0;
		return Math.min(zoomRatioX.length-defaultZoomIndexX-1,zoomRatioY.length-defaultZoomIndexY-1);
	}


	public static void addDigesterEvents(Digester digester){
		// main properties of bar
		digester.addFactoryCreate("*/bar/styles", "com.microproject.graphic.configuration.BarStylesFactory");
	    digester.addSetProperties("*/bar/styles");
		digester.addSetNext("*/bar/styles", "add", "com.microproject.configuration.NamedItem");

		// start section
		digester.addObjectCreate("*/bar/styles/style", "com.microproject.graphic.configuration.BarStyle");
	    digester.addSetProperties("*/bar/styles/style");
	    digester.addSetNext("*/bar/styles/style", "addStyle", "com.microproject.graphic.configuration.BarStyle");

	}
}
