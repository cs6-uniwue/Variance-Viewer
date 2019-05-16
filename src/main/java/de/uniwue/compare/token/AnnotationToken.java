package de.uniwue.compare.token;

/**
 * Token wrapper class to compare tokens by their annotation content (ignoring
 * text)
 *
 */
public class AnnotationToken extends Token {

	public AnnotationToken(Token token) {
		super(token.getBegin(), token.getEnd(), token.getContent(), token.getContentTag(), token.getAnnotations());
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
		Token other = (Token) obj;
		if(!other.contentTag.contentEquals(contentTag)) 
			return false;
		if (annotations == null) {
			if (other.annotations != null)
				return false;
		} else if (!annotations.equals(other.annotations))
			return false;
		if (content == null) {
			if (other.content != null)
				return false;
		} else if (!content.equals(other.content))
			return false;
		return true;
	}

}
