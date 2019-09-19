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
			<input type="submit" value="Compare">
		</form>

	</article>
	<article class="demos">
		<h1>Demos</h1>
		<hr>
		<a href="demo/view?demo=0">Demo 1</a>
	</article>

	<section id="warning"/>
	<script type="text/javascript" src="resources/js/home.js"></script>
	<c:if test="${warning != null}">
		<script>
			displayWarning('<c:out value="${warning}"></c:out>',6000);
		</script>
	</c:if>
</t:webpage>

