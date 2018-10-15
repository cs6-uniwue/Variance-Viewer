package de.uniwue.translate;

import java.io.InputStream;

import org.apache.uima.cas.CAS;
import org.apache.uima.resource.ResourceInitializationException;

import de.uniwue.mk.kall.formatconversion.teireader.reader.TEIReader;
import de.uniwue.mk.kall.formatconversion.teireader.struct.EKnownXMLFormat;
import de.uniwue.mk.kall.formatconversion.xmlFormat.detection.XMLFormatDetector;
import de.uniwue.wa.server.editor.TextAnnotationStruct;

public class TEIToAthenConverter {
	
	/**
	 * Reads an input stream from an TEI xml file and outputs an Athen annotation struct
	 * 
	 * @param is
	 *            TEI input stream
	 * @return Athen annotation struct
	 *
	 */
	public static TextAnnotationStruct convertTEIToAthen(InputStream is) {
		// Create UIMA CAS from TEI xml file InputStream
		CAS cas;
		try {
			cas = new TEIReader().readDocument(is, false, null).getFirst();
		} catch (ResourceInitializationException e) {
			throw new IllegalArgumentException(
					"Unsupported filetype, can't be converted to Athen. InputStream must be derived from a TEI xml file.");
		}

		// Wrap CAS into webAthen struct
		return new TextAnnotationStruct(cas, null);
	}

	/**
	 * Check if an input stream is from type TEI xml
	 * @param is
	 * @return
	 */
	public static boolean isTEI(InputStream is) {
		try {
			EKnownXMLFormat type = XMLFormatDetector.detectFormat(new TEIReader().readDocument(is, false, null).getFirst());
			if(type == EKnownXMLFormat.TEI)
				return true;
			else
				return false;
		} catch (ResourceInitializationException e) {
			return false;
		}
	}
}
