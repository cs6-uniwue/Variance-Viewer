Variance Viewer: Functionality and procedures
=============================================
The Variance Viewer is a tool to compare documents, find and analyse their variances. 
Differences between documents are processed on word level, which are classified by user defined rules.

This document will describe the basic functionality and code structure.

Input
=====
The Variance Viewer can to either compare two plain text documents or two [TEI documents](https://tei-c.org/).
If both documents are of type TEI, they will parsed and in addition to a text comparision, be compared on visual differences via the `rend` attribute.
_(Every pair of documents/files where not both are of type TEI, will be treated as plain text documents)_

e.g. (Plain text)
* Document 1:
	> This is a test.
* Document 2:		
	> This is a text.
* Difference (without variances classification):		
	> This is a ~~test~~text. 


e.g. (TEI)
* Dokument 1:
```
<TEI>
	<teiHeader> 
		<fileDesc>
			<titleStmt> <title>Test 1</title> </titleStmt>
		</fileDesc>
	</teiHeader>
	<text>
		<body>
			<p>This is a text.</p>
		</body>
	</text>
 </TEI>
```

> This is a test.
_(Possible visualization)_


* Dokument 2:
```
<TEI>
	<teiHeader> 
		<fileDesc>
			<titleStmt> <title>Test 2</title> </titleStmt>
		</fileDesc>
	</teiHeader>
	<text>
		<body>
			<p><h1 rend='bold'>This</hi> is a text.</p>
		</body>
	</text>
 </TEI>
```

> **This** is a text.
_(Possible visualization)_

* Difference (without variances classification):		
	> ~~This~~__This__ is a test.
	_(Possible visualization)_



Process
-------
The comparision of documents with the Variance Viewer are not restricted on content changes. 
Users can define rules by which the documents will be compared.
This process is comprisered of the the (_Parsing_,) _Tokenizing_, _Comparison_ and _Variance finding_.


### Parsing (TEI)
The parsing of documents is depending on the type of input documents.
If either or both documents are plain text then no parsing is executed.
If both are TEI documents, than they are parsed by extracting the content predefined "Content Tags" with their `rend` attributes intact.
* `Content Tags`: XML tags in TEI documents in which the main content is defined (defined in the settings). 
	* e.g. `p` for the comparison between paragraphs. (_Default:_ `head` and `p`)
	* Meta tags about dokument namen etc. are thereby filtered out
* `rend`: Attributes of TEI xml tags, with information about the prefered presentation.
	* There are no predefined rend types in TEI. Every tool that displays TEI must interpret it per document. The presentation in this tool can be tweaked via css with user settings.
	* e.g. `rend="XXL"` could be interpreted as an extra big font size.

### Tokenizing
The contents of two documents are compared on word level.
Those words are thereby extracted as tokens with additional information.

Tokens are single words separated by whitespace and contain:
* Text (a word)
* Their position in the document (Character based) 
* `rend` attribute(s) if applicable (TEI xml)
* `Content Tag` from which they stem if applicable (TEI xml)


### Comparision
The comparison of documents is done with the help of the Google library [Java Diff Utilities](https://mvnrepository.com/artifact/com.googlecode.java-diff-utils/diffutils).
This library resieves two list of objects, which are compared to another with the objects equals method.
The result are differences of `ADD`, `REMOVE`, `CHANGE` and `EQUAL`, which allows to transform one list into the other.

e.g.:
```
List 1			List 2
======			======
a			a
b			b
c			x
d			c
e			e
f			g

Differenes between list 2 over list 1
=====================================
a - EQUAL
b - EQUAL
x - ADD
c - EQUAL
d - REMOVE
e - EQUAL
g - CHANGE to f

```

This concept is used within the list of tokens.

Tokens can be transformed into two different subtypes, which are compared in different ways:
* Text: Two text tokens are identical if:
	* They contain the same text
	* Are from the same `Content Tag` (only in TEI)
* Annotation (with `rend`): Two annotation tokens are identical if:
	* They contain the same text
	* They have the same `rend` attributes 
	* Are from the same `Content Tag` (only in TEI)

#### Variance finding
The differences between tokens can be classified into variance types via user defined rules.
There are three predefined variance types and three user defined variance types.
The predefined variance type are base types that are always used in the classification, while the user defined variance type are building blocks to create ones own variance types.
Every variance type but CONTENT and SEPARATION can be configurated.

* Predefined:
	* TYPOGRAPHY - Only present in TEI texts. Represents the changes in how text is displayed (utilizes the rend attribute) e.g. `<p rend="xxl">Test</p>` changed to `<p>Test</p>`
	* SEPARATION - Represents the separation changes between multiple tokens. e.g. "Thistest" changed to "This test" 
	* CONTENT - The fallback variance type for all changes that can not be classified as any other variance.
* User defined:
	* MISSING - (sequence of) characters missing in one word but present in the other. 
		e.g. missing characters "x" with "Testx" changed to "Test" 
		This allows to create variance types with different character sets that can be found as "missing" 
	* DISTANCE - levenshtein distance on character basis between two words (with min and max distance).
		e.g. min distance:0 and max distance:2 with "Test" changed to "Text" 
		This allows to create variance types with difference error ranges
	* REPLACEMENT - Changes between words, where a sequence of characters of one word are changed to another sequence of characters in the other word.
		e.g. Replacement Rule "ae ä" with "Bär" changed to "Baer"
		This allows to create variance types with different types of replacements between words/tokens 

_More about the configuration of variance types see [__User defined Variances__ in the README](../README.md#user-defined-variances)_

