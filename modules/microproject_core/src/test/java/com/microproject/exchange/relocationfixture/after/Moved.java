/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.exchange.relocationfixture.after;

import java.io.Serializable;

/** The same class as {@code relocationfixture.before.Moved}, after a package move. */
public class Moved implements Serializable {
	private static final long serialVersionUID = 7L;

	public String name;
	public Part part;

	public static class Part implements Serializable {
		private static final long serialVersionUID = 8L;

		public int value;
	}
}
