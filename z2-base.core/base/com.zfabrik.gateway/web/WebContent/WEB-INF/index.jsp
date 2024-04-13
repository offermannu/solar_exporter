<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@page import="java.io.PrintWriter"%>
<html>
<head>
<title>Gateway Detach Utility</title>
</head>
<body>
<h1>Gateway Detach Utility</h1>

<%-- handle error display --%>
<c:if test="${error!=null}">
<p style="color:red"><pre>
<% ((Exception) request.getAttribute("error")).printStackTrace(new PrintWriter(out)); %>
</pre></p>
</c:if>

<c:if test="${worker!=null}">
	<p>Current dispatch target worker: <c:out value="${worker} (${stateString})"/></p>

	<c:choose>
		<c:when test="${state<3}">
			<%-- allow dispatch	--%>
			<form action="?" method="post">
				<input type="submit" value="Detach <c:out value="${worker}"/> and sync">
				<input type="hidden" name="worker" value="<c:out value="${worker}"/>">
			</form>
		</c:when>
		<c:otherwise>
			<p><a href="?">Refresh</a> to see current state</p>
		</c:otherwise>
	</c:choose>
</c:if>
<p style="font-size: x-small; color:gray; margin-top:1em; padding-top:1em; border-top: solid 1px gray">
Served from <c:out value="${currentWorker}"/> in session <c:out value="${sessionId}"/>  | <a href="?logoff">log off</a>
</p>
</body>
</html>