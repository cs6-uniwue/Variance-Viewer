package de.uniwue.web.view;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import de.uniwue.compare.ConnectedContent;
import de.uniwue.compare.ContentType;
import de.uniwue.compare.VarianceType;
import de.uniwue.compare.token.Token;

public class LineCreator {

	private static String TOKENSPLIT = "((?<=" + System.lineSeparator() + ")|(?=" + System.lineSeparator() + "))";

	public static List<ConnectedLines> patch(List<ConnectedContent> content) {
		List<ConnectedLines> lines = new LinkedList<ConnectedLines>();

		ConnectedLines connectedLines = new ConnectedLines();
		lines.add(connectedLines);
		Line originalLine = new Line(1);
		Line revisedLine = new Line(1);
		connectedLines.addOriginalLines(originalLine);
		connectedLines.addRevisedLines(revisedLine);

		for (ConnectedContent connectedContent : content) {
			VarianceType varianceType = connectedContent.getVarianceType();
			switch (connectedContent.getContentType()) {
			case INSERT:
				for (Token token : connectedContent.getRevised()) {
					String tokenContent = token.getContent();
					if (tokenContent.contains(System.lineSeparator())) {
						revisedLine = splitToken(token, varianceType, connectedLines, revisedLine, false);
					} else
						revisedLine
								.addContent(new Content(token, ContentType.INSERT, varianceType, token.getHighlight()));
				}
				break;
			case DELETE:
				for (Token token : connectedContent.getOriginal()) {
					String tokenContent = token.getContent();
					if (tokenContent.contains(System.lineSeparator())) {
						originalLine = splitToken(token, varianceType, connectedLines, originalLine, true);
					} else
						originalLine
								.addContent(new Content(token, ContentType.DELETE, varianceType, token.getHighlight()));
				}
				break;
			case CHANGE:
				for (Token token : connectedContent.getOriginal()) {
					String tokenContent = token.getContent();
					if (tokenContent.contains(System.lineSeparator())) {
						originalLine = splitToken(token, varianceType, connectedLines, originalLine, true);
					} else
						originalLine
								.addContent(new Content(token, ContentType.CHANGE, varianceType, token.getHighlight()));
				}
				for (Token token : connectedContent.getRevised()) {
					String tokenContent = token.getContent();
					if (tokenContent.contains(System.lineSeparator())) {
						revisedLine = splitToken(token, varianceType, connectedLines, revisedLine, false);
					} else
						revisedLine
								.addContent(new Content(token, ContentType.CHANGE, varianceType, token.getHighlight()));
				}
				break;
			case EQUAL:
				for (Token token : connectedContent.getOriginal()) {
					String tokenContent = token.getContent();
					if (tokenContent.contains(System.lineSeparator())) {
						int begin = token.getBegin();
						for (String contentPart : tokenContent.split(TOKENSPLIT)) {
							Token tokenPart = new Token(begin, begin + contentPart.length(), contentPart,
									token.getAnnotations());
							begin += contentPart.length();
							originalLine.addContent(
									new Content(tokenPart, ContentType.EQUAL, varianceType, null));
							revisedLine.addContent(
									new Content(tokenPart, ContentType.EQUAL, varianceType, null));
							if (contentPart.equals(System.lineSeparator())) {
								connectedLines = new ConnectedLines();
								lines.add(connectedLines);
								originalLine = new Line(originalLine.getLineNr() + 1);
								revisedLine = new Line(revisedLine.getLineNr() + 1);
								connectedLines.addRevisedLines(revisedLine);
								connectedLines.addOriginalLines(originalLine);
							}
						}
					} else {
						originalLine
								.addContent(new Content(token, ContentType.EQUAL, varianceType, null));
						revisedLine
								.addContent(new Content(token, ContentType.EQUAL, varianceType, null));
					}
				}
				break;
			}
		}

		return lines;
	}

	private static Line splitToken(Token token, VarianceType varianceType, ConnectedLines connectedLines,
			Line currentLine, boolean isOriginalLine) {
		int begin = token.getBegin();

		Deque<long[]> highlightQueue = new ArrayDeque<>(token.getHighlight());

		long inTokenPos = 0;
		for (String contentPart : token.getContent().split(TOKENSPLIT)) {
			int partLength = contentPart.length();
			Token tokenPart = new Token(begin, begin + partLength, contentPart, token.getAnnotations());
			long intTokenEnd = inTokenPos+partLength;
			
			List<long[]> highlight = new LinkedList<>();
			while(partLength > 0 && !highlightQueue.isEmpty() && highlightQueue.peek()[0] < intTokenEnd) {
				long[] highlightPart = highlightQueue.pop();
				if(highlightPart[1] > intTokenEnd) {
					highlightQueue.addFirst(new long[] {intTokenEnd, highlightPart[1]});
					highlightPart[1] = intTokenEnd;
				}
				
				highlightPart[0] -= inTokenPos;
				highlightPart[1] -= inTokenPos;
				
				highlight.add(highlightPart);
			}

			currentLine.addContent(new Content(tokenPart, ContentType.CHANGE, varianceType, highlight));
			if (contentPart.equals(System.lineSeparator())) {
				currentLine = new Line(currentLine.getLineNr() + 1);
				if (isOriginalLine) {
					connectedLines.addOriginalLines(currentLine);
				} else {
					connectedLines.addRevisedLines(currentLine);
				}
			}
			begin += partLength;
			inTokenPos = intTokenEnd;
		}
		return currentLine;
	}
}
