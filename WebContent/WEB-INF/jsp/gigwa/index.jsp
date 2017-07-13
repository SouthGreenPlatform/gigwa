<%--
		GIGWA - Genotype Investigator for Genome Wide Analyses
		Copyright (C) 2016 <CIRAD>
		    
		This program is free software: you can redistribute it and/or modify
		it under the terms of the GNU Affero General Public License, version 3 as
		published by the Free Software Foundation.
		
		This program is distributed in the hope that it will be useful,
		but WITHOUT ANY WARRANTY; without even the implied warranty of
		MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
		GNU Affero General Public License for more details.
		
		See <http://www.gnu.org/licenses/gpl-3.0.html> for details about
		GNU Affero General Public License V3.
--%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" import="org.springframework.security.core.*,org.springframework.security.core.context.SecurityContextHolder,fr.cirad.web.controller.gigwa.base.AbstractVariantController,fr.cirad.web.controller.gigwa.GigwaController" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<sec:authentication property="principal" var="principal"/>
<sec:authorize access="hasRole('ROLE_ADMIN')" var="isAdmin"/>
<sec:authorize access="hasRole('ROLE_ANONYMOUS')" var="isAnonymous"/>
<html>
<%
java.util.Properties prop = new java.util.Properties();
prop.load(getServletContext().getResourceAsStream("/META-INF/MANIFEST.MF"));
String appVersion = prop.getProperty("Implementation-version");
appVersion = appVersion == null ? "" : ("v" + prop.getProperty("Implementation-version").replace("-RELEASE", ""));
%>
<head>
	<title>Gigwa <%= appVersion %></title>
	<link rel="shortcut icon" href="../favicon.png" />
	<link rel ="stylesheet" type="text/css" href="../css/main.css" title="style">
	<script type="text/javascript" src="../js/jquery-1.8.2.min.js"></script>
	<script type="text/javascript" src="../js/jquery.simplemodal.1.4.4_removeExpression_fixed.js"></script>
	<script type="text/javascript" src="../js/jquery.cookie.js"></script>
	<script type="text/javascript" src="../js/listNav.js"></script>
	<script type="text/javascript">
		function showTutorial()
		{
		    $("#tutorialDialog").modal({
		    	opacity:80,
		    	overlayCss: {backgroundColor:"#111"}
		    });
		}
		
		function showRoleManager()
		{
		    $("#roleManagerDialog").modal({
		    	opacity:80,
		    	overlayCss: {backgroundColor:"#111"}
		    });
		}
		
		function applySelection()
		{
			var selectedView = $('input:radio[name=gigwaView]:checked').val();
			if (selectedView == null || $('#modules').val() == "")
			{
				window.frames['mainFrame'].document.body.innerHTML = "";
				window.frames['mainFrame'].document.body.style.backgroundColor='#f0f0f0';
			}
			else
			{
				var destinationURL = selectedView + '?module=' + $('#modules').val();
				window.frames['mainFrame'].location.href = destinationURL;
			}
		}
	
		function setBodyFrameToFullHeight() {
			$('#bodyFrame').css('height', (document.documentElement.clientHeight - 40)+'px');
		}
				
	    $(document).ready(function() {
	    	setBodyFrameToFullHeight();
	    });
	    $(window).resize(function() {
	    	setBodyFrameToFullHeight();
	    });	
	</script>
</head>	

<body onLoad="window.frames['mainFrame'].document.body.style.backgroundColor='#f0f0f0'; border:0;">

<c:set var="defaultViewURL" value="<%=AbstractVariantController.variantSearchPageURL%>" />

<div style="position:absolute; margin-top:-2px;">
<img src="../img/logo.png" height="25"><%= appVersion %>
</div>

<div style="float:right; margin-top:-6px;">
	<a style="margin-right:20px;" href="#" onclick="showTutorial();" title='Online tutorial'><img src='../img/tutorial.gif' border='0'></a>
    <c:if test="${userDao.doesLoggedUserOwnEntities()}">
		<a style="margin-right:20px;" href="#" onclick="showRoleManager();" title='User permissions'><img height='26' style="margin-bottom:4px;" src='../img/users.png' border='0'></a>
	</c:if>
    <c:if test="${userDao.canLoggedUserWriteToSystem()}">
		<a target="mainFrame" href='<c:url value="<%=GigwaController.importPageURL%>" />' onclick="$('form')[0].reset();" title='Import data'><img src='../img/import.gif' border='0'></a>
    </c:if>
    <c:if test="${principal == null || isAdmin}">
		<a style="margin-left:20px;" href='<c:url value="<%=GigwaController.mainPageURL%>" />?reloadDatasources=true' onclick="$('form')[0].reset();" title='Reload database list and permissions'><img src='../img/refresh.gif' border='0'></a>
    </c:if>
    <c:if test="${principal != null && isAnonymous}">
		<a href='../j_spring_security_logout' title='Log-in for private data'><img src='../img/login.gif' border='0'></a>
	</c:if>
	<c:if test="${principal != null && !isAnonymous}">
		<a style="margin-left:20px;" href='../j_spring_security_logout' title='Log-out ${principal.username}'><img src='../img/logout.gif' border='0'></a>
	</c:if>
</div>

<form style="margin-left:250px;">
	Database:
	<select id="modules" onchange="applySelection();" style="margin-right:40px;">
		<option value="">- Select one -</option>
		<c:forEach var="moduleName" items="${modules}">
			<option value="${moduleName}">${moduleName}</option>
		</c:forEach>
	</select>
	
	<div style="display:inline;<c:if test='${fn:length(views) < 2}'> visibility:hidden;</c:if>">
	Working on
	<c:forEach var="view" items="${views}">
		<input type="radio" name="gigwaView" value='<c:url value="${view.value}" />' onclick="applySelection();"<c:if test='${defaultViewURL eq view.value}'> checked</c:if>>${view.key} &nbsp;
	</c:forEach>
	</div>
</form>

<iframe style="margin-top:-7px; margin-left:0px; width:100%; border:1px black solid; background-color:#fff;" name="mainFrame" name="bodyFrame" id="bodyFrame"></iframe>

<div id="tutorialDialog" align="center" style="margin:10px; width:960px; display:none; position:absolute; top:0px; left:0px; z-index:100; background-color:#ffffff;">
	<table style="border-spacing:0 border:2px solid #E0E0E0; margin-top:2px;" cellpadding='0' cellspacing='0'>
		<tr>
			<td>
				<iframe src="../tutorial.html" style="width:960px; height:540px; border:0;"></iframe>
			</td>
		</tr>
		<tr>
			<td style="background-color:#ffffff; height:1px;" align='center' valign="middle"></td>
		</tr>
		<tr>
			<td style="background-color:#ddeeee; height:25px;" align='center' valign="middle">
				<div style="position:absolute; left:840px; top:550px; font-weight:bold; font-style:italic; width:150px;">GIGWA tutorial&nbsp;</div>
				<form>
					<input type='button' value='Close' onclick="abortExport();" class="simplemodal-close">
				</form>
			</td>
		</tr>
	</table>
</div>

<div id="roleManagerDialog" align="center" style="margin:10px; width:960px; display:none; position:absolute; top:0px; left:0px; z-index:100; background-color:#ffffff;">
	<table style="border-spacing:0 border:2px solid #E0E0E0; margin-top:2px;" cellpadding='0' cellspacing='0'>
		<tr>
			<td>
				<iframe src="../private/AdminFrameSet.html" style="width:960px; height:540px; border:0;"></iframe>
			</td>
		</tr>
		<tr>
			<td style="background-color:#ffffff; height:1px;" align='center' valign="middle"></td>
		</tr>
		<tr>
			<td style="background-color:#ddeeee; height:25px;" align='center' valign="middle">
				<div style="position:absolute; left:840px; top:550px; font-weight:bold; font-style:italic; width:150px;">User permissions&nbsp;</div>
				<form>
					<input type='button' value='Close' onclick="abortExport();" class="simplemodal-close">
				</form>
			</td>
		</tr>
	</table>
</div>

</body>

</html>
