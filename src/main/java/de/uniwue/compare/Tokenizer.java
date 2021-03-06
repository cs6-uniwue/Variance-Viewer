package de.uniwue.compare;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import de.uniwue.compare.token.Token;

public class Tokenizer {

	/**
	 * Tokenize a simple string content to Tokens
	 * 
	 * @param content
	 *            document sting content
	 * @return list of all tokens
	 */
	public static List<Token> tokenize(String content, String contentTag) {
		final List<Token> tokens = new ArrayList<Token>();

		if (content.length() > 0) {
			final LinkedList<String> tokenTexts = new LinkedList<String>();

			// Split at new line and keep empty strings (separator before and after)
			final Iterator<String> tokenLines = Arrays.asList(content.split(System.lineSeparator(), -1)).iterator();

			while (tokenLines.hasNext()) {
				final String line = tokenLines.next();
				// Add all tokens in each line
				if (!line.equals(""))
					tokenTexts.addAll(Arrays.asList(line.split("((?<="+SpecialCharacter.SPACES_REGEX+")|(?="+SpecialCharacter.SPACES_REGEX+"))")));

				// Add line separators between lines
				if (tokenLines.hasNext())
					tokenTexts.add(System.lineSeparator());
			}

			// Convert string tokens to token objects
			int pointer = 0;
			for (String tokenText : tokenTexts) {
				if (tokenText.matches(SpecialCharacter.SPACES_REGEX+"+"))
					pointer += tokenText.length();
				else
					tokens.add(new Token(pointer, pointer += tokenText.length(), tokenText, contentTag));
			}
		}
		return tokens;
	}

	/**
	 * Tokenize a document with annotations inside of tokenizable tags.
	 * 
	 * @param content
	 *            document string content
	 * @param annotations
	 *            document annotations
	 * @param contentTags
	 *            tags to tokenize inside
	 * @return list of all tokens
	 */
	public static List<Token> tokenize(String content, List<Annotation> annotations, List<String> contentTags) {
		List<Annotation> tokenizable = annotations.stream().filter(
				a -> a.getFeatures().containsKey("TagName") && contentTags.contains(a.getFeatures().get("TagName")))
				.collect(Collectors.toList());
		

		List<Token> tokens = new ArrayList<>();
		for (Annotation annotation : tokenizable) {
			final int begin = annotation.getBegin();
			final int end = annotation.getEnd();

			List<Token> curTokens = tokenize(content.substring(begin, end),annotation.getFeatures().get("TagName"));
			curTokens.forEach(t -> t.moveDelta(begin));
			tokens.addAll(curTokens);
		}

		// Add Annotations
		for (Annotation annotation : annotations) {
			final Map<String, String> features = annotation.getFeatures();

			// Extract attributes from features
			final Map<String, String> attributes = new HashMap<String,String>();
			if (features.containsKey("Attributes")) {
				String[] attribute_strings = features.get("Attributes").split("##");
				for(String attribute_string: attribute_strings) {
					if(attribute_string.contains("=")) {
						String[] att_val = attribute_string.split("=");
						attributes.put(att_val[0], att_val[1]);
					}
				}
			}
			
			if(attributes.containsKey("rend")) {
				final String rend = attributes.get("rend");
				final int annotationBegin = annotation.getBegin();
				final int annotationEnd = annotation.getEnd();
				if (annotationBegin == annotationEnd) {
					// Does not include text

					// Find contentTag
					String contentTag = "";
					for (Annotation contentAnnotations : tokenizable) {
						final int begin = contentAnnotations.getBegin();
						final int end = contentAnnotations.getEnd();
						if(begin <= annotationBegin && annotationEnd <= end) {
							contentTag = annotation.getFeatures().get("TagName");
							break;
						}
					}

					// Create token without text
					Token token = new Token(annotationBegin, annotationEnd, "", contentTag);
					token.addAnnotation(rend);
					tokens.add(token);
					// Sort tokens
					tokens.sort((t1, t2) -> {
						int compare = Long.compare(t1.getBegin(), t2.getBegin());
						if (compare == 0)
							compare = Long.compare(t1.getEnd(), t2.getEnd());
						if (compare == 0)
							compare = t1.getContent().compareTo(t2.getContent());
						return compare;
					});
				} else {
					for (Token token : tokens) {
						final long tokenBegin = token.getBegin();
						final long tokenEnd = token.getEnd();

						if (tokenBegin > annotationEnd)
							break;

						if (annotationBegin <= tokenBegin && tokenEnd <= annotationEnd) {
							token.addAnnotation(rend);
						}
					}

				}
			}
		}
		
		return tokens;
	}
}
