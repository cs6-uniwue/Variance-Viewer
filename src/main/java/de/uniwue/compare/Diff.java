package de.uniwue.compare;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import de.uniwue.compare.token.TextToken;
import de.uniwue.compare.token.Token;
import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;
import difflib.PatchFailedException;

/**
 * Implementation of ChangeAlgorithm with google-diff-match-patch
 * Compare two plain text documents or TEI contents with all tags extracted as Annotations
 */
public class Diff {
	/**
	 * Compare two plain text documents 
	 * 
	 * @param content1 Content String of document 1
	 * @param content2 Content String of document 2
	 * @param settings Settings with rules to consider for the variance analysis
	 * @return
	 * @throws PatchFailedException
	 */
	public static List<ConnectedContent> comparePlainText(String content1, String content2, Settings settings) {
		List<Token> tokens1 = Tokenizer.tokenize(content1,"plain");
		List<Token> tokens2 = Tokenizer.tokenize(content2,"plain");

		// Compute diff. Get the Patch object.
		Patch<Token> patch = DiffUtils.diff(tokens1, tokens2);

		return DiffCreator.patch(tokens1, tokens2, patch.getDeltas(), false, settings);
	}

	/**
	 * Compare two TEI documents that are each split into String content (xml tags removed) and
	 * Annotations (extracted xml tags in a pointer format)
	 * 
	 * @param content1Text Content String of document 1
	 * @param content2Text Content String of document 2
	 * @param annotations1 XML tags of document 1 in Annotation pointer format
	 * @param annotations2 XML tags of document 2 in Annotation pointer format
	 * @param settings Settings with compare body tags and rules to consider for the variance analysis
	 * @return
	 */
	public static List<ConnectedContent> compareXML(String content1Text, String content2Text,
			Collection<Annotation> annotations1, Collection<Annotation> annotations2, Settings settings) {

		final Comparator<Annotation> annotationComparator = (a1, a2) -> {
			int compare = Long.compare(a1.getBegin(), a2.getBegin());
			return (compare != 0) ? compare : Long.compare(a1.getEnd(), a2.getEnd());
		};

		final List<Annotation> sortedAnnotations1 = new ArrayList<>(annotations1);
		sortedAnnotations1.sort(annotationComparator);
		final List<Annotation> sortedAnnotations2 = new ArrayList<>(annotations2);
		sortedAnnotations2.sort(annotationComparator);
		
		// Compute diff. Get the Patch object.
		final List<Token> tokens1 = Tokenizer.tokenize(content1Text, sortedAnnotations1,
				settings.getContentTags());
		final List<Token> tokens2 = Tokenizer.tokenize(content2Text, sortedAnnotations2,
				settings.getContentTags());

		final List<TextToken> textTokens1 = tokens1.stream().map(t -> t.getTextToken()).collect(Collectors.toList());
		final List<TextToken> textTokens2 = tokens2.stream().map(t -> t.getTextToken()).collect(Collectors.toList());

		// Text compare
		final Patch<TextToken> textPatch = DiffUtils.diff(textTokens1, textTokens2);
		final List<Delta<TextToken>> textDeltas = textPatch.getDeltas();

		return DiffCreator.patch(textTokens1, textTokens2, textDeltas, true, settings);
	}

}