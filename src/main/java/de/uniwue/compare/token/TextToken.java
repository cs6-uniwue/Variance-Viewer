package de.uniwue.compare.token;

/**
 * Token wrapper class to compare tokens by their text content (ignoring
 * annotations)
 *
 */
public class TextToken extends Token {

	public TextToken(Token token) {
		super(token.getBegin(),token.getEnd(),token.getContent(),token.getAnnotations());
		this.highlight = token.highlight;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TextToken other = (TextToken) obj;
		if (content == null) {
			if (other.content != null)
				return false;
		} else if (!content.equals(other.content))
			return false;
		return true;
	}

}
