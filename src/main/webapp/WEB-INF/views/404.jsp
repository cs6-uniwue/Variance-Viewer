<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags"%>

<t:webpage>
	<article>
		<h1>404</h1>
		<hr>
		<p>Hupps! Cound't find your request</p>
		<p><a href="${pageContext.request.contextPath}\">Back</a></p>
	</article>
</t:webpage>