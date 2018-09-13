package de.uniwue.web.view;

import java.util.Arrays;
import java.util.LinkedList;

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
}
