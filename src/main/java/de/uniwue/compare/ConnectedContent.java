package de.uniwue.compare;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;

import de.uniwue.compare.token.Token;

/**
 * Connection between original and revised tokens.
 * Tokens are typically considered connected if they are equal or reference
 * the "same" text with changes inside
 */
public class ConnectedContent {
	private LinkedList<Token> original, revised;
	private final ContentType contentType;
	private VarianceType varianceType;

	public ConnectedContent(Token original, Token revised, ContentType contentType) {
		this(new LinkedList<Token>(Arrays.asList(original)),new LinkedList<Token>(Arrays.asList(revised)),contentType);
	}
	public ConnectedContent(LinkedList<Token> original, LinkedList<Token> revised,
			ContentType contentType) {
		this.original = new LinkedList<Token>();
		addOriginal(original);
		this.revised = revised;
		this.contentType = contentType;
		this.varianceType = contentType.equals(ContentType.EQUAL) ? VarianceType.NONE : VarianceType.CONTENT;
	}

	public ConnectedContent(LinkedList<Token> equalContent) {
		if(equalContent.size() == 0)
			throw new IllegalArgumentException("Can't create an empty connected content of type equal");
		this.original = new LinkedList<Token>();
		addOriginal(equalContent);
		this.contentType = ContentType.EQUAL;
		this.varianceType = VarianceType.NONE;
	}

	public ConnectedContent(ContentType type) {
		this(new LinkedList<Token>(), new LinkedList<Token>(), type);
	}

	public LinkedList<Token> getOriginal() {
		if(original != null)
			return new LinkedList<Token>(original);
		else
			return new LinkedList<Token>();
	}

	public LinkedList<Token> getRevised() {
		if(revised != null)
			return new LinkedList<Token>(revised);
		else
			return new LinkedList<Token>();
	}

	public ContentType getContentType() {
		return contentType;
	}

	public VarianceType getVarianceType() {
		return varianceType;
	}

	public void setVarianceType(VarianceType varianceType) {
		this.varianceType = varianceType;
	}

	public void addOriginal(Collection<Token> original) {
		this.original.addAll(original);
	}

	public void addOriginal(Token... original) {
		this.original.addAll(Arrays.asList(original));
	}

	public void addRevised(Collection<Token> revised) {
		if (contentType.equals(ContentType.EQUAL))
			this.original.addAll(revised);
		this.revised.addAll(revised);
	}

	public void addRevised(Token... revised) {
		this.addRevised(Arrays.asList(revised));
	}

	public String getOriginalAsText() {
		String toString = "";
		long lastEnd = original.size() > 0 ? original.getFirst().getBegin() : 0;
		for (Token token : original) {
			if (token.getBegin() < token.getEnd()) {
				toString += token.getContent();
				if (lastEnd < token.getBegin())
					toString += " ";
				lastEnd = token.getEnd();
			}
		}

		return toString;
	}

	public String getRevisedAsText() {
		String toString = "";
		long lastEnd = revised.size() > 0 ? revised.getFirst().getBegin() : 0;
		for (Token token : revised) {
			if (token.getBegin() < token.getEnd()) {
				if (lastEnd < token.getBegin())
					toString += " ";
				toString += token.getContent();
				lastEnd = token.getEnd();
			}
		}
		return toString;
	}

	@Override
	public String toString() {
		return "[" + contentType +" | " + varianceType + ": \'" + original + "\', \'" + revised + "\']";
	}
}
