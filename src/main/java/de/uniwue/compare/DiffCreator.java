package de.uniwue.compare;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import de.uniwue.compare.token.AnnotationToken;
import de.uniwue.compare.token.Token;
import de.uniwue.compare.token.VarianceToken;
import de.uniwue.compare.variance.VarianceClassifier;
import de.uniwue.compare.variance.types.Variance;
import difflib.Chunk;
import difflib.Delta;
import difflib.Delta.TYPE;
import difflib.DiffUtils;
import difflib.Patch;

public class DiffCreator {

	/**
	 * Patch a diff with the original two lists of tokens to combine them into
	 * connected content (Adds equal parts not present in the diff etc.)
	 * Tokens are combined into connected contents by analyzing and classifying
	 * them by different variance types.
	 * 
	 * @param originalLines          Lines form the first document
	 * @param revisedLines           Lines from the second document
	 * @param diffs                  Diffs between the two documents
	 * @param diffAnnotationsInEqual Search more annotations in equal portions
	 * @param settings      		Setting for normalizations
	 * @return
	 */
	public static List<ConnectedContent> patch(List<? extends Token> originalLines, List<? extends Token> revisedLines,
			List<? extends Delta<? extends Token>> diffs, boolean diffAnnotationsInEqual, Settings settings) {
		List<ConnectedContent> content = new LinkedList<ConnectedContent>();
		final List<Variance> variances = VarianceClassifier.sortVariances(settings.getVariances());
		variances.remove(Variance.TYPOGRAPHY);
		
		int prevOriginalEndPosition = -1;
		int prevRevisedEndPosition = -1;

		// Loop over all deltas add missing deltas, combine and classify them into connected contents 
		ConnectedContent curContent = null;
		for (Delta<? extends Token> delta : diffs) {
			final Chunk<? extends Token> original = delta.getOriginal();
			final Chunk<? extends Token> revised = delta.getRevised();
			final int currentOriginalPosition = original.getPosition();
			final int currentRevisedPosition = revised.getPosition();
			TYPE type = delta.getType();

			// Get equal content in between changes (Delta only lists changes not equal content)
			if (currentOriginalPosition > prevOriginalEndPosition + 1
					|| currentRevisedPosition > prevRevisedEndPosition + 1) {
				List<? extends Token> equalOriginalLines = originalLines.subList(prevOriginalEndPosition + 1,
						currentOriginalPosition);

				if (diffAnnotationsInEqual) {
					// Compare annotations in equal text (convert to AnnotationTokens and compare those)
					List<? extends Token> equalRevisedLines = revisedLines.subList(prevRevisedEndPosition + 1,
							currentRevisedPosition);
					List<AnnotationToken> annotationTokens1 = equalOriginalLines.stream()
							.map(t -> t.getAnnotationToken()).collect(Collectors.toList());
					List<AnnotationToken> annotationTokens2 = equalRevisedLines.stream()
							.map(t -> t.getAnnotationToken()).collect(Collectors.toList());
					Patch<AnnotationToken> annotationPatch = DiffUtils.diff(annotationTokens1, annotationTokens2);
					List<ConnectedContent> annotationDiff = patch(equalOriginalLines, equalRevisedLines,
							annotationPatch.getDeltas(), false, settings);

					for (ConnectedContent annotationContent : annotationDiff) {
						if (!annotationContent.getContentType().equals(ContentType.EQUAL))
							annotationContent.setVarianceType(Variance.TYPOGRAPHY.getName());

						// Add to last content (if of same type) or create new content
						curContent = addContent(annotationContent.getOriginal(), annotationContent.getRevised(),
								curContent, content, annotationContent.getContentType(), annotationContent.getVarianceType());
					}
				} else {
					// Add to last equal content (if of same type) or create new content
					curContent = addEqualContent(equalOriginalLines, curContent, content);
				}
			}

			switch (type) {
			case INSERT:
				// Add to last insert content (if of same type) or create new content
				curContent = insert(revised, revisedLines, curContent, content, variances);
				break;
			case DELETE:
				// Add to last delete content (if of same type) or create new content
				curContent = delete(original, originalLines, curContent, content, variances);
				break;
			case CHANGE:
				LinkedList<Token> originalTokens = new LinkedList<Token>(original.getLines());
				LinkedList<Token> revisedTokens = new LinkedList<Token>(revised.getLines());

				// Compare normalized tokens to find connected diffs that can be equalized by
				// Punctuation, Graphemics etc.
				LinkedList<VarianceToken> originalTestTokens = originalTokens.stream()
						.map(t -> t.getVarianceToken(variances)).collect(Collectors.toCollection(LinkedList::new));
				LinkedList<VarianceToken> revisedTestTokens = revisedTokens.stream()
						.map(t -> t.getVarianceToken(variances)).collect(Collectors.toCollection(LinkedList::new));

				Patch<VarianceToken> textPatch = DiffUtils.diff(originalTestTokens, revisedTestTokens);
				List<? extends Delta<? extends Token>> testDeltas = textPatch.getDeltas();

				int lastOriginalPosition = -1;
				int lastRevisedPosition = -1;
				for (Delta<? extends Token> testDelta : testDeltas) {
					// Get equal tokens (before delta changes)
					if (lastOriginalPosition + 1 < testDelta.getOriginal().getPosition()) {
						LinkedList<Token> equalTokensOriginal = new LinkedList<Token>(originalTokens
								.subList(lastOriginalPosition + 1, testDelta.getOriginal().getPosition()));
						LinkedList<Token> equalTokensRevised = new LinkedList<Token>(
								revisedTokens.subList(lastRevisedPosition + 1, testDelta.getRevised().getPosition()));

						for (int i = 0; i < equalTokensOriginal.size(); i++) {
							Token originalTest = equalTokensOriginal.get(i);
							Token revisedTest = equalTokensRevised.get(i);

							if (originalTest.getContent().length() > 0 && revisedTest.getContent().length() > 0) {
								// Change
								String varianceType = VarianceClassifier.classifyTouple(originalTest, revisedTest,
										ContentType.CHANGE, variances);
								highlightTokens(originalTest, revisedTest, varianceType);

								// Add to last content (if of same type) or create new content
								curContent = addContent(Arrays.asList(originalTest), Arrays.asList(revisedTest), curContent, content, 
										ContentType.CHANGE, varianceType);
							} else {
								// Delete or Insert
								String varianceType = null;
								if (originalTest.getContent().length() > 0) {
									varianceType = VarianceClassifier.classifySingle(originalTest, originalTokens, ContentType.DELETE,
											settings.getVariances());
								} else if (revisedTest.getContent().length() > 0) {
									varianceType = VarianceClassifier.classifySingle(revisedTest, revisedTokens, ContentType.INSERT,
											settings.getVariances());
								}

								highlightTokens(originalTest, revisedTest, varianceType);

								// Add to last content (if of same type) or create new content
								if (originalTest.getContent().length() > 0) {
									curContent = addContent(Arrays.asList(originalTest), new LinkedList<>(), curContent, content,
											ContentType.INSERT, varianceType);
								} else if (revisedTest.getContent().length() > 0) {
									curContent = addContent(new LinkedList<>(), Arrays.asList(revisedTest), curContent, content,
											ContentType.DELETE, varianceType);
								}
							}
						}
					}
					LinkedList<Token> unequalTokensOriginal = new LinkedList<Token>(originalTokens
							.subList(testDelta.getOriginal().getPosition(), testDelta.getOriginal().last() + 1));
					LinkedList<Token> unequalTokensRevised = new LinkedList<Token>(revisedTokens
							.subList(testDelta.getRevised().getPosition(), testDelta.getRevised().last() + 1));
					int originalCount = unequalTokensOriginal.size();
					int revisedCount = unequalTokensRevised.size();

					if (originalCount == 0 && revisedCount > 0) {
						// Add to last insert content (if of same type) or create new content
						curContent = insert(unequalTokensRevised, revisedLines, curContent, content, variances);
					} else if (originalCount > 0 && revisedCount == 0) {
						// Add to last delete content (if of same type) or create new content
						curContent = delete(unequalTokensOriginal, originalLines, curContent, content, variances);
					} else if (originalCount > 0 && revisedCount > 0) {
						unequalTokensOriginal.forEach(t -> t.highlightEverything());
						unequalTokensRevised.forEach(t -> t.highlightEverything());

						// Add to last content (if of same type) or create new content
						curContent = addContent(unequalTokensOriginal, unequalTokensRevised, curContent, content,
								ContentType.CHANGE, Variance.CONTENT.getName());
					}

					lastOriginalPosition = testDelta.getOriginal().last();
					lastRevisedPosition = testDelta.getRevised().last();
				}

				// Test equal tokens after the end of the last change
				if (lastOriginalPosition + 1 < originalTestTokens.size()) {
					// Test Equal on end
					LinkedList<Token> equalTokensOriginal = new LinkedList<Token>(
							originalTokens.subList(lastOriginalPosition + 1, originalTestTokens.size()));
					LinkedList<Token> equalTokensRevised = new LinkedList<Token>(
							revisedTokens.subList(lastRevisedPosition + 1, revisedTestTokens.size()));

					for (int i = 0; i < equalTokensOriginal.size(); i++) {
						Token originalTest = equalTokensOriginal.get(i);
						Token revisedTest = equalTokensRevised.get(i);
						String varianceType = VarianceClassifier.classifyTouple(originalTest, revisedTest, ContentType.CHANGE,
								variances);
						highlightTokens(originalTest, revisedTest, varianceType);

						// Add to last content (if of same type) or create new content
						curContent = addContent(Arrays.asList(originalTest), Arrays.asList(revisedTest), curContent, content,
								ContentType.CHANGE, varianceType);
					}
				}

				break;
			}

			prevOriginalEndPosition = original.last();
			prevRevisedEndPosition = revised.last();
		}
		// Add equals that are the end of the document (Delta only lists changes not equal content)
		if (originalLines.size() > prevOriginalEndPosition + 1) {
			List<? extends Token> equalOriginalLines = originalLines.subList(prevOriginalEndPosition + 1,
					originalLines.size());
			if (diffAnnotationsInEqual) {
				// Compare annotations in equal text
				List<? extends Token> equalRevisedLines = revisedLines.subList(prevRevisedEndPosition + 1,
						revisedLines.size());

				List<AnnotationToken> annotationTokens1 = equalOriginalLines.stream().map(t -> t.getAnnotationToken())
						.collect(Collectors.toList());
				List<AnnotationToken> annotationTokens2 = equalRevisedLines.stream().map(t -> t.getAnnotationToken())
						.collect(Collectors.toList());

				Patch<AnnotationToken> annotationPatch = DiffUtils.diff(annotationTokens1, annotationTokens2);
				List<ConnectedContent> annotationDiff = patch(equalOriginalLines, equalRevisedLines,
						annotationPatch.getDeltas(), false, settings);

				for (ConnectedContent annotationContent : annotationDiff) {
					if (!annotationContent.getContentType().equals(ContentType.EQUAL))
						annotationContent.setVarianceType(Variance.TYPOGRAPHY.getName());

					// Add to last content (if of same type) or create new content
					curContent = addContent(annotationContent.getOriginal(), annotationContent.getRevised(),
							curContent, content, annotationContent.getContentType(), annotationContent.getVarianceType());
				}

				content.addAll(annotationDiff);
			} else {
				// Add to last equal content (if of same type) or create new content
				curContent = addEqualContent(equalOriginalLines, curContent, content);
			}
		}
		
		
		// Post classification (check for variance types spanning over multiple words)
		// Test all consecutive connected components of type "CONTENT"
		List<ConnectedContent> postcorrection = new LinkedList<>();
		List<ConnectedContent> backlog = new LinkedList<>();
		for (ConnectedContent c : content) {	
			if (c.getVarianceType().equals(Variance.CONTENT.getName())) {
				backlog.add(c);
			} else {
				if (backlog.size() > 0) {
					// Work on backlog
					postcorrection.addAll(VarianceClassifier.classifyMultiple(backlog));
				}
				backlog = new LinkedList<>();
				postcorrection.add(c);
			}
		}
		if (backlog.size() > 0) {
			// Last work on result backlog
			postcorrection.addAll(VarianceClassifier.classifyMultiple(backlog));
		}
	
		return postcorrection;
	}

	/**
	 * Add to last content (if of same type) or create new content.
	 * Return the ConnectedContent to which it was added to.
	 * 
	 * @param original
	 * @param revised
	 * @param curContent
	 * @param contents
	 * @param contentType
	 * @param varianceType
	 * @return
	 */
	private static ConnectedContent addContent(List<? extends Token> original, List<? extends Token> revised, ConnectedContent curContent,
			List<ConnectedContent> contents, ContentType contentType, String varianceType ) {

		if (curContent == null || !curContent.getContentType().equals(contentType)
				|| !curContent.getVarianceType().equals(varianceType)) {
			curContent = new ConnectedContent(original, revised, contentType, varianceType);
			contents.add(curContent);
		} else {
			if(original.size() > 0)
				curContent.addOriginal(original);
			if(revised.size() > 0)
				curContent.addRevised(revised);
		}
		return curContent;
	}
	
	/**
	 * Add this equal to last content (if is also equal) or create new equal content.
	 * Return the ConnectedContent to which it was added to.
	 * 
	 * @param tokens
	 * @param curContent
	 * @param contents
	 * @return
	 */
	private static ConnectedContent addEqualContent(List<? extends Token> tokens, ConnectedContent curContent,
			List<ConnectedContent> contents) {
		if (curContent == null || !curContent.getContentType().equals(ContentType.EQUAL)) {
			curContent = new ConnectedContent(new LinkedList<Token>(tokens));
			contents.add(curContent);
		} else {
			curContent.addOriginal(tokens);
		}
		return curContent;
	}
	
	/**
	 * Process chunks of insert changes into ConnectedContents
	 * 
	 * @param revised           Inserted tokens
	 * @param curContent        Content that is currently on head
	 * @param allContent        List of all Connected Contents
	 * @param normalizerStorage Normalize settings
	 * @return Connected content
	 */
	private static ConnectedContent insert(Chunk<? extends Token> revised, List<? extends Token> revisedLines, ConnectedContent curContent,
			List<ConnectedContent> allContent, List<Variance> variances) {
		return prosessInsertAndDelete(revised.getLines(), revisedLines, curContent, allContent, variances,
				ContentType.INSERT);
	}

	/**
	 * Process chunks of delete changes into ConnectedContents
	 * 
	 * @param original          Deleted tokens
	 * @param curContent        Content that is currently on head
	 * @param allContent        List of all Connected Contents
	 * @param normalizerStorage Normalize settings
	 * @return Connected content
	 */
	private static ConnectedContent delete(Chunk<? extends Token> original, List<? extends Token> originalDocument, ConnectedContent curContent,
			List<ConnectedContent> allContent, List<Variance> variances) {
		return prosessInsertAndDelete(original.getLines(), originalDocument, curContent, allContent, variances,
				ContentType.DELETE);
	}

	/**
	 * Process list of insert changes into ConnectedContents
	 * 
	 * @param revised           Inserted tokens
	 * @param curContent        Content that is currently on head
	 * @param allContent        List of all Connected Contents
	 * @param normalizerStorage Normalize settings
	 * @return Connected content
	 */
	private static ConnectedContent insert(List<? extends Token> revised, List<? extends Token> revisedDocument, ConnectedContent curContent,
			List<ConnectedContent> allContent, List<Variance> variances) {
		return prosessInsertAndDelete(revised, revisedDocument, curContent, allContent, variances, ContentType.INSERT);
	}

	/**
	 * Process list of delete changes into ConnectedContents
	 * 
	 * @param original          Deleted tokens
	 * @param curContent        Content that is currently on head
	 * @param allContent        List of all Connected Contents
	 * @param normalizerStorage Normalize settings
	 * @return Connected content
	 */
	private static ConnectedContent delete(List<? extends Token> original, List<? extends Token> originalDocument, ConnectedContent curContent,
			List<ConnectedContent> allContent, List<Variance> variances) {
		return prosessInsertAndDelete(original, originalDocument, curContent, allContent, variances, ContentType.DELETE);
	}

	/**
	 * Helper function to process a list of tokens of either insert or delete
	 * changes into ConnectedContent.
	 * 
	 * @param tokens            List of tokens
	 * @param curContent        Content that is currently on head
	 * @param allContent        List of all Connected Contents
	 * @param normalizerStorage Normalize settings
	 * @param curContentType    Content Type of the list of tokens (either INSERT or
	 *                          DELETE)
	 * @return Connected content
	 */
	private static ConnectedContent prosessInsertAndDelete(List<? extends Token> tokens, List<? extends Token> document, ConnectedContent curContent,
			List<ConnectedContent> allContent, List<Variance> variances, ContentType curContentType) {
		if (!curContentType.equals(ContentType.INSERT) && !curContentType.equals(ContentType.DELETE))
			throw new IllegalArgumentException(
					"Expects content of type INSERT or DELETE. (Was " + curContentType + ")");

		for (Token token : tokens) {
			token.highlightEverything();
			String curVariance = token instanceof AnnotationToken ? Variance.TYPOGRAPHY.getName()
					: VarianceClassifier.classifySingle(token, document, curContentType, variances);
			if (curContent == null || !curContent.getContentType().equals(curContentType)
					|| !curContent.getVarianceType().equals(curVariance)) {
				curContent = new ConnectedContent(curContentType, curVariance);
				allContent.add(curContent);
			}
			if (curContentType.equals(ContentType.INSERT))
				curContent.addRevised(token);
			else
				curContent.addOriginal(token);
		}
		return curContent;
	}

	/**
	 * Highlight the specific changes between two tokens.
	 * 
	 * E.g. "Test" "Text" -> "Te[s,x]t" Highlight char 2 (s/x)
	 * 
	 * @param token1 Token one to highlight
	 * @param token2 Token two to highlight
	 */
	private static void highlightTokens(Token token1, Token token2, String varianceType) {
		if (!varianceType.equals(Variance.TYPOGRAPHY.getName())) {

			List<long[]> highlightToken1 = new LinkedList<>();
			List<long[]> highlightToken2 = new LinkedList<>();

			Patch<String> annotationPatch = DiffUtils.diff(Arrays.asList(token1.getContent().split("(?!^)")),
					Arrays.asList(token2.getContent().split("(?!^)")));
			for (Delta<String> delta : annotationPatch.getDeltas()) {
				TYPE type = delta.getType();
				if (type.equals(TYPE.INSERT) || type.equals(TYPE.CHANGE)) {
					Chunk<String> insert = delta.getRevised();
					highlightToken2.add(new long[] { insert.getPosition(), insert.last() + 1 });
				}

				if (type.equals(TYPE.DELETE) || type.equals(TYPE.CHANGE)) {
					Chunk<String> delete = delta.getOriginal();
					highlightToken1.add(new long[] { delete.getPosition(), delete.last() + 1 });
				}
			}
			token1.setHighlight(highlightToken1);
			token2.setHighlight(highlightToken2);
		} else {
			token1.highlightEverything();
			token2.highlightEverything();
		}
	}
}
