<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags"%>

<t:webpage>
   <article class="file-upload">
		<h1>Compare text files</h1>
		<hr>

		Select (or drag and drop) two plain text or TEI Files to compare to each other</br>
		<form name="compare-form" action="view" method="POST" onsubmit="return validateForm()" enctype="multipart/form-data">
			<input id="file1" name="file1" type="file"><br>
			<input name="file2" type="file"><br>
			<hr>
			Select settings to use for the comparison or select your own file:</br>
			<input type="radio" id="default_settings" name="settings" value="default" checked>
			<label for="default_settings">
				default (<a href="default.txt" download><u>⇓</u></a>)
			</label>
			<br>

			<div id="user_settings_wrapper">
				<input type="radio" id="user_settings" name="settings" value="user">
				<label for="user_settings">
					<input name="settingsFile" type="file" accept=".txt, .conf">
				</label>
			</div>
			<br>
			<hr>
			<%-- Even after some discussion. I'm not sure about the usefulness of the normalization.
				It should be discussed whether other options are clearer and more useful for the end user. --%>
			<div id="user_normalize_wrapper">
				<input type="checkbox" id="use_normalize" name="normalize">
				<label for="use_normalize">
					Normalize inputs
				</label>
 				<a href="#" class="help_normalize openNormalizeHelp">🛈</a>
				<br>
				<input id="normalize_setting" name="normalizeFile" type="file" accept=".txt, .conf">
			</div>
			<br>
			<input type="submit" value="Compare">
		</form>
	</article>

	<article class="demos">
		<h1>Demos</h1>
		<hr>
		<ul id="demo-list">
		<li>
			<a class="demo-view" href="demo/view?demo=0">Demo 1</a>
			<a class="demo-download" href="demo/download/demo0.zip"><u>⇓</u></a>
		</li>
		</ul>
	</article>

	<article id="blur"></article>	
	<article id="help-normalize" class="help">
		<section id="help-header">
			Normalize documents
			<hr>
		</section>
		<section id="help-body">
			<p>This setting allows to normalize both input files, by replacing and removing words or part of words.</p>
			<p>Every line in the normalization file represents one normalization rule. A normalization rule is a sequence of characters without whitespace or two sequences of characters separated by whitespace.</p> 
			<p>A rule with two character sequences e.g. <code class="inline">ſ s</code> will replace every instance of long-s ſ with a normal s before comparing the texts.</p>
			<p>Rules without whitespace characters will represent sequences that are to be removed from both files before comparing the texts.</p>
			<p>An example could be as follows:</p>
			<code>
				ſ ss<br>
				PAGEBREAK
			</code>
			<p>These rules would remove every occurence of PAGEBREAK from both files and replace every "ſ" with "ss" before comparing the texts.</p>
		</section>
		<section id="help-confirm">
			<button id="help-close" type="button">Close</button>
		</section>
	</article>
	<section id="warning"/>
	<script type="text/javascript" src="resources/js/home.js"></script>
	<c:if test="${warning != null}">
		<script>
			displayWarning('<c:out value="${warning}"></c:out>',6000);
		</script>
	</c:if>
</t:webpage>

