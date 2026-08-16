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
package com.microproject.print;

import java.awt.Dimension;
import java.awt.print.PageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import javax.print.DocFlavor;
import javax.print.PrintService;
import javax.print.attribute.EnumSyntax;
import javax.print.attribute.standard.Media;
import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.MediaSizeName;
import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;

import com.microproject.strings.Messages;

public class PageSizes extends MediaSizeName{
	public static final int BIG_PAGE=ExtendedPageFormat.BIG_PAGE;
	public static final int CUSTOM=ExtendedPageFormat.CUSTOM;
	protected static PageSizes instance;
	protected Format[] sizes,sizesSystemNames;
	public static class Format{
		protected String name;
		protected MediaSizeName value;
		protected Dimension dimension;
		protected int type;
		public Format(String name,int type) {
			this.name=name;
			this.value=null;
			this.type=type;
		}
//		public Format(String name, MediaSizeName value) {
//			this.name=name;
//			this.value=value;
//			dimension=new Dimension();
//			MediaSize size=MediaSize.getMediaSizeForName(value);
//			dimension.setSize(size.getX(MediaSize.MM),size.getX(MediaSize.MM));
//		}
		public Format(String name, MediaSize size) {
			this.name=name;
			this.value=size.getMediaSizeName();
			dimension=new Dimension();
			dimension.setSize(size.getX(MediaSize.MM),size.getY(MediaSize.MM));
		}
		public String toString() {
			return name;
		}
		public Dimension getDimension() {
			return dimension;
		}
		public MediaSizeName getValue() {
			return value;
		}
		public int getType() {
			return type;
		}

	}

	protected PageSizes(){
		super(-1);
		String[] names=getStringTable();
		EnumSyntax[] values=getEnumValueTable();
		ArrayList<Format> s = new ArrayList<Format>(names.length+2);
		s.add(new Format(Messages.getString("PageSetupDialog.PaperFormat.Custom"),CUSTOM));
		s.add(new Format(Messages.getString("PageSetupDialog.PaperSizeSettings.SinglePage"),BIG_PAGE));
		ArrayList<Format> so = new ArrayList<Format>(names.length+2);
		so.add(new Format(Messages.getString("PageSetupDialog.PaperFormat.Custom"),CUSTOM));
		so.add(new Format(Messages.getString("PageSetupDialog.PaperSizeSettings.SinglePage"),BIG_PAGE));
		for (int i=0;i<names.length;i++){
			MediaSize size=MediaSize.getMediaSizeForName((MediaSizeName)values[i]);
			if (size==null) continue; //all MediaSizeName aren't necessary present in MediaSize
			String oname=names[i];
			String name=oname;
			if (name.startsWith("iso-")) name=name.substring(4);
			else if (name.startsWith("na-")) name=name.substring(3);
			else if (name.startsWith("jis-")&&name.length()>5) name=name.substring(0,5).toUpperCase()+name.substring(5);
			if (name.length()==0) continue;
			name=name.substring(0,1).toUpperCase()+name.substring(1);
			s.add(new Format(name,size));
			so.add(new Format(oname,size));
		}
		sizes=new Format[s.size()];
		sizes=s.toArray(sizes);
		sizesSystemNames=new Format[so.size()];
		sizesSystemNames=so.toArray(sizesSystemNames);
	}

	public static PageSizes getInstance(){
		if (instance==null){
			instance=new PageSizes();
		}
		return instance;
	}


	public Format[] getPageSizes(){
		return sizes;
	}

	public Dimension getPageDimension(Object ps){
		if (ps==null || ! (ps instanceof Format)) return null;
		return ((PageSizes.Format)ps).getDimension();

	}
	public boolean isCustomPageSize(Object ps){
		if (ps==null || ! (ps instanceof Format)) return false;
		return ((PageSizes.Format)ps).getType()==CUSTOM;
	}
	public boolean isBigPageSize(Object ps){
		if (ps==null || ! (ps instanceof Format)) return false;
		return ((PageSizes.Format)ps).getType()==BIG_PAGE;
	}

	public Format getPageSize(ExtendedPageFormat pageFormat){
		MediaSizeName name=pageFormat.getSizeName();
		if (name==null){
			int type=pageFormat.getType();
			for (int i=1;i<sizes.length;i++){
				if (sizesSystemNames[i].getType()==type) return sizes[i];
			}
		}else{
			for (int i=1;i<sizes.length;i++){
				if (name.equals(sizesSystemNames[i].getValue())) return sizes[i];
			}
		}
		return sizes[0];
	}


	public MediaSizeNameModel createComboBoxModel(PrintService printService){
		return new MediaSizeNameModel(sizes,sizesSystemNames,printService);
	}

	public static class MediaSizeNameModel extends AbstractListModel implements ComboBoxModel{
		protected Format[] sizes,sizesSystemNames;
		protected ArrayList<Format> currentSizes;
		protected MediaSizeNameModel(Format[] sizes,Format[] sizesSystemNames,PrintService printService){
			this.sizes=sizes;
			this.sizesSystemNames=sizesSystemNames;
			currentSizes=new ArrayList<Format>(sizes.length);
			update(printService);
		}

		public Format update(PrintService printService){
			Set<MediaSizeName> mediaSizeNames=null;
			if (!(printService instanceof PDFPrintService)){
				Media[] m=(Media[])printService.getSupportedAttributeValues(Media.class,DocFlavor.SERVICE_FORMATTED.PRINTABLE,null);
				mediaSizeNames=new HashSet<MediaSizeName>();
				for (int i=0;i<m.length;i++){
					if (m[i] instanceof MediaSizeName) mediaSizeNames.add((MediaSizeName)m[i]);
				}
			}
			currentSizes.clear();
			boolean lastSelectedItemFound=false;
			MediaSizeName selected=selectedItem==null?null:((Format)selectedItem).getValue();
			for (int i=0;i<sizes.length;i++){
				MediaSizeName m=sizesSystemNames[i].getValue();
				if (m==null&&mediaSizeNames!=null) continue;
				if (mediaSizeNames==null||mediaSizeNames.contains(m)){
					if (m!=null&&m.equals(selected)) lastSelectedItemFound=true;
					currentSizes.add(sizes[i]);
				}
			}

			Format sel=null;
			if (!lastSelectedItemFound){
				sel= selectDefault(printService,true);
			}

			fireContentsChanged(this, 0, currentSizes.size());
			return sel;
		}

		protected Object selectedItem;

		public Object getSelectedItem() {
			return selectedItem;
		}

		public void setSelectedItem(Object selectedItem) {
			this.selectedItem = selectedItem;
		}
		public Format selectDefault(PrintService printService,boolean select){
			MediaSizeName name=ExtendedPageFormat.getDefaultMediaSizeName(printService);
			for(Format f: currentSizes){
				if (f.getValue()==null) continue;
				if (f.getValue().equals(name)){
					if (select) setSelectedItem(f);
					return f;
				}
			}
			return null;

		}



		public Object getElementAt(int index) {
			return currentSizes.get(index);
		}

		public int getSize() {
			return currentSizes.size();
		}

	}



//		new Format("A4",210,297),
//		new Format("A5",148,210),
//		new Format("A6",105,148),
//		new Format("A7",74,105),
//		new Format("A8",52,74),
//		new Format("A9",37,52),
//		new Format("A10",26,37),
//		new Format("B0",1000,1414),
//		new Format("B1",707,1000),
//		new Format("B2",500,707),
//		new Format("B3",353,500),
//		new Format("B4",250,353),
//		new Format("B5",176,250),
//		new Format("B6",125,176),
//		new Format("B7",88,125),
//		new Format("B8",62,88),
//		new Format("B9",44,62),
//		new Format("B10",31,44),
//		new Format("Letter",8.5,11,INCH),
//		new Format("Legal",8.5,14,INCH),
//		new Format("Ledger",17,11,INCH),
//		new Format("Tabloid",11,17,INCH),
//		new Format("Executive",7.5,10.5,INCH),
//		new Format("Super-B",13,19,INCH),
//		new Format("Half Letter",5.5,8.5,INCH),
//		new Format("Architectural-A",9,12,INCH),
//		new Format("Architectural-B",12,18,INCH),
//		new Format("Architectural-C",18,24,INCH),
//		new Format("Architectural-D",22.5,36,INCH),
//		new Format("Architectural-E",36,48,INCH),
//		new Format("ANSI-A",8.5,11,INCH),
//		new Format("ANSI-B",11,17,INCH),
//		new Format("ANSI-C",17,22,INCH),
//		new Format("ANSI-D",22,34,INCH),
//		new Format("ANSI-E",34,44,INCH)
//	};
}

