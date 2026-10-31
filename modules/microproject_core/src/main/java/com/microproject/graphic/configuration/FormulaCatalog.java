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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.microproject.configuration.NamedItem;
import com.microproject.field.InvalidFormulaException;
import com.microproject.strings.Messages;

/**
 * A named catalog of formula-backed items (action lists, cell styles) built from the XML
 * configuration.  The XML digester binds {@code id} and {@code name} by property name and the
 * item-adding method by name on the concrete subclass, so those names must stay public.
 */
public abstract class FormulaCatalog<T, F extends FormulaCatalog.Factory<T>> implements NamedItem {
	private static final Logger logger = Logger.getLogger(FormulaCatalog.class.getName());

	/** What a catalog needs from the factory that compiles one of its items. */
	public interface Factory<T> {
		String getId();
		String getFormulaText();
		T create() throws InvalidFormulaException;
	}

	String name = null;
	String id = null;
	private final Map<String, T> itemMap = new HashMap<>();
	private final Map<String, F> factoryMap = new HashMap<>();
	private final List<F> factories = new ArrayList<>();

	protected void register(F factory) {
		factories.add(factory);
		factoryMap.put(factory.getId(), factory);
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public void setId(String id) {
		this.id = id;
		setName(Messages.getString(id));
	}
	public String getId() {
		return id;
	}

	protected T resolve(String itemId) {
		T item = itemMap.get(itemId);
		if (item == null) {
			F factory = getFactory(itemId);
			try {
				item = factory.create();
				itemMap.put(itemId, item);
			} catch (InvalidFormulaException e) {
				logger.severe("Formula not set: invalid formula text: " + factory.getFormulaText());
			}
		}
		return item;
	}

	public F getFactory(String itemId) {
		return factoryMap.get(itemId);
	}
	public List<F> getFactories() {
		return factories;
	}
}
