package de.uniwue.compare.token;

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
public class TokenReference<T> {

	
	private final T reference;
	private final Token token;

	public TokenReference(T reference, Token token) {
		if(reference == null)
			throw new NullPointerException("The pointer of a token reference can not be null.");
		this.reference = reference;
		this.token = token;
	}
	
	public T getReference() {
		return reference;
	}

	public Token getToken() {
		return token;
	}

	@Override
	public String toString() {
		return String.format("['%s' -> %s ]", this.reference, token);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;

		if(obj instanceof TokenReference<?>) {
			TokenReference<?> other = (TokenReference<?>) obj;
			return this.reference.equals(other.getReference());
		} else {
			return false;
		}
	}

}
