package de.uniwue.web.view;

import java.util.Arrays;
import java.util.LinkedList;

/**
 * A Line that is to be displayed in the web view 
 */
public class Line {
	private int lineNr = 0;
	private LinkedList<Content> tokens;

	public Line(int lineNr, Content... content) {
		this.lineNr = lineNr;
		this.tokens = new LinkedList<Content>();
		this.tokens.addAll(Arrays.asList(content));
	}

	public void addContent(Content... content) {
		this.tokens.addAll(Arrays.asList(content));
	}

	public long getBegin() {
		return tokens.getFirst().getBegin();
	}

	public long getEnd() {
		return tokens.getLast().getEnd();
	}

	public long getContentLength() {
		return this.getEnd() - this.getBegin();
	}

	public LinkedList<Content> getContent() {
		return tokens;
	}

	public int getLineNr() {
		return lineNr;
	}

	@Override
	public String toString() {
		return String.format("[%d:%s]", lineNr, tokens.stream().map((a) -> a.getContent()).reduce("",(a,b) -> a+" "+b));
	}
}