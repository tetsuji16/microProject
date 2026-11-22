/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.exchange.relocationfixture.before;

import java.io.Serializable;

/** Shape of a persisted class before a package move. */
public class Moved implements Serializable {
	private static final long serialVersionUID = 7L;

	public String name;
	public Part part;

	public static class Part implements Serializable {
		private static final long serialVersionUID = 8L;

		public int value;
	}
}
