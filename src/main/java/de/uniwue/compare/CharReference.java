package de.uniwue.compare;

/**
 * A token reference to compare objects that reference a token.
 *
 * e.g. splitting a token into its characters, comparing the characters
 *  and applying changes found onto the tokens.
 *
 * Token1:"Test"
 * Token2:"Test2"
 * 
 * T->Token1 == T->Token2
 * e->Token1 == e->Token2
 * s->Token1 == s->Token2
 * t->Token1 == t->Token2
 * 				2->Token2
 * 
 *
 * @param <T> Token reference to compare
 */
public class CharReference<T> {

	
	private final T reference;
	private final int tokenIndex;
	private final boolean isSeparator;

	public CharReference(T reference, int tokenIndex, boolean isSeparator) {
		if(reference == null)
			throw new NullPointerException("The pointer of a token reference can not be null.");
		this.reference = reference;
		this.tokenIndex = tokenIndex;
		this.isSeparator = isSeparator;
	}
	
	public T getReference() {
		return reference;
	}

	public int getTokenIndex() {
		return tokenIndex;
	}

	public boolean isSeparator() {
		return isSeparator;
	}	
	
	@Override
	public String toString() {
		return String.format("['%s' -> %d ]", this.reference, tokenIndex);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;

		if(obj instanceof CharReference<?>) {
			CharReference<?> other = (CharReference<?>) obj;
			return this.reference.equals(other.getReference());
		} else {
			return false;
		}
	}

}
