package de.uniwue.web.view;

import java.util.Arrays;
import java.util.LinkedList;

/**
 * Display of connected lines from the original and revised document, inside the web view.
 */
public class ConnectedLines {
	
	private LinkedList<Line> original, revised;
	
	public ConnectedLines(LinkedList<Line> original, LinkedList<Line> revised) {
		this.original = original;
		this.revised = revised;
	}

	public ConnectedLines() {
		this(new LinkedList<Line>(), new LinkedList<Line>());
	}

	public long getBegin() {
		return original.getFirst().getBegin();
	}

	public long getEnd() {
		return original.getLast().getBegin();
	}

	public LinkedList<Line> getOriginal() {
		return original;
	}

	public LinkedList<Line> getRevised() {
		return revised;
	}

	public void addOriginalLines(Line... lines) {
		this.original.addAll(Arrays.asList(lines));
	}

	public void addRevisedLines(Line... lines) {
		this.revised.addAll(Arrays.asList(lines));
	}
	
	@Override
	public String toString() {
		String content = "";
		// Hope Java adds zip in the future
		int origSize = original.size();
		int revisedSize = revised.size();
		for(int i=0; i < origSize  || i < revisedSize; i++) {
			String lineOrig = i < origSize ? original.get(i).toString() : "";
			String lineRevised = i < revisedSize ? revised.get(i).toString() : "";
			content += String.format("%s | %s", lineOrig,lineRevised);
		}
	
		return content;
	}
}
