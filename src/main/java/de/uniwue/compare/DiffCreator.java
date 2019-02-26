package de.uniwue.compare;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import de.uniwue.compare.token.AnnotationToken;
import de.uniwue.compare.token.TextToken;
import de.uniwue.compare.token.Token;
import difflib.Chunk;
import difflib.Delta;
import difflib.Delta.TYPE;
import difflib.DiffUtils;
import difflib.Patch;

public class DiffCreator {

	/**
	 * Patch a diff with the original two lists of tokens to combine them into
	 * connected content (Adds equal parts not present in the diff etc.)
	 * 
	 * @param originalLines
	 *            Lines form the first document
	 * @param revisedLines
	 *            Lines from the second document
	 * @param diffs
	 *            Diffs between the two documents
	 * @param diffAnnotationsInEqual
	 *            Search more annotations in equal portions
	 * @param normalizerStorage
	 *            Setting for normalizations
	 * @return
	 */
	public static List<ConnectedContent> patch(List<? extends Token> originalLines, List<? extends Token> revisedLines,
			List<? extends Delta<? extends Token>> diffs, boolean diffAnnotationsInEqual, Settings normalizerStorage) {
		List<ConnectedContent> content = new LinkedList<ConnectedContent>();

		int prevOriginalEndPosition = -1;
		int prevRevisedEndPosition = -1;

		ConnectedContent curContent = null;
		for (Delta<? extends Token> delta : diffs) {
			final Chunk<? extends Token> original = delta.getOriginal();
			final Chunk<? extends Token> revised = delta.getRevised();
			final int currentOriginalPosition = original.getPosition();
			final int currentRevisedPosition = revised.getPosition();
			TYPE type = delta.getType();

			// Get equal content in between changes (Delta only lists changes not equal
			// content)
			if (currentOriginalPosition > prevOriginalEndPosition + 1
					|| currentRevisedPosition > prevRevisedEndPosition + 1) {
				List<? extends Token> equalOriginalLines = originalLines.subList(prevOriginalEndPosition + 1,
						currentOriginalPosition);

				if (diffAnnotationsInEqual) {
					// Compare annotations in equal text
					List<? extends Token> equalRevisedLines = revisedLines.subList(prevRevisedEndPosition + 1,
							currentRevisedPosition);

					List<AnnotationToken> annotationTokens1 = equalOriginalLines.stream()
							.map(t -> t.getAnnotationToken()).collect(Collectors.toList());
					List<AnnotationToken> annotationTokens2 = equalRevisedLines.stream()
							.map(t -> t.getAnnotationToken()).collect(Collectors.toList());

					Patch<AnnotationToken> annotationPatch = DiffUtils.diff(annotationTokens1, annotationTokens2);
					List<ConnectedContent> annotationDiff = patch(equalOriginalLines, equalRevisedLines,
							annotationPatch.getDeltas(), false, normalizerStorage);

					for (ConnectedContent annotationContent : annotationDiff) {
						if (!annotationContent.getContentType().equals(ContentType.EQUAL))
							annotationContent.setVarianceType(VarianceType.TYPOGRAPHY);

						// Add to last content (if of same type) or create new content
						if (curContent == null
								|| !curContent.getContentType().equals(annotationContent.getContentType())
								|| !curContent.getVarianceType().equals(annotationContent.getVarianceType())) {
							curContent = annotationContent;
							content.add(curContent);
						} else {
							curContent.addOriginal(annotationContent.getOriginal());
							curContent.addRevised(annotationContent.getRevised());
						}
					}
				} else {
					// Add to last equal content (if of same type) or create new content
					if (curContent == null || !curContent.getContentType().equals(ContentType.EQUAL)) {
						curContent = new ConnectedContent(new LinkedList<Token>(equalOriginalLines));
						content.add(curContent);
					} else {
						curContent.addOriginal(new LinkedList<Token>(equalOriginalLines));
					}
				}
			}

			switch (type) {
			case INSERT:
				// Add to last insert content (if of same type) or create new content
				curContent = insert(revised, curContent, content, normalizerStorage);
				break;
			case DELETE:
				// Add to last delete content (if of same type) or create new content
				curContent = delete(original, curContent, content, normalizerStorage);
				break;
			case CHANGE:
				LinkedList<Token> originalTokens = new LinkedList<Token>(original.getLines());
				LinkedList<Token> revisedTokens = new LinkedList<Token>(revised.getLines());

				// Compare normalized tokens to find connected diffs that can be equalized by
				// Punctuation, Graphemics etc.
				LinkedList<TextToken> originalTestTokens = normalize(originalTokens, normalizerStorage).stream()
						.map(t -> t.getTextToken()).collect(Collectors.toCollection(LinkedList::new));
				LinkedList<TextToken> revisedTestTokens = normalize(revisedTokens, normalizerStorage).stream()
						.map(t -> t.getTextToken()).collect(Collectors.toCollection(LinkedList::new));

				Patch<TextToken> textPatch = DiffUtils.diff(originalTestTokens, revisedTestTokens);
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
							VarianceType varianceType = getVarianceTypeTouple(originalTest, revisedTest,
									ContentType.CHANGE, normalizerStorage);
							if (!varianceType.equals(VarianceType.TYPOGRAPHY)
									&& !varianceType.equals(VarianceType.ABBREVIATION))
								highlightTokens(originalTest, revisedTest);
							else {
								originalTest.highlightEverything();
								revisedTest.highlightEverything();
							}

							// Add to last content (if of same type) or create new content
							if (curContent == null || !curContent.getContentType().equals(ContentType.CHANGE)
									|| !curContent.getVarianceType().equals(varianceType)) {
								curContent = new ConnectedContent(originalTest, revisedTest, ContentType.CHANGE);
								curContent.setVarianceType(varianceType);
								content.add(curContent);
							} else {
								curContent.addOriginal(originalTest);
								curContent.addRevised(revisedTest);
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
						curContent = insert(unequalTokensRevised, curContent, content, normalizerStorage);
					} else if (originalCount > 0 && revisedCount == 0) {
						// Add to last delete content (if of same type) or create new content
						curContent = delete(unequalTokensOriginal, curContent, content, normalizerStorage);
					} else if (originalCount > 0 && revisedCount > 0) {
						unequalTokensOriginal.forEach(t -> t.highlightEverything());
						unequalTokensRevised.forEach(t -> t.highlightEverything());

						// Add to last content (if of same type) or create new content
						if (curContent == null || !curContent.getContentType().equals(ContentType.CHANGE)
								|| !curContent.getVarianceType().equals(VarianceType.CONTENT)) {
							curContent = new ConnectedContent(unequalTokensOriginal, unequalTokensRevised,
									ContentType.CHANGE);
							curContent.setVarianceType(VarianceType.CONTENT);
							content.add(curContent);
						} else {
							curContent.addOriginal(unequalTokensOriginal);
							curContent.addRevised(unequalTokensRevised);
						}
					}

					lastOriginalPosition = testDelta.getOriginal().last();
					lastRevisedPosition = testDelta.getRevised().last();
				}

				if (lastOriginalPosition + 1 < originalTestTokens.size()) {
					// Test Equal on end
					LinkedList<Token> equalTokensOriginal = new LinkedList<Token>(
							originalTokens.subList(lastOriginalPosition + 1, originalTestTokens.size()));
					LinkedList<Token> equalTokensRevised = new LinkedList<Token>(
							revisedTokens.subList(lastRevisedPosition + 1, revisedTestTokens.size()));

					for (int i = 0; i < equalTokensOriginal.size(); i++) {
						Token originalTest = equalTokensOriginal.get(i);
						Token revisedTest = equalTokensRevised.get(i);
						VarianceType varianceType = getVarianceTypeTouple(originalTest, revisedTest, ContentType.CHANGE,
								normalizerStorage);
						if (!varianceType.equals(VarianceType.TYPOGRAPHY)
								&& !varianceType.equals(VarianceType.ABBREVIATION))
							highlightTokens(originalTest, revisedTest);
						else {
							originalTest.highlightEverything();
							revisedTest.highlightEverything();
						}

						// Add to last content (if of same type) or create new content
						if (curContent == null || !curContent.getContentType().equals(ContentType.CHANGE)
								|| !curContent.getVarianceType().equals(varianceType)) {
							curContent = new ConnectedContent(originalTest, revisedTest, ContentType.CHANGE);
							curContent.setVarianceType(varianceType);
							content.add(curContent);
						} else {
							curContent.addOriginal(originalTest);
							curContent.addRevised(revisedTest);
						}
					}
				}

				break;
			}

			prevOriginalEndPosition = original.last();
			prevRevisedEndPosition = revised.last();
		}
		// Add equals that are the end of the document (Delta only lists changes not
		// equal content)
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
						annotationPatch.getDeltas(), false, normalizerStorage);

				for (ConnectedContent annotationContent : annotationDiff) {
					if (!annotationContent.getContentType().equals(ContentType.EQUAL))
						annotationContent.setVarianceType(VarianceType.TYPOGRAPHY);

					// Add to last content (if of same type) or create new content
					if (curContent == null || !curContent.getContentType().equals(annotationContent.getContentType())
							|| !curContent.getVarianceType().equals(annotationContent.getVarianceType())) {
						curContent = annotationContent;
						content.add(curContent);
					} else {
						curContent.addOriginal(annotationContent.getOriginal());
						curContent.addRevised(annotationContent.getRevised());
					}
				}

				content.addAll(annotationDiff);
			} else {
				// Add to last equal content (if of same type) or create new content
				if (curContent == null || !curContent.getContentType().equals(ContentType.EQUAL)) {
					curContent = new ConnectedContent(new LinkedList<Token>(equalOriginalLines));
					content.add(curContent);
				} else {
					curContent.addOriginal(new LinkedList<Token>(equalOriginalLines));
				}
			}
		}

		return content;
	}

	/**
	 * Process chunks of insert changes into ConnectedContents
	 * 
	 * @param revised
	 *            Inserted tokens
	 * @param curContent
	 *            Content that is currently on head
	 * @param allContent
	 *            List of all Connected Contents
	 * @param normalizerStorage
	 *            Normalize settings
	 * @return Connected content
	 */
	private static ConnectedContent insert(Chunk<? extends Token> revised, ConnectedContent curContent,
			List<ConnectedContent> allContent, Settings normalizerStorage) {
		return prosessInsertAndDelete(revised.getLines(), curContent, allContent, normalizerStorage,
				ContentType.INSERT);
	}

	/**
	 * Process chunks of delete changes into ConnectedContents
	 * 
	 * @param original
	 *            Deleted tokens
	 * @param curContent
	 *            Content that is currently on head
	 * @param allContent
	 *            List of all Connected Contents
	 * @param normalizerStorage
	 *            Normalize settings
	 * @return Connected content
	 */
	private static ConnectedContent delete(Chunk<? extends Token> original, ConnectedContent curContent,
			List<ConnectedContent> allContent, Settings normalizerStorage) {
		return prosessInsertAndDelete(original.getLines(), curContent, allContent, normalizerStorage,
				ContentType.DELETE);
	}

	/**
	 * Process list of insert changes into ConnectedContents
	 * 
	 * @param revised
	 *            Inserted tokens
	 * @param curContent
	 *            Content that is currently on head
	 * @param allContent
	 *            List of all Connected Contents
	 * @param normalizerStorage
	 *            Normalize settings
	 * @return Connected content
	 */
	private static ConnectedContent insert(List<? extends Token> revised, ConnectedContent curContent,
			List<ConnectedContent> allContent, Settings normalizerStorage) {
		return prosessInsertAndDelete(revised, curContent, allContent, normalizerStorage, ContentType.INSERT);
	}

	/**
	 * Process list of delete changes into ConnectedContents
	 * 
	 * @param original
	 *            Deleted tokens
	 * @param curContent
	 *            Content that is currently on head
	 * @param allContent
	 *            List of all Connected Contents
	 * @param normalizerStorage
	 *            Normalize settings
	 * @return Connected content
	 */
	private static ConnectedContent delete(List<? extends Token> original, ConnectedContent curContent,
			List<ConnectedContent> allContent, Settings normalizerStorage) {
		return prosessInsertAndDelete(original, curContent, allContent, normalizerStorage, ContentType.DELETE);
	}

	/**
	 * Helper function to process a list of tokens of either insert or delete
	 * changes into ConnectedContent.
	 * 
	 * @param tokens
	 *            List of tokens
	 * @param curContent
	 *            Content that is currently on head
	 * @param allContent
	 *            List of all Connected Contents
	 * @param normalizerStorage
	 *            Normalize settings
	 * @param curContentType
	 *            Content Type of the list of tokens (either INSERT or DELETE)
	 * @return Connected content
	 */
	private static ConnectedContent prosessInsertAndDelete(List<? extends Token> tokens, ConnectedContent curContent,
			List<ConnectedContent> allContent, Settings normalizerStorage, ContentType curContentType) {
		if (!curContentType.equals(ContentType.INSERT) && !curContentType.equals(ContentType.DELETE))
			throw new IllegalArgumentException(
					"Expects content of type INSERT or DELETE. (Was " + curContentType + ")");

		for (Token token : tokens) {
			token.highlightEverything();
			VarianceType curVariance = token instanceof AnnotationToken ? VarianceType.TYPOGRAPHY
					: getVarianceTypeSingle(token, curContentType, normalizerStorage);
			if (curContent == null || !curContent.getContentType().equals(curContentType)
					|| !curContent.getVarianceType().equals(curVariance)) {
				curContent = new ConnectedContent(curContentType);
				curContent.setVarianceType(curVariance);
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
	 * @param token1
	 *            Token one to highlight
	 * @param token2
	 *            Token two to highlight
	 */
	private static void highlightTokens(Token token1, Token token2) {
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
	}

	/**
	 * Classify the changes in an insert or delete token. Normalizes the token to
	 * identify the variance type step by step.
	 * 
	 * Separation -> Punctuation -> (else) Content
	 * 
	 * @param content
	 *            Token to classify
	 * @param type
	 *            ContentType of the variance type (preferably INSERT or DELETE.
	 *            CHANGE will be handled like INSERT/DELETE and EQUAL will not be
	 *            set to VarianceType NONE)
	 * @param normalizerStorage
	 *            Normalize settings and rules
	 * @return Variance Type of the token
	 */
	private static VarianceType getVarianceTypeSingle(Token content, ContentType type, Settings normalizerStorage) {
		if (type.equals(ContentType.INSERT) || type.equals(ContentType.DELETE) || type.equals(ContentType.CHANGE)) {
			String contentWork = normalizeLineSEPARATION(content.getContent());
			if (contentWork.equals(""))
				return VarianceType.SEPARATION;
			else if (removePunctuation(contentWork, normalizerStorage).equals(""))
				return VarianceType.PUNCTUATION;
			else
				return VarianceType.CONTENT;
		}
		return VarianceType.NONE;
	}

	/**
	 * Classify the changes in two tokens to variance types. Normalizes the tokens
	 * to identify the variance types step by step.
	 * 
	 * Equal -> Separation -> Typography -> Punctuation -> Graphemics ->
	 * Abbreviations -> (else) Content
	 * 
	 * @param original
	 *            Token from the original text
	 * @param revised
	 *            Token from the revised text
	 * @param type
	 *            Content type (Equal, Insert, Delete, Change)
	 * @param normalizerStorage
	 *            Normalize settings and rules
	 * @return Variance Type of the tuple of tokens
	 */
	private static VarianceType getVarianceTypeTouple(Token original, Token revised, ContentType type,
			Settings normalizerStorage) {
		if (type.equals(ContentType.EQUAL))
			return VarianceType.NONE;

		String originalWork = normalizeLineSEPARATION(original.getContent());
		String revisedWork = normalizeLineSEPARATION(revised.getContent());

		// Test for Separation
		if (originalWork.equals(revisedWork) && original.getAnnotations().equals(revised.getAnnotations()))
			return VarianceType.SEPARATION;

		// Test for Typography
		if (originalWork.equals(revisedWork))
			return VarianceType.TYPOGRAPHY;

		// Test for Punctuation
		if (removePunctuation(originalWork, normalizerStorage)
				.equals(removePunctuation(revisedWork, normalizerStorage)))
			return VarianceType.PUNCTUATION;

		// Test for Graphemics
		if (normalizeGraphemics(originalWork, normalizerStorage)
				.equals(normalizeGraphemics(revisedWork, normalizerStorage)))
			return VarianceType.GRAPHEMICS;

		// Test for Abbreviations
		if (normalizeAbbreviations(originalWork, normalizerStorage)
				.equals(normalizeAbbreviations(revisedWork, normalizerStorage)))
			return VarianceType.ABBREVIATION;

		return VarianceType.CONTENT;
	}

	/**
	 * Normalize tokens to check for different variance types. E.g. "Test" "Test."
	 * can both be normalized to "Test", which helps to identify underlying variance
	 * types that can be classified. (Punctuation in this case)
	 * 
	 * @param list
	 *            list of tokens to normalize
	 * @param normalizerStorage
	 *            normalize settings (Config file)
	 * @return List of normalized tokens
	 */
	private static LinkedList<Token> normalize(List<Token> list, Settings normalizerStorage) {
		LinkedList<Token> normalized = new LinkedList<Token>();

		for (Token token : list) {
			String content = normalize(token.getContent(), normalizerStorage);
			normalized.add(new Token(token.getBegin(), token.getEnd(), content, token.getAnnotations()));
		}
		return normalized;
	}

	/**
	 * Normalize a token to check for different variance types. E.g. "Test" "Test."
	 * can both be normalized to "Test", which helps to identify underlying variance
	 * types that can be classified. (Punctuation in this case)
	 * 
	 * @param token
	 *            token to normalize
	 * @param normalizerStorage
	 *            normalize settings (Config file)
	 * @return normalized token
	 */
	private static String normalize(String token, Settings normalizerStorage) {
		return normalizeGraphemics(
				removePunctuation(normalizeAbbreviations(normalizeLineSEPARATION(token), normalizerStorage),
						normalizerStorage),
				normalizerStorage);
	}

	/**
	 * Normalize a token to check for line separation. E.g. "Test\n" "Test" can be
	 * normalized to "Test", which helps to identify a line separation variance
	 * type.
	 * 
	 * @param token
	 *            token to normalize
	 * @return normalized token
	 */
	private static String normalizeLineSEPARATION(String token) {
		return token.replace(System.lineSeparator(), "");
	}

	/**
	 * Normalize a token to check for punctuations. E.g. "Test." "Test" can be
	 * normalized to "Test" (if given "." as punctuation rule), which helps to
	 * identify a punctuation variance type.
	 * 
	 * @param token
	 *            token to normalize
	 * @param normalizerStorage
	 *            normalize settings with all punctuations (Config file)
	 * @return normalized token
	 */
	private static String removePunctuation(String token, Settings normalizerStorage) {
		String punctuations = "";
		for (String punctuation : normalizerStorage.getPunctuation())
			punctuations += punctuation;
		if (punctuations.length() > 0)
			return token.replaceAll("[" + Pattern.quote(punctuations) + "]", "");
		else
			return token;
	}

	/**
	 * Normalize a token to check for graphemic changes. E.g. "TestÄ" "TestAe" can
	 * be normalized to "TestAe" (if given the graphemic rule "Ä->Ae"), which helps
	 * to identify a graphemic variance type.
	 * 
	 * @param token
	 *            token to normalize
	 * @param normalizerStorage
	 *            normalize settings with all graphemic rules (Config file)
	 * @return normalized token
	 */
	private static String normalizeGraphemics(String token, Settings normalizerStorage) {
		String normalizedToken = token;
		normalizedToken = normalizedToken.toLowerCase();
		for (Map.Entry<String, String> touple : normalizerStorage.getGraphemes().entrySet())
			normalizedToken = normalizedToken.replaceAll(Pattern.quote(touple.getKey()), touple.getValue());

		return normalizedToken;
	}

	/**
	 * Normalize a token to check for abbreviation changes. E.g. "TestEx"
	 * "TestExample" can be normalized to "TestExample" (if given the abbreviation
	 * rule "Ex->Example"), which helps to identify a abbreviation variance type.
	 * 
	 * @param token
	 *            token to normalize
	 * @param normalizerStorage
	 *            normalize settings with all abbreviation rules (Config file)
	 * @return normalized token
	 */
	private static String normalizeAbbreviations(String token, Settings normalizerStorage) {
		String normalizedToken = token;
		for (Map.Entry<String, String> touple : normalizerStorage.getAbbreviations().entrySet()) {
			normalizedToken = normalizedToken.replaceAll(Pattern.quote(touple.getKey()), touple.getValue());
		}

		return normalizedToken;
	}
}
