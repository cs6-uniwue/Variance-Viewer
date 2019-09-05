package de.uniwue.web.view;

import java.util.Arrays;
import java.util.LinkedList;

/**
 * Display of connected lines from the original and revised document, inside the web view.
 */
public class ConnectedLines {
	
	private LinkedList<Line> original, revised;
	private int curOriginalLineNr, curRevisedLineNr;
	private boolean lockOrig, lockRev;
	
	public ConnectedLines(int originalLineNr, int revisedLineNr) {
		this.curOriginalLineNr = originalLineNr;
		this.curRevisedLineNr = revisedLineNr;
		this.original = new LinkedList<Line>();
		this.revised = new LinkedList<Line>();
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

	private void addOriginalLines(Line... lines) {
		this.original.addAll(Arrays.asList(lines));
	}

	private void addRevisedLines(Line... lines) {
		this.revised.addAll(Arrays.asList(lines));
	}
	
	public void endOriginalLine() {
		this.lockOrig = true;
	}
	
	public void endRevisedLine() {
		this.lockRev = true;
	}
	
	public void addOriginalContent(Content content) {
		if (original.isEmpty() || this.lockOrig) { 
			this.addOriginalLines(new Line(++curOriginalLineNr));
			this.lockOrig = false;
		}
		original.getLast().addContent(content);
	}
	
	public void addRevisedContent(Content content) {
		if (revised.isEmpty() || this.lockRev) {
			this.addRevisedLines(new Line(++curRevisedLineNr));
			this.lockRev = false;
		}
		revised.getLast().addContent(content);
	}

	public int getCurOriginalLineNr() {
		return curOriginalLineNr;
	}
	
	public int getCurRevisedLineNr() {
		return curRevisedLineNr;
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
