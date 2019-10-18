package de.uniwue.web.controller;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

import de.uniwue.compare.Annotation;
import de.uniwue.compare.ConnectedContent;
import de.uniwue.compare.Diff;
import de.uniwue.compare.DocumentType;
import de.uniwue.compare.Settings;
import de.uniwue.compare.SpecialCharacter;
import de.uniwue.compare.variance.VarianceClassifier;
import de.uniwue.compare.variance.VarianceStatistics;
import de.uniwue.compare.variance.types.Variance;
import de.uniwue.translate.DiffExporter;
import de.uniwue.translate.TEIToAthenConverter;
import de.uniwue.translate.XMLCleaner;
import de.uniwue.wa.server.editor.TextAnnotationStruct;
import de.uniwue.web.view.LineCreator;

@Controller
public class NavigationController {
	@Autowired
	ServletContext servletContext;

	@RequestMapping(value = "/")
	public String home(Model model) {
		return "home";
	}

	@RequestMapping(value = "/home")
	public String home2(Model model) {
		return "redirect:/";
	}

	@RequestMapping(value = "/view", method = RequestMethod.POST)
	public String view(Model model,
			@RequestParam("file1") MultipartFile file1,
			@RequestParam("file2") MultipartFile file2,
			@RequestParam("settings") String settingsType,
			@RequestParam(value = "settingsFile", required = false) MultipartFile settingsFile,
			@RequestParam(value = "normalize", required = false) String normalize,
			@RequestParam(value = "normalizeFile", required = false) MultipartFile normalizeFile) {

		if (!file1.isEmpty() && !file2.isEmpty()) {
			// Read normalize files / settings
			Settings settings = null;
			switch(settingsType) {
			case "default":
				settings = new Settings(StorageManager.getDefault(servletContext));
				break;
			case "user":
				if (!settingsFile.isEmpty()) {
					try {
						settings = new Settings(StorageManager.getSettings(settingsFile, servletContext));
					} catch (IllegalArgumentException e) {
						// Invalid settingsString
						model.addAttribute("warning", "Invalid settings file. " + e.getMessage() + " Redirected to home.");
						return home(model);
					}
				} else {
					model.addAttribute("warning", "User settings file was selected, but now file has been provided. Redirected to home.");
					return home(model);
				}
				break;
			default: 
				model.addAttribute("warning", "Unkown settings file was selected. Redirected to home.");
				return home(model);
			}

			// Compare document files
			try {
				String file1Type = file1.getContentType();
				String file2Type = file2.getContentType();
				String content1 = "";
				String content2 = "";
				// Base document type
				DocumentType document1Type = file1Type.equals("text/xml") ? DocumentType.XML : DocumentType.PLAINTEXT;
				DocumentType document2Type = file2Type.equals("text/xml") ? DocumentType.XML : DocumentType.PLAINTEXT;
				
				// Check for TEI document type
				if (document1Type.equals(DocumentType.XML)) {
					content1 = XMLCleaner.clean(new String(file1.getBytes(), "UTF-8"));
					document1Type = TEIToAthenConverter.isTEI(new ByteArrayInputStream(content1.getBytes()))
							? DocumentType.TEI
							: DocumentType.XML;
				} else {
					content1 = new String(file1.getBytes(), "UTF-8");
				}
				if (document2Type.equals(DocumentType.XML)) {
					content2 = XMLCleaner.clean(new String(file2.getBytes(), "UTF-8"));
					document2Type = TEIToAthenConverter.isTEI(new ByteArrayInputStream(content2.getBytes()))
							? DocumentType.TEI
							: DocumentType.XML;
				} else {
					content2 = new String(file2.getBytes(), "UTF-8");
				}
				
				// Normalization 
				// Move to own class if normalization is extended further than replacement
				if(normalize != null) {
					if(normalizeFile == null) {
						model.addAttribute("warning",
								"A normalize file must be selected when choosing to normalize. Redirected to home.");
						return home(model);
					} else {
						String normalizer = new String(normalizeFile.getBytes(), "UTF-8");
						int line = 1;
						for (String ruleString: normalizer.split(SpecialCharacter.LINE_BREAKS_REGEX)) {
							// Ignore empty lines
							if(ruleString.trim().length() > 0) {
								// Check for rules `<delete>` and `<from> <to>`
								String[] rule = ruleString.trim().split(SpecialCharacter.SPACES_REGEX);
								if (rule.length > 2) {
									model.addAttribute("warning",
											"Rule in normalization file line "+line+" is invalid. Redirected to home.");
									return home(model);
								} else if (rule.length == 1) {
									content1 = content1.replace(rule[0], "");
									content2 = content2.replace(rule[0], "");
								} else if (rule.length == 2){
									content1 = content1.replace(rule[0], rule[1]);
									content2 = content2.replace(rule[0], rule[1]);
								}
							}
							line++;
						}
					}
				}

				// Compare and create response
				return compare(model,
						file1.getOriginalFilename(),
						file2.getOriginalFilename(),
						content1, content2,
						document1Type, document2Type,
						settings);
				
			} catch (IOException e1) {
				return "redirect:/404";
			}
		} else {
			return "redirect:/404";
		}
	}

	@RequestMapping(value = "/view", method = RequestMethod.GET)
	public String view2(Model model) {
		return "redirect:/";
	}

	@RequestMapping(value = "demo/view", method = RequestMethod.GET)
	public String demoview(Model model, @RequestParam("demo") int demo) {
		// Initialize
		File file1 = null;
		File file2 = null;
		Settings settings;
		DocumentType document1Type = DocumentType.PLAINTEXT;
		DocumentType document2Type = DocumentType.PLAINTEXT;
		
		// Select demo
		try {
			file1 = getDemoFile1(demo, servletContext);
			file2 = getDemoFile2(demo, servletContext);
			settings = getDemoSettings(demo, servletContext);
		} catch (NoSuchElementException e) {
			return "redirect:/404";
		}

		// Load demo
		try {
			// Compare and create response
			return compare(model,
					file1.getName(),
					file2.getName(),
					new String(Files.readAllBytes(file1.toPath()), StandardCharsets.UTF_8),
					new String(Files.readAllBytes(file2.toPath()), StandardCharsets.UTF_8),
					document1Type, document2Type,
					settings);
		} catch (IOException e) {
			e.printStackTrace();
			return "redirect:/404";
		}
	}
	
	@RequestMapping(value = "demo/download/demo{demo}.zip", method = RequestMethod.GET)
	public ResponseEntity<byte[]> downloaddemo(Model model, @PathVariable("demo") int demo) {
		// Initialize
		HttpHeaders headers = new HttpHeaders();
		headers.setCacheControl("no-cache");
		
		// Select demo
		try {	
		    ByteArrayOutputStream baos = new ByteArrayOutputStream();
		    ZipOutputStream zip = new ZipOutputStream(baos);
		    addToZip(zip, getDemoFile1(demo, servletContext));
		    addToZip(zip, getDemoFile2(demo, servletContext));
		    addToZip(zip, getDemoSettingsFile(demo, servletContext));
		    zip.close();
    		
			headers.setContentType(MediaType.TEXT_PLAIN);
			headers.setContentLength(baos.toByteArray().length);
			return new ResponseEntity<byte[]>(baos.toByteArray(), headers, HttpStatus.OK);
		} catch (NoSuchElementException e) {
			e.printStackTrace();
			return new ResponseEntity<byte[]>(new byte[0], headers, HttpStatus.BAD_REQUEST);
		} catch (IOException e) {
			e.printStackTrace();
			return new ResponseEntity<byte[]>(new byte[0], headers, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	private void addToZip(ZipOutputStream zip, File file) throws IOException {
		    byte[] filebyte = Files.readAllBytes(file.toPath());
		    ZipEntry entry = new ZipEntry(file.getName());
		    entry.setSize(filebyte.length);
		    
		    zip.putNextEntry(entry);
		    zip.write(filebyte);
		    zip.closeEntry();
	}
	
	@RequestMapping(value = "/default.txt", method = RequestMethod.GET)
	public ResponseEntity<byte[]> defaultSettings(Model model) {
		final String defaultSettings = StorageManager.getDefault(servletContext);
		final byte[] settingsBytes = defaultSettings.getBytes();

		HttpHeaders headers = new HttpHeaders();
		headers.setCacheControl("no-cache");
		headers.setContentType(MediaType.TEXT_PLAIN);
		headers.setContentLength(settingsBytes.length);

		return new ResponseEntity<byte[]>(settingsBytes, headers, HttpStatus.OK);
	}
	
	@RequestMapping(value = "/404")
	public String error404(Model model) {
		model.addAttribute("warning", "Unable to find requested page. Redirected to home.");
		return home(model);
	}

	@RequestMapping(value = "/400")
	public String error400(Model model) {
		model.addAttribute("warning", "Unable to understand your request. Redirected to home.");
		return home(model);
	}

	@RequestMapping(value = "/500")
	public String error500(Model model) {
		model.addAttribute("warning", "Sorry the server had an internal error. Redirected to home.");
		return home(model);
	}

	@Bean(name = "multipartResolver")
	public CommonsMultipartResolver multipartResolver() {
		CommonsMultipartResolver multipartResolver = new CommonsMultipartResolver();
		multipartResolver.setMaxUploadSize(10485760);
		return multipartResolver;
	}

	
	public String compare(Model model,
			String filename1, String filename2,
			String file1, String file2,
			DocumentType document1Type, DocumentType document2Type,
			Settings settings) {

		List<Variance> variancetypes = new ArrayList<>(settings.getVariances());
		List<String> outputVarianceTypes = variancetypes.stream().map(v -> v.getName()).collect(Collectors.toList());

		// Compare document files
		if (document1Type.equals(DocumentType.TEI) && document2Type.equals(DocumentType.TEI)) {
			// Convert to Athen
			TextAnnotationStruct document1 = TEIToAthenConverter
					.convertTEIToAthen(new ByteArrayInputStream(file1.getBytes()));
			TextAnnotationStruct document2 = TEIToAthenConverter
					.convertTEIToAthen(new ByteArrayInputStream(file2.getBytes()));

			Collection<Annotation> annotations1 = document1.getAnnotations().stream()
					.map(a -> new Annotation(a)).collect(Collectors.toList());
			Collection<Annotation> annotations2 = document2.getAnnotations().stream()
					.map(a -> new Annotation(a)).collect(Collectors.toList());

			// Find differences
			List<ConnectedContent> differences = Diff.compareXML(document1.getText(), document2.getText(),
					annotations1, annotations2, settings);

			model.addAttribute("exportJSON",
					DiffExporter.convertToAthenJSONString(document1, differences, outputVarianceTypes));
			model.addAttribute("allLines", LineCreator.patch(differences));
			
			model.addAttribute("statistics", new VarianceStatistics(differences, settings.getVariances()));
		} else {
			// Interpret as plain text
			String content1 = file1;
			content1 = content1.replaceAll("\\r\\n", "\n").replaceAll("\\r", "\n");
			String content2 = file2;
			content2 = content2.replaceAll("\\r\\n", "\n").replaceAll("\\r", "\n");
			List<ConnectedContent> differences = Diff.comparePlainText(content1, content2, settings);

			outputVarianceTypes.remove(Variance.TYPOGRAPHY.getName());
			
			model.addAttribute("exportJSON",
					DiffExporter.convertToAthenJSONString(content1, differences, outputVarianceTypes));
			model.addAttribute("allLines", LineCreator.patch(differences));

			model.addAttribute("statistics", new VarianceStatistics(differences, settings.getVariances()));

			variancetypes.remove(Variance.TYPOGRAPHY);

		}
		model.addAttribute("variancetypes", VarianceClassifier.sortVariances(variancetypes));
		model.addAttribute("document1name", filename1);
		model.addAttribute("document2name", filename2);
		model.addAttribute("document1type", document1Type);
		model.addAttribute("document2type", document2Type);
		model.addAttribute("externalCSS", settings.getExternalCss());
		return "view";
	}
	
	/********
	 * DEMO *
	 ********/
	private static File getDemoFile1(int demo, ServletContext servletContext) {
		final String fs = File.separator;
		final File[] files1 = new File[] {
			StorageManager.getFile("demo"+ fs +"GW5041"+ fs +"GW5041-Lesetext.txt", servletContext)
		};
		try {
			return files1[demo];
		} catch (IndexOutOfBoundsException e) {
			throw new NoSuchElementException("No file 1 for demo "+demo+" found.");
		}
	}

	private static File getDemoFile2(int demo, ServletContext servletContext) {
		final String fs = File.separator;
		final File[] files2 = new File[] {
			StorageManager.getFile("demo"+ fs +"GW5041"+ fs +"GW5041-OCR.txt", servletContext)
		};
		try {
			return files2[demo];
		} catch (IndexOutOfBoundsException e) {
			throw new NoSuchElementException("No file 2 for demo "+demo+" found.");
		}
	}

	private static Settings getDemoSettings(int demo, ServletContext servletContext) {
		if(demo == 0) {
			return new Settings(StorageManager.getDefault(servletContext));
		} else {
			throw new NoSuchElementException("No settings for demo "+demo+" found.");
		}
	}
	
	private static File getDemoSettingsFile(int demo, ServletContext servletContext) {
		final File[] settings = new File[] {
			StorageManager.getFile("defaults.txt", servletContext)
		};
		try {
			return settings[demo];
		} catch (IndexOutOfBoundsException e) {
			throw new NoSuchElementException("No settings file for demo "+demo+" found.");
		}
	}
}
