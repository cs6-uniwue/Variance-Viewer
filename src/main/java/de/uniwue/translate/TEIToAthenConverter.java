package de.uniwue.translate;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.cas.CAS;
import org.apache.uima.resource.ResourceInitializationException;

import de.uniwue.mk.kall.formatconversion.teireader.reader.TEIReader;
import de.uniwue.mk.kall.formatconversion.teireader.struct.EKnownXMLFormat;
import de.uniwue.mk.kall.formatconversion.xmlFormat.detection.XMLFormatDetector;
import de.uniwue.wa.server.editor.TextAnnotationStruct;

public class TEIToAthenConverter {

	/**
	 * Reads an input stream from an TEI xml file and outputs an Athen annotation
	 * struct
	 * 
	 * @param is TEI input stream
	 * @return Athen annotation struct
	 *
	 */
	public static TextAnnotationStruct convertTEIToAthen(InputStream is) {
		// Create UIMA CAS from TEI xml file InputStream
		CAS cas;
		try {
			cas = new TEIReader().readDocument(XMLCleaner.clean(is), false, null).getFirst();
		} catch (ResourceInitializationException e) {
			throw new IllegalArgumentException(
					"Unsupported filetype, can't be converted to Athen. InputStream must be derived from a TEI xml file.");
		}

		// Wrap CAS into webAthen struct
		return new TextAnnotationStruct(cas, null);
	}

	/**
	 * Check if an input stream is from type TEI xml
	 * 
	 * @param is
	 * @return
	 */
	public static boolean isTEI(InputStream is) {
		// Read into String
		StringBuilder stringBuilder = new StringBuilder();
		String line = null;
		try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
			while ((line = bufferedReader.readLine()) != null) {
				stringBuilder.append(line);
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String xmlString = stringBuilder.toString();

		// Check basic TEI elements
		// Check for TEI element
		Matcher matcherTEI = Pattern.compile("<TEI(?:[^<>])*>.*</TEI>").matcher(xmlString);
		// Check for teiHeader element
		Matcher matcherTEIheader = Pattern.compile("<teiHeader(?:[^<>])*>.*</teiHeader>").matcher(xmlString);

		if (matcherTEI.find() && matcherTEIheader.find()) {
			// Check if it compiles to TEI
			try {
				EKnownXMLFormat type = XMLFormatDetector.detectFormat(new TEIReader()
						.readDocument(new ByteArrayInputStream(XMLCleaner.clean(xmlString).getBytes()), false, null)
						.getFirst());
				if (type != EKnownXMLFormat.PAGE_XML)
					return true;
				else
					return false;
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		} else {
			return false;
		}
	}
}
