package de.uniwue.compare.variance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
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
import de.uniwue.compare.variance.types.VarianceTypography;
import difflib.Chunk;
import difflib.Delta;
import difflib.Delta.TYPE;
import difflib.DiffUtils;
import difflib.Patch;
import difflib.myers.Equalizer;

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

	public static List<ConnectedContent> classifyMultiple(List<ConnectedContent> contents){
		List<ConnectedContent> classified = new ArrayList<>();
		
		// Separate tokens into characters
		List<CharReference<String>> original = new LinkedList<>();
		List<CharReference<String>> revised = new LinkedList<>();
		for(ConnectedContent content : contents) {
			Iterator<Token> origIterator = content.getOriginal().iterator();
			while(origIterator.hasNext()) {
				Token o = origIterator.next();
				original.addAll(Arrays.stream(o.getContent().split("(?!^)"))
					.map(c -> new CharReference<>(c, o, content)).collect(Collectors.toList()));
				if(origIterator.hasNext())
					original.add(new CharReference<>(" ", null, null));
			}
			Iterator<Token> revIterator = content.getRevised().iterator();
			while(revIterator.hasNext()) {
				Token r = revIterator.next();
				revised.addAll(Arrays.stream(r.getContent().split("(?!^)"))
					.map(c -> new CharReference<>(c, r, content)).collect(Collectors.toList()));
				if(revIterator.hasNext())
					revised.add(new CharReference<>(" ", null, content));
			}
		}
	
		
		// Search for whitespace changes in character based changes
		Patch<CharReference<String>> annotationPatch = DiffUtils.diff(original, revised);
		
		// Filter differences for whitespace changes
		List<Delta<CharReference<String>>> whitespaceDeltas = new ArrayList<>(); 
		for (Delta<CharReference<String>> delta : annotationPatch.getDeltas()) {
			// Search for deletions and insert, for separations
			TYPE type = delta.getType();
			Chunk<CharReference<String>> chunk = null;
			if (type.equals(TYPE.INSERT)) 
				chunk = delta.getRevised();
			else if(type.equals(TYPE.DELETE)) 
				chunk = delta.getOriginal();
			else 
				continue;

			// Check for everything other than whitespace changes
			for(CharReference<String> r: chunk.getLines()) {
				if (!r.getReference().matches(SpecialCharacter.WHITESPACES_REGEX+"+")) {
					continue;
				}
			}
			// Add to whitespace changes, since the only changes in this Delta are
			// the insertion or deletion of whitespaces
			whitespaceDeltas.add(delta);
		}
		
		// Iterate over all changes, split connected contents with whitespace changes 
		int origIndex = -1;
		int revIndex = -1;
		for (Delta<CharReference<String>> delta : whitespaceDeltas) {
			TYPE type = delta.getType();
			if (type.equals(TYPE.INSERT)) {
				Chunk<CharReference<String>> insert = delta.getRevised();
				for(CharReference<String> r: insert.getLines()) {
					if (!r.getReference().matches(SpecialCharacter.WHITESPACES_REGEX+"+")) {
						continue;
					}
				}
				whitespaceDeltas.add(delta);
			}

			if (type.equals(TYPE.DELETE)) {
				Chunk<CharReference<String>> delete = delta.getOriginal();
				for(CharReference<String> r: delete.getLines())
					System.out.println("- \'"+r.getReference()+"\'");
			}


			int origStart = delta.getOriginal().getPosition();
			int revStart = delta.getRevised().getPosition();
			
			if (origStart > origIndex + 1 || revStart > revIndex + 1) {
				// Add Unchanged Content between diffs
				List<CharReference<String>> unchangedOrig = original.subList(origIndex + 1, origIndex);
				List<CharReference<String>> unchangedRev = revised.subList(revIndex + 1, revIndex);
				
			}
			
			
			origStart = delta.getOriginal().last();
			revStart = delta.getRevised().last();
		}
		
		return contents;
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
