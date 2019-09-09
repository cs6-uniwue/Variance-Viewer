package de.uniwue.compare.token;

import java.util.List;

import de.uniwue.compare.variance.VarianceClassifier;
import de.uniwue.compare.variance.types.Variance;
import de.uniwue.compare.variance.types.VarianceDistance;
import de.uniwue.compare.variance.types.VarianceLineSeparation;
import de.uniwue.compare.variance.types.VarianceMissing;
import de.uniwue.compare.variance.types.VarianceReplacement;
import de.uniwue.compare.variance.types.VarianceTypography;

/**
 * Token wrapper class to compare tokens ignoring any defined variance types
 */
public class VarianceToken extends Token {

	private final List<Variance> variances;

	public VarianceToken(Token token, List<Variance> variances) {
		super(token.getBegin(), token.getEnd(), token.getContent(), token.getContentTag(), token.getAnnotations());
		this.highlight = token.highlight;
		this.variances = variances;
	}

	public List<Variance> getVariances() {
		return variances;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Token other = (Token) obj;
		if(!other.contentTag.contentEquals(contentTag)) 
			return false;
		
		if (this.content == null) {
			if (other.content != null)
				return false;
		} else if(this.content.contentEquals(other.getContent())) {
			return true;
		}
		
		for (Variance var : VarianceClassifier.sortVariances(this.variances)) {
			String thisWork = this.getContent();
			String otherWork = other.getContent();

			// Test for Typography
			if (var instanceof VarianceTypography && thisWork.equals(otherWork))
				return true;

			if (var instanceof VarianceLineSeparation 
					&& VarianceClassifier.normalizeLineSeparation(thisWork).equals(VarianceClassifier.normalizeLineSeparation(otherWork)))
					return true;

			if (var instanceof VarianceMissing) {
				VarianceMissing missing = (VarianceMissing) var;
				if(VarianceClassifier.normalizeMissing(thisWork, missing.getMissing()).equals(VarianceClassifier.normalizeMissing(otherWork, missing.getMissing())))
					return true;
			}

			if (var instanceof VarianceReplacement) {
				VarianceReplacement replace = (VarianceReplacement) var;
				if(VarianceClassifier.normalizeReplace(thisWork, replace.getRules()).equals(VarianceClassifier.normalizeReplace(otherWork, replace.getRules())))
					return true;
			}
			
			if (var instanceof VarianceDistance) {
				VarianceDistance distance = (VarianceDistance ) var;
				int worddistance = VarianceDistance.distance(thisWork, otherWork);
				if(distance.getDistanceMin() <= worddistance && worddistance <= distance.getDistanceMax())
					return true;
			}
		}
		
		return false;
	}

}
