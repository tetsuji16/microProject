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
import java.util.HashMap;
import java.util.Map;


/**
 * These are standard colors that are found all over the internet
 */
public class Colors {
	public static final Color ALICE_BLUE = new Color(0xf0f8ff);
	public static final Color ANTIQUE_WHITE = new Color(0xfaebd7);
	public static final Color AQUAMARINE = new Color(0x7fffd4);
	public static final Color AZURE = new Color(0xf0ffff);
	public static final Color BEIGE = new Color(0xf5f5dc);
	public static final Color BISQUE = new Color(0xffe4c4);
	public static final Color BLACK = new Color(0x000000);
	public static final Color BLANCHED_ALMOND = new Color(0xffebcd);
	public static final Color BLUE = new Color(0x0000ff);
	public static final Color BLUE_VIOLET = new Color(0x8a2be2);
	public static final Color BROWN = new Color(0xa52a2a);
	public static final Color BURLY_WOOD = new Color(0xdeb887);
	public static final Color CADET_BLUE = new Color(0x5f9ea0);
	public static final Color CHARTREUSE = new Color(0x7fff00);
	public static final Color CHOCOLATE = new Color(0xd2691e);
	public static final Color CORAL = new Color(0xff7f50);
	public static final Color CORNFLOWER_BLUE = new Color(0x6495ed);
	public static final Color CORNSILK = new Color(0xfff8dc);
	public static final Color CYAN = new Color(0x00ffff);
	public static final Color DARK_GOLDENROD = new Color(0xb8860b);
	public static final Color DARK_GREEN = new Color(0x006400);
	public static final Color DARK_KHAKI = new Color(0xbdb76b);
	public static final Color DARK_OLIVE_GREEN = new Color(0x556b2f);
	public static final Color DARK_ORANGE = new Color(0xff8c00);
	public static final Color DARK_ORCHID = new Color(0x9932cc);
	public static final Color DARK_SALMON = new Color(0xe9967a);
	public static final Color DARK_SEA_GREEN = new Color(0x8fbc8f);
	public static final Color DARK_SLATE_BLUE = new Color(0x483d8b);
	public static final Color DARK_SLATE_GRAY = new Color(0x2f4f4f);
	public static final Color DARK_TURQUOISE = new Color(0x00ced1);
	public static final Color DARK_VIOLET = new Color(0x9400d3);
	public static final Color DEEP_PINK = new Color(0xff1493);
	public static final Color DEEP_SKY_BLUE = new Color(0x00bfff);
	public static final Color DIM_GRAY = new Color(0x696969);
	public static final Color DODGER_BLUE = new Color(0x1e90ff);
	public static final Color FIRE_BRICK = new Color(0xb22222);
	public static final Color FLORAL_WHITE = new Color(0xfffaf0);
	public static final Color FOREST_GREEN = new Color(0x228b22);
	public static final Color GREEN = new Color(0x00ff00);
	public static final Color GAINSBORO = new Color(0xdcdcdc);
	public static final Color GHOST_WHITE = new Color(0xf8f8ff);
	public static final Color GOLD = new Color(0xffd700);
	public static final Color GOLDENROD = new Color(0xdaa520);
	public static final Color GRAY = new Color(0xbebebe);
	public static final Color HONEYDEW = new Color(0xf0fff0);
	public static final Color HOT_PINK = new Color(0xff69b4);
	public static final Color INDIAN_RED = new Color(0xcd5c5c);
	public static final Color IVORY = new Color(0xfffff0);
	public static final Color KHAKI = new Color(0xf0e68c);
	public static final Color LAVENDER = new Color(0xe6e6fa);
	public static final Color LAVENDER_BLUSH = new Color(0xfff0f5);
	public static final Color LAWN_GREEN = new Color(0x7cfc00);
	public static final Color LEMON_CHIFFON = new Color(0xfffacd);
	public static final Color LIGHT_BLUE = new Color(0xadd8e6);
	public static final Color LIGHT_CORAL = new Color(0xf08080);
	public static final Color LIGHT_CYAN = new Color(0xe0ffff);
	public static final Color LIGHT_GOLDENROD = new Color(0xeedd82);
	public static final Color LIGHT_GOLDENROD_YELLOW = new Color(0xfafad2);
	public static final Color LIGHT_GRAY = new Color(0xd3d3d3);
	public static final Color LIGHT_GREY = new Color(0xd3d3d3);
	public static final Color LIGHT_PINK = new Color(0xffb6c1);
	public static final Color LIGHT_SALMON = new Color(0xffa07a);
	public static final Color LIGHT_SEA_GREEN = new Color(0x20b2aa);
	public static final Color LIGHT_SKY_BLUE = new Color(0x87cefa);
	public static final Color LIGHT_SLATE_BLUE = new Color(0x8470ff);
	public static final Color LIGHT_SLATE_GRAY = new Color(0x778899);
	public static final Color LIGHT_SLATE_GREY = new Color(0x778899);
	public static final Color LIGHT_STEEL_BLUE = new Color(0xb0c4de);
	public static final Color LIGHT_YELLOW = new Color(0xffffe0);
	public static final Color LIME_GREEN = new Color(0x32cd32);
	public static final Color LINEN = new Color(0xfaf0e6);
	public static final Color MAGENTA = new Color(0xff00ff);
	public static final Color MAROON = new Color(0xb03060);
	public static final Color MEDIUM_AQUAMARINE = new Color(0x66cdaa);
	public static final Color MEDIUM_BLUE = new Color(0x0000cd);
	public static final Color MEDIUM_ORCHID = new Color(0xba55d3);
	public static final Color MEDIUM_PURPLE = new Color(0x9370db);
	public static final Color MEDIUM_SEA_GREEN = new Color(0xcb371);
	public static final Color MEDIUM_SLATE_BLUE = new Color(0x7b68ee);
	public static final Color MEDIUM_SPRING_GREEN = new Color(0x00fa9a);
	public static final Color MEDIUM_TURQUOISE = new Color(0x48d1cc);
	public static final Color MEDIUM_VIOLET_RED = new Color(0xc71585);
	public static final Color MIDNIGHT_BLUE = new Color(0x191970);
	public static final Color MINT_CREAM = new Color(0xf5fffa);
	public static final Color MISTY_ROSE = new Color(0xffe4e1);
	public static final Color MOCCASIN = new Color(0xffe4b5);
	public static final Color NAVAJO_WHITE = new Color(0xffdead);
	public static final Color NAVY = new Color(0x000080);
	public static final Color NAVY_BLUE = new Color(0x000080);
	public static final Color OLD_LACE = new Color(0xfdf5e6);
	public static final Color OLIVE_DRAB = new Color(0x6b8e23);
	public static final Color ORANGE = new Color(0xffa500);
	public static final Color ORANGE_RED = new Color(0xff4500);
	public static final Color ORCHID = new Color(0xda70d6);
	public static final Color PALE_GOLDENROD = new Color(0xeee8aa);
	public static final Color PALE_GREEN = new Color(0x98fb98);
	public static final Color PALE_YELLOW = new Color(0xfbfb56);
	public static final Color PALE_TURQUOISE = new Color(0xafeeee);
	public static final Color PALE_VIOLET_RED = new Color(0xdb7093);
	public static final Color PAPAYA_WHIP = new Color(0xffefd5);
	public static final Color PEACH_PUFF = new Color(0xffdab9);
	public static final Color PERU = new Color(0xcd853f);
	public static final Color PINK = new Color(0xffc0cb);
	public static final Color PLUM = new Color(0xdda0dd);
	public static final Color POWDER_BLUE = new Color(0xb0e0e6);
	public static final Color PURPLE = new Color(0xa020f0);
	public static final Color RED = new Color(0xff0000);
	public static final Color ROSY_BROWN = new Color(0xbc8f8f);
	public static final Color ROYAL_BLUE = new Color(0x4169e1);
	public static final Color SADDLE_BROWN = new Color(0x8b4513);
	public static final Color SALMON = new Color(0xfa8072);
	public static final Color SANDY_BROWN = new Color(0xf4a460);
	public static final Color SEA_GREEN = new Color(0x2e8b57);
	public static final Color SEASHELL = new Color(0xfff5ee);
	public static final Color SIENNA = new Color(0xa0522d);
	public static final Color SKY_BLUE = new Color(0x87ceeb);
	public static final Color SLATE_BLUE = new Color(0x6a5acd);
	public static final Color SLATE_GRAY = new Color(0x708090);
	public static final Color SNOW = new Color(0xfffafa);
	public static final Color SPRING_GREEN = new Color(0x00ff7f);
	public static final Color STEEL_BLUE = new Color(0x4682b4);
	public static final Color TAN = new Color(0xd2b48c);
	public static final Color THISTLE = new Color(0xd8bfd8);
	public static final Color TOMATO = new Color(0xff6347);
	public static final Color TURQUOISE = new Color(0x40e0d0);
	public static final Color VIOLET = new Color(0xee82ee);
	public static final Color VIOLET_RED = new Color(0xd02090);
	public static final Color WHEAT = new Color(0xf5deb3);
	public static final Color WHITE = new Color(0xffffff);
	public static final Color WHITE_SMOKE = new Color(0xf5f5f5);
	public static final Color YELLOW = new Color(0xffff00);
	public static final Color YELLOW_GREEN = new Color(0x9acd32);

	// A few added for ProjectLibre
	public static final Color NORMAL_YELLOW = new Color(0xffffb0);
	public static final Color NORMAL_LIGHT_YELLOW = new Color(0xffffe0);
	public static final Color MULTIPROJET1 = new Color(0xececec);
	public static final Color MULTIPROJET0 = new Color(0xdcecec);
	public static final Color VERY_LIGHT_GRAY = new Color(0xEAEAEA);
	public static final Color NOT_TOO_DARK_GRAY=new Color(120,120,120);
	public static final Color MONDAY_DONE = new Color(0x00C875);
	public static final Color MONDAY_WORKING_ON_IT = new Color(0xFDAB3D);
	public static final Color MONDAY_STUCK = new Color(0xE2445C);
	public static final Color MONDAY_NOT_STARTED = new Color(0xC4C4C4);
	public static final Color MONDAY_GROUP_A = new Color(0x579BFC);
	public static final Color MONDAY_GROUP_B = new Color(0xA25DDC);
	public static final Color MONDAY_BACKGROUND = new Color(0xF7F7F7);
	public static final Color MONDAY_HEADER_BACKGROUND = new Color(0xF5F6F8);
	public static final Color MONDAY_GRID_LINE = new Color(0xE1E1E1);
	public static final Color MONDAY_DONE_BG = new Color(0x00, 0xC8, 0x75, 76);
	public static final Color MONDAY_WORKING_ON_IT_BG = new Color(0xFD, 0xAB, 0x3D, 76);
	public static final Color MONDAY_STUCK_BG = new Color(0xE2, 0x44, 0x5C, 76);
	public static final Color MONDAY_NOT_STARTED_BG = new Color(0xC4, 0xC4, 0xC4, 76);
	public static final Color MONDAY_GROUP_A_BG = new Color(0x57, 0x9B, 0xFC, 76);
	public static final Color MONDAY_GROUP_B_BG = new Color(0xA2, 0x5D, 0xDC, 76);

  private static Object[][] data = {
	  {"ALICE_BLUE",ALICE_BLUE},
	  {"ANTIQUE_WHITE",ANTIQUE_WHITE},
	  {"AQUAMARINE",AQUAMARINE},
	  {"AZURE",AZURE},
	  {"BEIGE",BEIGE},
	  {"BISQUE",BISQUE},
	  {"BLACK",BLACK},
	  {"BLANCHED_ALMOND",BLANCHED_ALMOND},
	  {"BLUE",BLUE},
	  {"BLUE_VIOLET",BLUE_VIOLET},
	  {"BROWN",BROWN},
	  {"BURLY_WOOD",BURLY_WOOD},
	  {"CADET_BLUE",CADET_BLUE},
	  {"CHARTREUSE",CHARTREUSE},
	  {"CHOCOLATE",CHOCOLATE},
	  {"CORAL",CORAL},
	  {"CORNFLOWER_BLUE",CORNFLOWER_BLUE},
	  {"CORNSILK",CORNSILK},
	  {"CYAN",CYAN},
	  {"DARK_GOLDENROD",DARK_GOLDENROD},
	  {"DARK_GREEN",DARK_GREEN},
	  {"DARK_KHAKI",DARK_KHAKI},
	  {"DARK_OLIVE_GREEN",DARK_OLIVE_GREEN},
	  {"DARK_ORANGE",DARK_ORANGE},
	  {"DARK_ORCHID",DARK_ORCHID},
	  {"DARK_SALMON",DARK_SALMON},
	  {"DARK_SEA_GREEN",DARK_SEA_GREEN},
	  {"DARK_SLATE_BLUE",DARK_SLATE_BLUE},
	  {"DARK_SLATE_GRAY",DARK_SLATE_GRAY},
	  {"DARK_TURQUOISE",DARK_TURQUOISE},
	  {"DARK_VIOLET",DARK_VIOLET},
	  {"DEEP_PINK",DEEP_PINK},
	  {"DEEP_SKY_BLUE",DEEP_SKY_BLUE},
	  {"DIM_GRAY",DIM_GRAY},
	  {"DODGER_BLUE",DODGER_BLUE},
	  {"FIRE_BRICK",FIRE_BRICK},
	  {"FLORAL_WHITE",FLORAL_WHITE},
	  {"FOREST_GREEN",FOREST_GREEN},
	  {"GREEN",GREEN},
	  {"GAINSBORO",GAINSBORO},
	  {"GHOST_WHITE",GHOST_WHITE},
	  {"GOLD",GOLD},
	  {"GOLDENROD",GOLDENROD},
	  {"GRAY",GRAY},
	  {"HONEYDEW",HONEYDEW},
	  {"HOT_PINK",HOT_PINK},
	  {"INDIAN_RED",INDIAN_RED},
	  {"IVORY",IVORY},
	  {"KHAKI",KHAKI},
	  {"LAVENDER",LAVENDER},
	  {"LAVENDER_BLUSH",LAVENDER_BLUSH},
	  {"LAWN_GREEN",LAWN_GREEN},
	  {"LEMON_CHIFFON",LEMON_CHIFFON},
	  {"LIGHT_BLUE",LIGHT_BLUE},
	  {"LIGHT_CORAL",LIGHT_CORAL},
	  {"LIGHT_CYAN",LIGHT_CYAN},
	  {"LIGHT_GOLDENROD",LIGHT_GOLDENROD},
	  {"LIGHT_GOLDENROD_YELLOW",LIGHT_GOLDENROD_YELLOW},
	  {"LIGHT_GRAY",LIGHT_GRAY},
	  {"LIGHT_GREY",LIGHT_GREY},
	  {"LIGHT_PINK",LIGHT_PINK},
	  {"LIGHT_SALMON",LIGHT_SALMON},
	  {"LIGHT_SEA_GREEN",LIGHT_SEA_GREEN},
	  {"LIGHT_SKY_BLUE",LIGHT_SKY_BLUE},
	  {"LIGHT_SLATE_BLUE",LIGHT_SLATE_BLUE},
	  {"LIGHT_SLATE_GRAY",LIGHT_SLATE_GRAY},
	  {"LIGHT_SLATE_GREY",LIGHT_SLATE_GREY},
	  {"LIGHT_STEEL_BLUE",LIGHT_STEEL_BLUE},
	  {"LIGHT_YELLOW",LIGHT_YELLOW},
	  {"LIME_GREEN",LIME_GREEN},
	  {"LINEN",LINEN},
	  {"MAGENTA",MAGENTA},
	  {"MAROON",MAROON},
	  {"MEDIUM_AQUAMARINE",MEDIUM_AQUAMARINE},
	  {"MEDIUM_BLUE",MEDIUM_BLUE},
	  {"MEDIUM_ORCHID",MEDIUM_ORCHID},
	  {"MEDIUM_PURPLE",MEDIUM_PURPLE},
	  {"MEDIUM_SEA_GREEN",MEDIUM_SEA_GREEN},
	  {"MEDIUM_SLATE_BLUE",MEDIUM_SLATE_BLUE},
	  {"MEDIUM_SPRING_GREEN",MEDIUM_SPRING_GREEN},
	  {"MEDIUM_TURQUOISE",MEDIUM_TURQUOISE},
	  {"MEDIUM_VIOLET_RED",MEDIUM_VIOLET_RED},
	  {"MIDNIGHT_BLUE",MIDNIGHT_BLUE},
	  {"MINT_CREAM",MINT_CREAM},
	  {"MISTY_ROSE",MISTY_ROSE},
	  {"MOCCASIN",MOCCASIN},
	  {"NAVAJO_WHITE",NAVAJO_WHITE},
	  {"NAVY",NAVY},
	  {"NAVY_BLUE",NAVY_BLUE},
	  {"OLD_LACE",OLD_LACE},
	  {"OLIVE_DRAB",OLIVE_DRAB},
	  {"ORANGE",ORANGE},
	  {"ORANGE_RED",ORANGE_RED},
	  {"ORCHID",ORCHID},
	  {"PALE_GOLDENROD",PALE_GOLDENROD},
	  {"PALE_GREEN",PALE_GREEN},
	  {"PALE_TURQUOISE",PALE_TURQUOISE},
	  {"PALE_VIOLET_RED",PALE_VIOLET_RED},
	  {"PAPAYA_WHIP",PAPAYA_WHIP},
	  {"PEACH_PUFF",PEACH_PUFF},
	  {"PERU",PERU},
	  {"PINK",PINK},
	  {"PLUM",PLUM},
	  {"POWDER_BLUE",POWDER_BLUE},
	  {"PURPLE",PURPLE},
	  {"RED",RED},
	  {"ROSY_BROWN",ROSY_BROWN},
	  {"ROYAL_BLUE",ROYAL_BLUE},
	  {"SADDLE_BROWN",SADDLE_BROWN},
	  {"SALMON",SALMON},
	  {"SANDY_BROWN",SANDY_BROWN},
	  {"SEA_GREEN",SEA_GREEN},
	  {"SEASHELL",SEASHELL},
	  {"SIENNA",SIENNA},
	  {"SKY_BLUE",SKY_BLUE},
	  {"SLATE_BLUE",SLATE_BLUE},
	  {"SLATE_GRAY",SLATE_GRAY},
	  {"SNOW",SNOW},
	  {"SPRING_GREEN",SPRING_GREEN},
	  {"STEEL_BLUE",STEEL_BLUE},
	  {"TAN",TAN},
	  {"THISTLE",THISTLE},
	  {"TOMATO",TOMATO},
	  {"TURQUOISE",TURQUOISE},
	  {"VIOLET",VIOLET},
	  {"VIOLET_RED",VIOLET_RED},
	  {"WHEAT",WHEAT},
	  {"WHITE",WHITE},
	  {"WHITE_SMOKE",WHITE_SMOKE},
	  {"YELLOW",YELLOW},
	  {"YELLOW_GREEN",YELLOW_GREEN},
	  {"NORMAL_YELLOW",NORMAL_YELLOW},
	  {"NORMAL_LIGHT_YELLOW",NORMAL_LIGHT_YELLOW},
	  {"MULTIPROJET1",MULTIPROJET1},
	  {"MULTIPROJET0",MULTIPROJET0},
	  {"VERY_LIGHT_GRAY",VERY_LIGHT_GRAY},
	  {"MONDAY_DONE",MONDAY_DONE},
	  {"MONDAY_WORKING_ON_IT",MONDAY_WORKING_ON_IT},
	  {"MONDAY_STUCK",MONDAY_STUCK},
	  {"MONDAY_NOT_STARTED",MONDAY_NOT_STARTED},
	  {"MONDAY_GROUP_A",MONDAY_GROUP_A},
	  {"MONDAY_GROUP_B",MONDAY_GROUP_B},
	  {"MONDAY_BACKGROUND",MONDAY_BACKGROUND},
	  {"MONDAY_HEADER_BACKGROUND",MONDAY_HEADER_BACKGROUND},
	  {"MONDAY_GRID_LINE",MONDAY_GRID_LINE},
	  {"MONDAY_DONE_BG",MONDAY_DONE_BG},
	  {"MONDAY_WORKING_ON_IT_BG",MONDAY_WORKING_ON_IT_BG},
	  {"MONDAY_STUCK_BG",MONDAY_STUCK_BG},
	  {"MONDAY_NOT_STARTED_BG",MONDAY_NOT_STARTED_BG},
	  {"MONDAY_GROUP_A_BG",MONDAY_GROUP_A_BG},
	  {"MONDAY_GROUP_B_BG",MONDAY_GROUP_B_BG}
 };

	private static volatile Map<String, Color> colorMap;

	public static Map<String, Color> getColors() {
		Map<String, Color> result = colorMap;
		if (result == null) {
			synchronized (Colors.class) {
				result = colorMap;
				if (result == null) {
					Map<String, Color> m = new HashMap<>();
					for (int i = 0; i < data.length; i++) {
						Object row[] = data[i];
						m.put((String) row[0], (Color) row[1]);
					}
					result = Map.copyOf(m);
					colorMap = result;
				}
			}
		}
		return result;
	}

	public static Color findColor(String key) {
		Color found = (Color) getColors().get(key);
		if (found == null) // if not a standard color, then assume that it needs to be parsed by color
			found = Color.getColor(key);
		return found;
	}

}

