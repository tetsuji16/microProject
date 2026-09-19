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
package com.microproject.core.dictionary;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;


/**
 * Dictionary for the legacy JAXB configuration engine. New code should use
 * {@link com.microproject.configuration.Dictionary}; this type remains only
 * as a compatibility boundary for {@code core.fields}/ {@code core.nodes}.
 *
 * @author Laurent Chretienneau
 */
@Deprecated(forRemoval = false)
public class Dictionary implements Iterable<HasStringId>{
	protected Map<DictionaryCategory, Map<String,HasStringId>> dictionary=new HashMap<DictionaryCategory, Map<String,HasStringId>>();
	protected Map<Class<?>, Set<String>> categories=new HashMap<Class<?>, Set<String>>();
	
	
	public void add(HasStringId hasId){
		if (hasId instanceof HasCategories){
			Set<String> categories=((HasCategories)hasId).getCategories();
			if (categories!=null && categories.size()>0){
				for (String category : categories)
					put(new DictionaryCategory(hasId.getClass(),category),hasId);
			}
		}
		put(new DictionaryCategory(hasId.getClass()),hasId); //add to ALL category
	}

	private HasStringId put(DictionaryCategory category, HasStringId hasId){
		//categories
		Set<String> cat=categories.get(category.getClasse());
		if (cat==null){
			cat=new HashSet<String>();
			categories.put(category.getClasse(),cat);
		}
		cat.add(category.getCategory());
		
		//dictionary
		Map<String,HasStringId> map=dictionary.get(category);
		if (map==null){
			map=new HashMap<String, HasStringId>();
			dictionary.put(category,map);
		}
		return map.put(hasId.getId(),hasId);		
	}

	
	public HasStringId get(Class<?> classe, String id) {
		return get(new DictionaryCategory(classe), id);
	}
	
	public HasStringId get(DictionaryCategory category, String id) {
		Map<String,HasStringId> map=dictionary.get(category);
		if (map==null)
			return null;
		return map.get(id);
	}
	
	public Map<String,HasStringId> get(DictionaryCategory category) {
		return dictionary.get(category);
	}

	public Map<String,HasStringId> get(Class<?> classe) { //retrieve using class/ALL category
		return dictionary.get(new DictionaryCategory(classe));
	}

	
	public Set<String> getCategories(Class<?> classe){
		return categories.get(classe);
	}

	public int size() {
		return dictionary.size();
	}

	public boolean isEmpty() {
		return dictionary.isEmpty();
	}
	public boolean containsKey(DictionaryCategory category) {
		return dictionary.containsKey(category);
	}
	
	public void clear() {
		categories.clear();
		dictionary.clear();
		
	}

	public Set<DictionaryCategory> keySet() {
		return dictionary.keySet();
	}
	
	public Set<Class<?>> getClasses(){
		return categories.keySet();
	}
	
	public Class<?>[] getClassesAsArray(){
		Set<Class<?>> classes=getClasses();
		return classes.toArray(new Class<?>[classes.size()]);
	}
	
	public Iterator<HasStringId> iterator(DictionaryCategory category) {
		Map<String,HasStringId> map=dictionary.get(category);
		if (map==null)
			return new Iterator<HasStringId>() {
				@Override
				public boolean hasNext() {
					return false;
				}

				@Override
				public HasStringId next() {
					return null;
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}
			
			};
		return map.values().iterator();
	}
	
	@Override
	public Iterator<HasStringId> iterator() {
		return new Iterator<HasStringId>() {
			private Iterator<Map<String,HasStringId>> iterator1=dictionary.values().iterator();
			private Iterator<HasStringId> iterator2=null;
			@Override
			public boolean hasNext() {
				return iterator1.hasNext() 
						|| (iterator2!=null && iterator2.hasNext());
			}

			@Override
			public HasStringId next() {
				if (iterator2==null || !iterator2.hasNext()){
					Map<String,HasStringId> map=iterator1.next();
					if (map==null)
						throw new NoSuchElementException();
					iterator2=map.values().iterator();
				}
				return iterator2.next();
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		
		};
	}	
	
}
