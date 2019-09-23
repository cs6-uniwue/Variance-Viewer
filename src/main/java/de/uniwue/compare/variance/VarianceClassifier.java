package de.uniwue.compare.variance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import de.uniwue.compare.CharReference;
import de.uniwue.compare.ConnectedContent;
import de.uniwue.compare.ContentType;
import de.uniwue.compare.SpecialCharacter;
import de.uniwue.compare.token.Token;
import de.uniwue.compare.variance.types.Variance;
import de.uniwue.compare.variance.types.VarianceContent;
import de.uniwue.compare.variance.types.VarianceDistance;
import de.uniwue.compare.variance.types.VarianceLine;
import de.uniwue.compare.variance.types.VarianceLineSeparation;
import de.uniwue.compare.variance.types.VarianceMissing;
import de.uniwue.compare.variance.types.VarianceReplacement;
import de.uniwue.compare.variance.types.VarianceSeparation;
import de.uniwue.compare.variance.types.VarianceType;
import de.uniwue.compare.variance.types.VarianceTypography;
import difflib.Delta;
import difflib.Delta.TYPE;
import difflib.DiffUtils;
import difflib.Patch;

public class VarianceClassifier {


	/**
	 * Classify the changes in an insert or delete token. Normalizes the token to
	 * identify the variance type step by step.
	 * 
	 * Separation -> Punctuation -> (else) Content
	 * 
	 * @param content           Token to classify
	 * @param type              ContentType of the variance type (INSERT or DELETE. 
	 * 							Every other type will result in VarianceType NONE)
	 * @param normalizerStorage Normalize settings and rules
	 * @return Variance Type of the token
	 */
	public static String classifySingle(Token content, List<? extends Token> document, ContentType type, List<Variance> varianceTypes) {
		if (type.equals(ContentType.INSERT) || type.equals(ContentType.DELETE) || type.equals(ContentType.CHANGE)) {
			for (Variance var : sortVariances(varianceTypes)) {
				String text = content.getContent();
				if (var instanceof VarianceLineSeparation 
						&& normalizeLineSeparation(text).equals(""))
						return var.getName();

				if (var instanceof VarianceMissing
						&& normalizeMissing(text, ((VarianceMissing) var).getMissing()).equals(""))
						return var.getName();

				if (var instanceof VarianceContent)
					return var.getName();
			}
			return "NONE"; // This should never have to be called, unless the varianceType does not contain VarianceContent
		} else {
			return "NONE";
		}
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
	 * @param type              Content type (EQUAL, INSERT, DELETE, CHANGE)
	 * @param normalizerStorage Normalize settings and rules
	 * @return Variance Type of the tuple of tokens
	 */
	public static String classifyTouple(Token original, Token revised, ContentType type,
			List<Variance> varianceTypes) {
		if (type.equals(ContentType.EQUAL))
			return "NONE";
		
		
		for (Variance var : sortVariances(varianceTypes)) {
			String originalWork = original.getContent();
			String revisedWork = revised.getContent();

			// Test for Typography
			if (var instanceof VarianceTypography && originalWork.equals(revisedWork))
				return var.getName();

			if (var instanceof VarianceLineSeparation 
					&& normalizeLineSeparation(originalWork).equals(normalizeLineSeparation(revisedWork)))
					return var.getName();

			if (var instanceof VarianceMissing) {
				VarianceMissing missing = (VarianceMissing) var;
				if(normalizeMissing(originalWork, missing.getMissing()).equals(normalizeMissing(revisedWork, missing.getMissing())))
					return var.getName();
			}

			if (var instanceof VarianceReplacement) {
				VarianceReplacement replace = (VarianceReplacement) var;
				if(normalizeReplace(originalWork, replace.getRules()).equals(normalizeReplace(revisedWork, replace.getRules())))
					return var.getName();
			}
			
			if (var instanceof VarianceDistance) {
				VarianceDistance distance = (VarianceDistance ) var;
				int worddistance = VarianceDistance.distance(originalWork, revisedWork);
				if(distance.getDistanceMin() <= worddistance && worddistance <= distance.getDistanceMax())
					return var.getName();
			}
			
			if (var instanceof VarianceContent)
				return var.getName();

		}
		
		return "NONE";
	}

	public static List<ConnectedContent> classifyMultiple(List<ConnectedContent> contents, List<Variance> varianceTypes){
		final String CONTENT = Variance.CONTENT.getName();
		final String SEPARATION = Variance.SEPARATION.getName();
		final List<String> backlogVariances = new ArrayList<>();
		backlogVariances.add(Variance.CONTENT.getName());
		for (Variance var : varianceTypes) {
			if (var.getType().equals(VarianceType.DISTANCE))
				backlogVariances.add(var.getName());
		}
		
		for(ConnectedContent content: contents) {
			if(!backlogVariances.contains(content.getVarianceType())) {
				throw new IllegalArgumentException(
						String.format("Unable to classify ConnectedContents '%s'. "+
										"ConnectedContents must be of VarianceType '%s', be a 'DISTANCE' or be a separation",
										content.getVarianceType(), CONTENT));
			}
		}

		List<ConnectedContent> classified = new ArrayList<>();

		// Combine connected contents
		List<Token> originalToken = new ArrayList<>();
		List<Token> revisedToken = new ArrayList<>();
		for(ConnectedContent content : contents) {
			originalToken.addAll(content.getOriginal());
			revisedToken.addAll(content.getRevised());
		}
		
		// Separate tokens into characters
		List<CharReference<String>> original = new LinkedList<>();
		List<CharReference<String>> revised = new LinkedList<>();
		ListIterator<Token> origIterator = originalToken.listIterator();
		while(origIterator.hasNext()) {
			int index = origIterator.nextIndex();
			Token o = origIterator.next();
			original.addAll(Arrays.stream(o.getContent().split("(?!^)"))
				.map(c -> new CharReference<>(c, index, false)).collect(Collectors.toList()));
			if(origIterator.hasNext()) // Text separations are indexed by their predecessor
				original.add(new CharReference<>(" ", index, true));
		}
		ListIterator<Token> revIterator = revisedToken.listIterator();
		while(revIterator.hasNext()) {
			int index = revIterator.nextIndex();
			Token r = revIterator.next();
			revised.addAll(Arrays.stream(r.getContent().split("(?!^)"))
				.map(c -> new CharReference<>(c, index, false)).collect(Collectors.toList()));
			if(revIterator.hasNext()) // Text separations are indexed by their predecessor
				revised.add(new CharReference<>(" ", index, false));
		}

		// Search for whitespace changes in character based changes
		Patch<CharReference<String>> annotationPatch = DiffUtils.diff(original, revised);
		
		// Init tokens that may be split
		Set<Integer> separatedOriginal = new HashSet<>();
		Set<Integer> separatedRevised = new HashSet<>();
		for(int i=0; i  < originalToken.size(); i++) 
			separatedOriginal.add(i);
		for(int i=0; i  < revisedToken.size(); i++) 
			separatedRevised.add(i);
		
		// Test all differences and remove every token that is changed by more than a separation
		for (Delta<CharReference<String>> delta : annotationPatch.getDeltas()) {
			// Search for deletions and insert, for separations
			TYPE type = delta.getType();
			if (type.equals(TYPE.INSERT)) {
				// Check if this delta describes a split and remove every token that
				// is change otherwise
				for(CharReference<String> r: delta.getRevised().getLines()) {
					if (!r.getReference().matches(SpecialCharacter.WHITESPACES_REGEX+"+")) {
						separatedRevised.remove(r.getTokenIndex());
					}
				}
			} else if(type.equals(TYPE.DELETE)) {
				// Check if this delta describes a split and remove every token that
				// is change otherwise
				for(CharReference<String> r: delta.getOriginal().getLines()) {
					if (!r.getReference().matches(SpecialCharacter.WHITESPACES_REGEX+"+")) {
						separatedOriginal.remove(r.getTokenIndex());
					}
				}
			} else if(type.equals(TYPE.CHANGE)) { 
				// Set delta not not being a split and remove all changed tokens
				for(CharReference<String> r: delta.getRevised().getLines()) 
					separatedRevised.remove(r.getTokenIndex());
				for(CharReference<String> r: delta.getOriginal().getLines()) 
					separatedOriginal.remove(r.getTokenIndex());
			}
		}
		
		// Create new Connected Contents
		origIterator = originalToken.listIterator();
		revIterator = revisedToken.listIterator();
		while(origIterator.hasNext() || revIterator.hasNext()) {
			List<Token> currentOrig = new ArrayList<>();
			List<Token> currentRev = new ArrayList<>();
			while(origIterator.hasNext() 
					&& !separatedOriginal.contains(origIterator.nextIndex())) {
				currentOrig.add(origIterator.next());
			}
			while(revIterator.hasNext() 
					&& !separatedRevised.contains(revIterator.nextIndex())) {
				currentRev.add(revIterator.next());
			}
			// Add all content not separated by whitespace 
			String varianceType = CONTENT;
			//(TODO) check for multiple connected contents in-between currentOrig and currentRev
			if(currentOrig.size() > 0 && currentRev.size() > 0) {
				if(currentOrig.size() == 1 && currentRev.size() == 1) 
					varianceType = classifyTouple(currentOrig.get(0), currentRev.get(0), ContentType.CHANGE, varianceTypes);
				classified.add(new ConnectedContent(currentOrig, currentRev, ContentType.CHANGE, varianceType));
			} else if (currentOrig.size() > 0) {
				classified.add(new ConnectedContent(currentOrig, currentRev, ContentType.DELETE, CONTENT));
			} else if (currentRev.size() > 0) {
				classified.add(new ConnectedContent(currentOrig, currentRev, ContentType.INSERT, CONTENT));
			}
			currentOrig = new ArrayList<>();
			currentRev = new ArrayList<>();
			
			while(origIterator.hasNext() 
					&& separatedOriginal.contains(origIterator.nextIndex())) {
				currentOrig.add(origIterator.next());
			}
			while(revIterator.hasNext() 
					&& separatedRevised.contains(revIterator.nextIndex())) {
				currentRev.add(revIterator.next());
			}
			// Add all content not separated by whitespace 
			if(currentOrig.size() > 0 && currentRev.size() > 0) {
				classified.add(new ConnectedContent(currentOrig, currentRev, ContentType.CHANGE, SEPARATION));
			} else if (currentOrig.size() > 0) {
				classified.add(new ConnectedContent(currentOrig, currentRev, ContentType.DELETE, CONTENT));
			} else if (currentRev.size() > 0) {
				classified.add(new ConnectedContent(currentOrig, currentRev, ContentType.INSERT, CONTENT));
			}
		}
		
		return classified;
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
	public static LinkedList<Token> normalize(List<Token> list, List<Variance> varianceTypes) {
		LinkedList<Token> normalized = new LinkedList<Token>();

		for (Token token : list) {
			String content = normalize(token.getContent(), varianceTypes);
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
	public static String normalize(String token, List<Variance> varianceTypes) {
		String normalized = token;

		for (Variance var : sortVariances(varianceTypes)) {
			if (var instanceof VarianceLineSeparation)
				normalized = normalizeLineSeparation(normalized);

			if (var instanceof VarianceMissing) 
				normalized = normalizeMissing(normalized, ((VarianceMissing) var).getMissing());

			if (var instanceof VarianceReplacement) 
				normalized = normalizeReplace(normalized, ((VarianceReplacement) var).getRules());
			

		}
		return normalized;
	}

	/**
	 * Normalize a token to check for line separation. E.g. "Test\n" "Test" can be
	 * normalized to "Test", which helps to identify a line separation variance
	 * type.
	 * 
	 * @param token token to normalize
	 * @return normalized token
	 */
	public static String normalizeLineSeparation(String token) {
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
	public static String normalizeMissing(String token, List<String> missing) {
		if (missing.size() > 0) {
			List<String> quoted = missing.stream().map(Pattern::quote).collect(Collectors.toList());
			return token.replaceAll(String.format("(%s)", String.join("|", quoted)), "");
		} else {
			return token;
		}
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
	public static String normalizeReplace(String token, Map<String, String> rules) {
		String normalizedToken = token;
		for (Map.Entry<String, String> touple : rules.entrySet()) {
			normalizedToken = normalizedToken.replaceAll(Pattern.quote(touple.getKey()), touple.getValue());
		}

		return normalizedToken;
	}
	
	public static List<Variance> sortVariances(List<Variance> variances) {
		List<Variance> sorted = new ArrayList<>();

		// Add all VarianceTypography
		sorted.addAll(variances.stream().filter(v -> v instanceof VarianceTypography)
				.sorted((v1,v2) -> Integer.compare(v1.getPriority(), v2.getPriority()))
				.collect(Collectors.toList()));

		// Add all VarianceMissing sorted by priority 
		sorted.addAll(variances.stream().filter(v -> v instanceof VarianceMissing)
				.sorted((v1,v2) -> Integer.compare(v1.getPriority(), v2.getPriority()))
				.collect(Collectors.toList()));
		
		// Add all VarianceReplacement sorted by priority 
		sorted.addAll(variances.stream().filter(v -> v instanceof VarianceReplacement)
				.sorted((v1,v2) -> Integer.compare(v1.getPriority(), v2.getPriority()))
				.collect(Collectors.toList()));
		
		// Add all VarianceLine sorted by priority 
		sorted.addAll(variances.stream().filter(v -> v instanceof VarianceLine)
				.sorted((v1,v2) -> Integer.compare(v1.getPriority(), v2.getPriority()))
				.collect(Collectors.toList()));

		// Add all VarianceSeparation sorted by priority 
		sorted.addAll(variances.stream().filter(v -> v instanceof VarianceSeparation)
				.sorted((v1,v2) -> Integer.compare(v1.getPriority(), v2.getPriority()))
				.collect(Collectors.toList()));

		// Add all VarianceDistance sorted by priority 
		sorted.addAll(variances.stream().filter(v -> v instanceof VarianceDistance)
				.sorted((v1,v2) -> Integer.compare(v1.getPriority(), v2.getPriority()))
				.collect(Collectors.toList()));

		// Add all VarianceContent
		sorted.addAll(variances.stream().filter(v -> v instanceof VarianceContent)
				.sorted((v1,v2) -> Integer.compare(v1.getPriority(), v2.getPriority()))
				.collect(Collectors.toList()));

		return sorted;
	}
	
}
