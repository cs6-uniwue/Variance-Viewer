Variance Viewer: Development
============================
The Variance Viewer is a web application written in Java.
It uses Maven as building tool and is a Spring application.

Frameworks, tools and resources
-------------------------------
* [Maven](https://maven.apache.org/) - Project builder and [repository provider](https://mvnrepository.com/)
* [Spring Framework](https://spring.io/) - Base framework to create the Java web application
* [Java Diff Utilities](https://mvnrepository.com/artifact/com.googlecode.java-diff-utils/diffutils) - Base object comparison framework
* [SASS](https://sass-lang.com/) - Enhanced style sheet language to create css from scss files (enhanced css)


Important notes for development from source
-------------------------------------------
Variance-Viewer can be developed by changing the sources and updating the installation as it is described in [_Installation_ in the README](../README.md#installation).
Only one change should be noted. In order to edit the style of the application SASS is needed.
A base compiled version of the current [scss](https://github.com/cs6-uniwue/Variance-Viewer/blob/master/src/main/webapp/resources/scss/main.scss) can be found in the [css directory](https://github.com/cs6-uniwue/Variance-Viewer/blob/master/src/main/webapp/resources/css/main.css).
In order to edit the css one must make the changes in the scss file and compile it into the css folder as described in the official [SASS guide](https://sass-lang.com/guide)

e.g. (inside the Variance-Viewer folder)
```
sass --watch src/main/webapp/resources/scss:src/main/webapp/resources/css --style compressed
```

Spring
------
Spring Framework is a application framwork and hereby used to create a web application for a Tomcat Server.

The base configuration and code for the web part of Variance-Viewer can be found in 
[src/main/java/de/uniwue/web/](https://github.com/cs6-uniwue/Variance-Viewer/tree/master/src/main/java/de/uniwue/web)
and 
[src/main/webapp/](https://github.com/cs6-uniwue/Variance-Viewer/tree/master/src/main/webapp/).

```
src/main/java/de/uniwue/web/
├── config				
│   └── MvcConfiguration.java		# Base configuration for the web application 
├── controller				 
│   ├── NavigationController.java	# Routing Requestmapping for web paths 
│   └── StorageManager.java		# Base file paths for important files used in the NavigationController
└── view				# Mapping of algorithm results to the browser view
    ├── ConnectedLines.java		
    ├── Content.java
    ├── LineCreator.java
    └── Line.java
```

```
src/main/webapp/
├── resources 		# Public resources accessible in the browser
│   ├── css		# CSS styling 
│   ├── font		# Special fonts for the viewer (e.g. Andron Scriptor Web to cover a wider range of special characters)
│   ├── img		# Different images used in the viewer (e.g. University banner)
│   ├── js		# JavaScript to enhance the usability further than comparing documents
│   └── scss		# SCSS files for SASS to build the css files in resources/css
└── WEB-INF		# Server internal resources and code 
    ├── defaults.txt 	# default configuration used in comparisons
    ├── demo		# different demo files to demonstrate the viewers capability
    ├── tags		# Web TAG files for Java JSP to enhance the views
    ├── views		# Web JSP tags for displaying Java output in the web view
    └── web.xml		# Web application configuration
```

Comparison
----------
The comparison of documents is done in [src/main/java/de/uniwue/compare/](https://github.com/cs6-uniwue/Variance-Viewer/tree/master/src/main/java/de/uniwue/compare).
```
src/main/java/de/uniwue/compare/
├── Annotation.java		# Document annotations of TEI
├── CharReference.java		# Reference to specific characters inside a token
├── ConnectedContent.java	# Connection between original and revised tokens (DELETE, EQUAL, …)
├── ContentType.java		# Enumeration with all available change types (INSERT, DELETE, CHANGE, EQUAL)
├── DiffCreator.java		# Base token comparator. Compares lists of tokens and classifies their variances
├── Diff.java			# A comparator that tokenizes and compares documents strings (with annotations)
├── DocumentType.java		# Recognized document types (PLAINTEXT, TEI, XML)
├── Settings.java		# Settings reader to interpret user settings like variance rules
├── SpecialCharacter.java	# Collection of special character groups e.g. a multitude of different spaces (SPACE, NO-BREAK SPACE,…)
├── token			# Token types with each refering the `Token` class and providing a different `equals` method
│   ├── AnnotationToken.java
│   ├── TextToken.java
│   ├── Token.java
│   └── VarianceToken.java
├── Tokenizer.java		# Tokenizer that creates Lists of `Token` from documents (with annotations)
└── variance
    ├── types			# Different types of variances with predefined and baseblock variances
    ├── VarianceClassifier.java	# Helper method to classify variance changes between two documents
    └── VarianceStatistics.java	# Basic statistics calculator from a finished comparison, via a list of ConnectedContents
```


Translate
---------
The translater code in [src/main/java/de/uniwue/translate/](https://github.com/cs6-uniwue/Variance-Viewer/tree/master/src/main/java/de/uniwue/translate) is used to read and convert the comparison data from and into different formats, as for example TEI.

```
src/main/java/de/uniwue/translate/
├── DiffExporter.java
├── TEIToAthenConverter.java
└── XMLCleaner.java
```



TODO
----
Some work that could be done to improve the Variance Viewer further.
* Refactor [src/main/java/de/uniwue/compare/DiffCreator](https://github.com/cs6-uniwue/Variance-Viewer/blob/master/src/main/java/de/uniwue/compare/DiffCreator.java) into a factory pattern.
