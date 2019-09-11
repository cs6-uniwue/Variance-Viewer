package de.uniwue.compare;

import org.apache.commons.lang3.ArrayUtils;

public class SpecialCharacter {
	/**
	 * Tokens are separated by spaces, but Java does not include 
	 * all types of spaces in regex '\s'.
	 * This array consists of all types of spaces derived from
	 * http://jkorpela.fi/chars/spaces.html
	 */
	public static final String[] SPACES = new String[] {
			"\u0020", 	// SPACE
			"\u00A0",	// NO-BREAK SPACE
			"\u1680",	// OGHAM SPACE MARK
			"\u180E",	// MONGOLIAN VOWEL SEPARATOR
			"\u2000",	// EN QUAD
			"\u2001",	// EM QUAD
			"\u2002",	// EN SPACE (nut)
			"\u2003",	// EM SPACE (mutton)
			"\u2004",	// THREE-PER-EM SPACE (thick space)
			"\u2005",	// FOUR-PER-EM SPACE (mid space)
			"\u2006",	// SIX-PER-EM SPACE
			"\u2007",	// FIGURE SPACE
			"\u2008",	// PUNCTUATION SPACE
			"\u2009",	// THIN SPACE
			"\u200A",	// HAIR SPACE
			"\u200B",	// ZERO WIDTH SPACE
			"\u202F",	// NARROW NO-BREAK SPACE
			"\u205F",	// MEDIUM MATHEMATICAL SPACE
			"\u3000",	// IDEOGRAPHIC SPACE
			"\uFEFF"	// ZERO WIDTH NO-BREAK SPACE
	};
	/**
	 * Regex group of all SPACES. 
	 * Can be used to test if a character is a space e.g. 'character.matches(SPACES_REGEX)'
	 */
	public static final String SPACES_REGEX = "["+String.join("", SPACES)+"]";
	
	/**
	 * Lines are separated by line breaks, but Java does not include 
	 * all types of line breaks in regex '\s'.
	 * This array consists of all types of line breaks derived from
	 * https://en.wikipedia.org/wiki/Whitespace_character
	 */
	public static final String[] LINE_BREAKS = new String[] {
			"\n",		// LINE FEED
			"\u000B",	// LINE TABULATION
			"\u000C",	// FORM FEED
			"\r",		// CARRIAGE RETURN
			"\u0085",	// NEXT LINE
			"\u2028",	// LINE SEPARATOR
			"\u2029"	// PARAGRAPH SEPARATOR
	};
	/**
	 * Regex group of all LINE_BREAKS. 
	 * Can be used to test if a character is a space e.g. 'character.matches(LINE_BREAKS_REGEX)'
	 */
	public static final String LINE_BREAKS_REGEX = "["+String.join("", LINE_BREAKS)+"]";
	
	/**
	 * This array consists of all types of whitespaces derived from
	 * http://jkorpela.fi/chars/spaces.html
	 * https://en.wikipedia.org/wiki/Whitespace_character
	 */
	public static final String[] WHITESPACES = ArrayUtils.addAll(SPACES, LINE_BREAKS);
	/**
	 * Regex group of all WHITESPACES. 
	 * Can be used to test if a character is a space e.g. 'character.matches(WHITESPACES_REGEX)'
	 */
	public static final String WHITESPACES_REGEX = "["+String.join("", WHITESPACES)+"]";
}
