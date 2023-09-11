<%@tag description="Main Webpage Tag" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<jsp:useBean id="date" class="java.util.Date" />
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta content="width=device-width" name="viewport" />
<meta name="viewport"
	content="width=device-width,initial-scale=1.0,minimum-scale=1.0,maximum-scale=1.0" />
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">

<link href="https://fonts.googleapis.com/css?family=Roboto:100,400"
	rel="stylesheet">
<link rel="stylesheet" href='<c:url value="/resources/css/main.css"/>'>

<style media="all">
<c:if test="${not empty externalCSS}">
<c:out value="${externalCSS}" />
</c:if>
</style>

<title>Variance Viewer</title>
</head>
<body>
	<header id="page-header"> 
	<a id="header-university-emblem" href="http://www.is.informatik.uni-wuerzburg.de/">
		<img id="header-university-emblem" src='<c:url value="/resources/img/uni-logo_white.png"/>' />
	</a>
	<a href='<c:url value="/"/>' id="page-header-title">Variance Viewer</a> </header>
	<main> <jsp:doBody /> </main>
	<footer>
	<hr>
	<p>© 2018 - <fmt:formatDate value="${date}" pattern="yyyy" /> Universität Würzburg <a href="https://www.uni-wuerzburg.de/sonstiges/impressum">Impressum</a></p>
	</footer>
</body>
</html>