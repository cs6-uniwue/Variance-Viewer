package de.uniwue.compare;

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
}
