package de.uniwue.compare;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import de.uniwue.compare.token.Token;

public class VarianceClassifier {


	/**
	 * Classify the changes in an insert or delete token. Normalizes the token to
	 * identify the variance type step by step.
	 * 
	 * Separation -> Punctuation -> (else) Content
	 * 
	 * @param content           Token to classify
	 * @param type              ContentType of the variance type (preferably INSERT
	 *                          or DELETE. CHANGE will be handled like INSERT/DELETE
	 *                          and EQUAL will not be set to VarianceType NONE)
	 * @param normalizerStorage Normalize settings and rules
	 * @return Variance Type of the token
	 */
	public static VarianceType getVarianceTypeSingle(Token content, ContentType type, Settings normalizerStorage) {
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
	 * @param original          Token from the original text
	 * @param revised           Token from the revised text
	 * @param type              Content type (Equal, Insert, Delete, Change)
	 * @param normalizerStorage Normalize settings and rules
	 * @return Variance Type of the tuple of tokens
	 */
	public static VarianceType getVarianceTypeTouple(Token original, Token revised, ContentType type,
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
	 * @param list              list of tokens to normalize
	 * @param normalizerStorage normalize settings (Config file)
	 * @return List of normalized tokens
	 */
	public static LinkedList<Token> normalize(List<Token> list, Settings normalizerStorage) {
		LinkedList<Token> normalized = new LinkedList<Token>();

		for (Token token : list) {
			String content = normalize(token.getContent(), normalizerStorage);
			normalized.add(new Token(token.getBegin(), token.getEnd(), content, token.getContentTag(), token.getAnnotations()));
		}
		return normalized;
	}

	/**
	 * Normalize a token to check for different variance types. E.g. "Test" "Test."
	 * can both be normalized to "Test", which helps to identify underlying variance
	 * types that can be classified. (Punctuation in this case)
	 * 
	 * @param token             token to normalize
	 * @param normalizerStorage normalize settings (Config file)
	 * @return normalized token
	 */
	public static String normalize(String token, Settings normalizerStorage) {
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
	 * @param token token to normalize
	 * @return normalized token
	 */
	public static String normalizeLineSEPARATION(String token) {
		return token.replace(System.lineSeparator(), "");
	}

	/**
	 * Normalize a token to check for punctuations. E.g. "Test." "Test" can be
	 * normalized to "Test" (if given "." as punctuation rule), which helps to
	 * identify a punctuation variance type.
	 * 
	 * @param token             token to normalize
	 * @param normalizerStorage normalize settings with all punctuations (Config
	 *                          file)
	 * @return normalized token
	 */
	public static String removePunctuation(String token, Settings normalizerStorage) {
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
	 * @param token             token to normalize
	 * @param normalizerStorage normalize settings with all graphemic rules (Config
	 *                          file)
	 * @return normalized token
	 */
	public static String normalizeGraphemics(String token, Settings normalizerStorage) {
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
	 * @param token             token to normalize
	 * @param normalizerStorage normalize settings with all abbreviation rules
	 *                          (Config file)
	 * @return normalized token
	 */
	public static String normalizeAbbreviations(String token, Settings normalizerStorage) {
		String normalizedToken = token;
		for (Map.Entry<String, String> touple : normalizerStorage.getAbbreviations().entrySet()) {
			normalizedToken = normalizedToken.replaceAll(Pattern.quote(touple.getKey()), touple.getValue());
		}

		return normalizedToken;
	}
}
