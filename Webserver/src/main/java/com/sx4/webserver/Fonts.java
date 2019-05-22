package com.sx4.webserver;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.File;
import java.io.IOException;

public class Fonts {
	
	public static final String FONT_PATH = "resources/fonts/";
	
	public static final Font GOTHAM_BLACK;
	public static final Font GOTHAM_BOOK;
	public static final Font GOTHAM_BOLD;
	public static final Font SEGOE_UI;
	
	public static final Font WHITNEY_MEDIUM;
	public static final Font WHITNEY_LIGHT;
	public static final Font WHITNEY_BOOK;
	
	public static final Font UNI_SANS;
	
	public static final Font EXO_REGULAR;
	
	static {
		try {
			WHITNEY_MEDIUM = Font.createFont(Font.TRUETYPE_FONT, new File(FONT_PATH + "whitney/Whitney-Medium.ttf"));
			WHITNEY_LIGHT = Font.createFont(Font.TRUETYPE_FONT, new File(FONT_PATH + "whitney/WhitneyLight.ttf"));
			WHITNEY_BOOK = Font.createFont(Font.TRUETYPE_FONT, new File(FONT_PATH + "whitney/whitney-book.otf"));
			
			GOTHAM_BLACK = Font.createFont(Font.TRUETYPE_FONT, new File(FONT_PATH + "gotham/Gotham-Black.otf"));
			GOTHAM_BOOK = Font.createFont(Font.TRUETYPE_FONT, new File(FONT_PATH + "gotham/GothamBook.ttf"));
			GOTHAM_BOLD =  Font.createFont(Font.TRUETYPE_FONT, new File(FONT_PATH + "gotham/GothamBold.ttf"));
			
			SEGOE_UI = Font.createFont(Font.TRUETYPE_FONT, new File(FONT_PATH + "segoeuireg.ttf"));
			
			UNI_SANS = Font.createFont(Font.TRUETYPE_FONT, new File(FONT_PATH + "uni-sans.otf"));
			
			EXO_REGULAR = Font.createFont(Font.TRUETYPE_FONT, new File(FONT_PATH + "exo.regular.otf"));
		}catch(FontFormatException | IOException e) {
			throw new RuntimeException("Failed to load fonts", e);
		}
	}
}