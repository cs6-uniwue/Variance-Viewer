package de.uniwue.compare.token;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.SortedSet;
import java.util.TreeSet;

import de.uniwue.compare.variance.types.Variance;

/**
 * Token class to represent the smallest unit in a text to compare.
 * A token is considered to be any text separated by whitespace.
 * Tokens are represented by their position in the text, its content, 
 * possible annotations and in which contentTag they appear in.
 * (Every token in PLAINTEXT has the same contentTag)
 * 
 * Contents can be highlighted to represent changes inside the token.
 */
public class Token {

	protected int begin, end;
	protected String content;
	protected String contentTag;
	protected SortedSet<String> annotations;
	protected List<long[]> highlight;

	public Token(int begin, int end, String content, String contentTag, SortedSet<String> annotations) {
		this.begin = begin;
		this.end = end;
		this.content = content;
		this.contentTag = contentTag;
		this.annotations = new TreeSet<String>(annotations);
	}

	public Token(int begin, int end, String content, String contentTag) {
		this(begin,end,content,contentTag,new TreeSet<>());
	}

	public Token(Token token) {
		this(token.getBegin(), token.getEnd(), token.getContent(), token.getContentTag(), token.getAnnotations());
		this.highlight = token.highlight;
	}

	public int getBegin() {
		return begin;
	}

	public int getEnd() {
		return end;
	}

	public String getContent() {
		return content;
	}
	
	public String getContentTag() {
		return contentTag;
	}
	
	public void moveDelta(int delta) {
		begin += delta;
		end += delta;
	}

	public void addAnnotation(String annotation) {
		annotations.add(annotation);
	}

	public SortedSet<String> getAnnotations() {
		return new TreeSet<String>(annotations);
	}

	public String getAnnotationsString() {
		final Queue<String> annotation_q = new LinkedList<String>(annotations);
		String toString = "";
		while(!annotation_q.isEmpty()) {
			final String annotation = annotation_q.remove();
			toString += annotation;
			if(!annotation_q.isEmpty())
				toString += " ";
		}
		return toString;
	}

	public boolean hasAnnotations() {
		return !annotations.isEmpty();
	}
	
	public List<long[]> getHighlight() {
		if(highlight == null || highlight.size() == 0)
			return Arrays.asList(new long[]{0,0});
		return highlight;
	}
	
	public void highlightEverything() {
		this.highlight = Arrays.asList(new long[]{0,content.length()});
	}
	
	public void setHighlight(List<long[]> highlight) {
		this.highlight = highlight;
	}

	/**
	 * Create a token with the same characteristics than this token in regards of
	 * text and annotations. Equals considers everything.
	 * 
	 * @return
	 */
	public Token getDefaultToken() {
		return new Token(this);
	}

	/**
	 * Create a token with the same characteristics than this token in regards of
	 * text. Equals ignores annotations (keeps annotations).
	 * 
	 * @return
	 */
	public TextToken getTextToken() {
		return new TextToken(this);
	}

	/**
	 * Create a token with the same characteristics than this token in regards of
	 * annotations. Equals ignores Text (keeps text).
	 * 
	 * @return
	 */
	public AnnotationToken getAnnotationToken() {
		return new AnnotationToken(this);
	}

	/**
	 * Create a token with the same characteristics than this token in regards of
	 * variance. Equals ignores Variances.
	 * 
	 * @return
	 */
	public VarianceToken getVarianceToken(List<Variance> variances) {
		return new VarianceToken(this, variances);
	}

	@Override
	public String toString() {
		return String.format("['%s',%d,%d,%s,[%s]]",
							content, begin, end, contentTag, String.join(",", annotations));
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
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
