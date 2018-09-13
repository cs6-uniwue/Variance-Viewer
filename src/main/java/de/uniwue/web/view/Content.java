package de.uniwue.web.view;

import java.util.List;

import de.uniwue.compare.ContentType;
import de.uniwue.compare.VarianceType;
import de.uniwue.compare.token.Token;

public class Content extends Token {

	private final ContentType contentType;
	private final VarianceType varianceType;

	public Content(int begin, int end, String content, ContentType contentType, VarianceType varianceType, List<long[]> highlight) {
		super(begin, end, content);
		this.contentType = contentType;
		this.varianceType = varianceType;
		this.highlight = highlight;
	}

	public Content(Token token, ContentType contentType, VarianceType varianceType, List<long[]> highlight) {
		this(token.getBegin(), token.getEnd(), token.getContent(), contentType, varianceType, highlight);
		this.addAnnotation(token.getAnnotationsString());
	}

	public ContentType getContentType() {
		return contentType;
	}

	public VarianceType getVarianceType() {
		return varianceType;
	}
}
