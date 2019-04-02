<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags"%>

<t:webpage>
	<article>
		<h1>Error</h1>
		<hr>
		<p>Sorry, we where unable to load your files.</p>
		<p>The reason being:</p>
		<p><c:out value="${message}" /></p>
		<p><a href="${pageContext.request.contextPath}\">Back</a></p>
	</article>
</t:webpage>