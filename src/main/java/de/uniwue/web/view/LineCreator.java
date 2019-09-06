package de.uniwue.web.view;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import de.uniwue.compare.ConnectedContent;
import de.uniwue.compare.ContentType;
import de.uniwue.compare.token.Token;

public class LineCreator {

	private static String TOKENSPLIT = "((?<=" + System.lineSeparator() + ")|(?=" + System.lineSeparator() + "))";

	public static List<ConnectedLines> patch(List<ConnectedContent> content) {
		List<ConnectedLines> lines = new LinkedList<ConnectedLines>();
		
		ConnectedLines connectedLines = new ConnectedLines(0, 0);
		lines.add(connectedLines);

		boolean singleEndedWithNewLine = false;
		
		for (ConnectedContent connectedContent : content) {
			String varianceType = connectedContent.getVarianceType();
		
			switch (connectedContent.getContentType()) {
			case INSERT:
				for (Token token : connectedContent.getRevised()) {
					String tokenContent = token.getContent();

					if (tokenContent.contains(System.lineSeparator())) {
						splitToken(token, varianceType, connectedLines, false);
					} else {
						connectedLines.addRevisedContent(new Content(token, ContentType.INSERT, varianceType, token.getHighlight()));
					}
					singleEndedWithNewLine = token.getContent().endsWith(System.lineSeparator());
				}
				break;
			case DELETE:
				for (Token token : connectedContent.getOriginal()) {
					String tokenContent = token.getContent();

					if (tokenContent.contains(System.lineSeparator())) {
						splitToken(token, varianceType, connectedLines, true);
					} else {
						connectedLines.addOriginalContent(new Content(token, ContentType.DELETE, varianceType, token.getHighlight()));
					}
					singleEndedWithNewLine = token.getContent().endsWith(System.lineSeparator());
				}
				break;
			case CHANGE:
				if (singleEndedWithNewLine) {
					int lastOrigLineNr = connectedLines.getCurOriginalLineNr();
					int lastRevLineNr = connectedLines.getCurRevisedLineNr();
					lines.add(connectedLines = new ConnectedLines(lastOrigLineNr, lastRevLineNr));
					singleEndedWithNewLine = false;
				}

				for (Token token : connectedContent.getOriginal()) {
					String tokenContent = token.getContent();
					
					if (tokenContent.contains(System.lineSeparator())) {
						splitToken(token, varianceType, connectedLines, true);
					} else {
						connectedLines.addOriginalContent(new Content(token, ContentType.CHANGE, varianceType, token.getHighlight()));
					}
				}
				for (Token token : connectedContent.getRevised()) {
					String tokenContent = token.getContent();

					if (tokenContent.contains(System.lineSeparator())) {
						splitToken(token, varianceType, connectedLines, false);
					} else {
						connectedLines.addRevisedContent(new Content(token, ContentType.CHANGE, varianceType, token.getHighlight()));
					}
				}
				break;
			case EQUAL:
				if (singleEndedWithNewLine) {
					int lastOrigLineNr = connectedLines.getCurOriginalLineNr();
					int lastRevLineNr = connectedLines.getCurRevisedLineNr();
					lines.add(connectedLines = new ConnectedLines(lastOrigLineNr, lastRevLineNr));
					singleEndedWithNewLine = false;
				}
				
				for (Token token : connectedContent.getOriginal()) {
					String tokenContent = token.getContent();
					
					if (tokenContent.contains(System.lineSeparator())) {
						int begin = token.getBegin();
						for (String contentPart : tokenContent.split(TOKENSPLIT)) {
							Token tokenPart = new Token(begin, begin + contentPart.length(), contentPart, 
									token.getContentTag(), token.getAnnotations());
							begin += contentPart.length();
							connectedLines.addOriginalContent(new Content(tokenPart, ContentType.EQUAL, varianceType, null));

							connectedLines.addRevisedContent(new Content(tokenPart, ContentType.EQUAL, varianceType, null));
							if (contentPart.equals(System.lineSeparator())) {
								connectedLines.endOriginalLine();
								connectedLines.endRevisedLine();
							}
						}
					} else {
						connectedLines.addOriginalContent(new Content(token, ContentType.EQUAL, varianceType, null));
						connectedLines.addRevisedContent(new Content(token, ContentType.EQUAL, varianceType, null));
					}
					if (token.getContent().endsWith(System.lineSeparator())) {
						int lastOrigLineNr = connectedLines.getCurOriginalLineNr();
						int lastRevLineNr = connectedLines.getCurRevisedLineNr();
						lines.add(connectedLines = new ConnectedLines(lastOrigLineNr, lastRevLineNr));
					}
				}
				break;
			}
		}

		return lines;
	}

	private static void splitToken(Token token, String varianceType, ConnectedLines connectedLines,
			boolean isOriginalLine) {
		int begin = token.getBegin();

		Deque<long[]> highlightQueue = new ArrayDeque<>(token.getHighlight());

		long inTokenPos = 0;
		Iterator<String> lineIter = Arrays.asList(token.getContent().split(TOKENSPLIT)).iterator();
		
		while (lineIter.hasNext()) {
			String contentPart = lineIter.next();
			int partLength = contentPart.length();
			Token tokenPart = new Token(begin, begin + partLength, contentPart, token.getContentTag(), token.getAnnotations());
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

			if (isOriginalLine) {
				connectedLines.addOriginalContent(new Content(tokenPart, ContentType.CHANGE, varianceType, highlight));
			} else {
				connectedLines.addRevisedContent(new Content(tokenPart, ContentType.CHANGE, varianceType, highlight));
			}
			if (contentPart.equals(System.lineSeparator())) {
				if (isOriginalLine) {
					connectedLines.endOriginalLine();
				} else {
					connectedLines.endRevisedLine();
				}
			}
			begin += partLength;
			inTokenPos = intTokenEnd;
		}
	}
}
