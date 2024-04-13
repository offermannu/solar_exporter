<%@include file="include/decl.jsp"%>
<%@include file="include/header.jsp"%>

<div id="header">
	<form action="?" method="post">
		<table>
		<tr><td><label for="process">Process:</label></td>
		<td><select id="process" name="process">
			<c:forEach items="${processes}" var="p">
				<option<c:if test="${p.key==process}"> selected="selected"</c:if> value="<c:out value="${p.key}"/>"><c:out value="${p.value}"/></option>
			</c:forEach>
		</select></td>
		<td><label for="group">Group:</label></td>
		<td><select id="group" name="group">
			<c:forEach items="${groups}" var="g">
				<option<c:if test="${g==group}"> selected="selected"</c:if>><c:out value="${g}"/></option>
			</c:forEach>
		</select></td>
		<td>
			<input type="checkbox" value="true" id="tabular" name="tabular"<c:if test="${tabular}"> checked="checked"</c:if>/>
			<label for="tabular">table</label>
		</td>
		<td>			
			<input type="submit" class="submit" value="update" name="update"/>
		</td>
		</tr>
		<tr>
		<td>Actions:</td>
		<td colspan="5"> 
			<input type="submit" class="submit"  value="Trigger local GC" name="callGC"/>
			<input type="submit" class="submit"  value="Sync (&lt;home&gt;)" name="synchronize"/>
			<input type="submit" class="submit"  value="Verify (&lt;home&gt;)" name="verify"/>
			<a href="<c:url value="log"/>"><input type="button" value="Log..."/></a>
		</td>
		</tr>
		<tr>
		<td style="cursor:pointer; vertical-align:top;" onclick="var d = document.getElementById('more'); d.style.display=(d.style.display=='none'? d.style.display='' : d.style.display='none');">more...</td>
		<td style="display:none" id="more" colspan="5">
		<table>
		<tr>
		<td></td>
		<td colspan="5"> 
			<input type="text" value="<c:out value="${fileName}"/>" name="fileName"/>
			<input type="submit" class="submit"  value="Write Heap Dump" name="writeHeapDump"/>
		</td>
		</tr>
		<tr>
		<td></td>
		<td colspan="5"> 
			<input type="text" value="<c:out value="${componentName}"/>" name="componentName"/>
			<input type="submit" class="submit"  value="Invalidate Component (& Verify)" name="invalidateComponentAndVerify"/>
		</td>
		</tr>
		<tr>
		<td></td>
		<td colspan="5"> 
			<input type="text" value="<c:out value="${resourceName}"/>" name="resourceName"/>
			<input type="submit" class="submit"  value="Invalidate Resource" name="invalidateResource"/>
		</td>
		</tr>
		</table>
		</td>
		</tr>
		</table>
	</form>
</div>

<c:if test="${not empty messages}">
<div id="messages">
<ul>
<c:forEach items="${messages}" var="m">
<li><c:out value="${m}"/></li>
</c:forEach>
</ul>
</div>
</c:if>

<c:choose>
<c:when test="${tabular}">
<%-- render tabular --%>

<table  class="datatable">
<tr>
<c:forEach items="${data.table.columns}" var="c">
<th><c:out value="${c}"/></th>
</c:forEach>
<th>Actions</th>
</tr>
<% int i=0; %>
<c:forEach items="${data.table.rows}" var="r">
<tr<%=(((++i)&1)==0? " style=\"background-color:#F0F0F0\"":"")%>>
<c:forEach items="${data.table.columns}" var="c">
<td><c:out value="${r.data[c]}"/></td>
</c:forEach>

<c:if test="${not empty r.actions}">
<td>
<c:forEach items="${r.actions}" var="a">
<a href="<c:url value="?">
	<c:param name="${a.name}"/>
	<c:param name="group" value="${group}"/>
	<c:param name="process" value="${process}"/>
	<c:param name="tabular" value="${tabular}"/>
	<c:forEach items="${a.params}" var="p"><c:param name="${p.key}" value="${p.value}"/></c:forEach></c:url>"><c:out value="${a.text}"/></a>&nbsp;
</c:forEach>
</td>
</c:if> 
 
</tr>
</c:forEach>

</table>

</c:when>
<c:otherwise>

<%-- render bean boxes --%>
<c:forEach items="${data.beans}" var="b">
<div class="databox">
<div class="databox_title"><span><c:out value="${b.title}"/></span></div>
<table><tr><th>Name</th><th>Value</th></tr>
	<c:forEach items="${b.attributes}" var="a">
		<tr><td><c:out value="${a.name}"/></td><td><c:out value="${a.value}"/></td></tr>
	</c:forEach>
	<c:if test="${not empty b.actions}">
		<tr><td>
			<c:forEach items="${b.actions}" var="a">
				<a href="<c:url value="?">
					<c:param name="${a.name}"/>
					<c:param name="group" value="${group}"/>
					<c:param name="process" value="${process}"/>
					<c:param name="tabular" value="${tabular}"/>
					<c:forEach items="${a.params}" var="p"><c:param name="${p.key}" value="${p.value}"/></c:forEach></c:url>"><c:out value="${a.text}"/></a>&nbsp;
			</c:forEach>
		</td></tr>	
	</c:if>	
</table>
</div>
</c:forEach>

</c:otherwise>
</c:choose>

<%@include file="include/footer.jsp"%>