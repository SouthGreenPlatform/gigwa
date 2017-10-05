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
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" import="org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jstl/fmt" %>  
<fmt:setBundle basename="config" />

<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
	<link rel="shortcut icon" href="favicon.ico" />
	<link media="screen" type="text/css" href="css/main.css" rel="StyleSheet">
	<title>Authentication</title>
	<script type="text/javascript" src="js/jquery-1.8.2.min.js"></script>
	<script language='javascript'>
	var currentWindow = this;
	while (currentWindow != top)
	{
		try
		{
 			currentWindow.parent.document;	// accessing this throws an exception if Gigwa is running in a frame
			currentWindow = currentWindow.parent;
		}
		catch(e)
		{
			break;
		}
	}
	if (currentWindow != this)
		currentWindow.location.href = location.href;
	</script>
</head>

<body onload="document.forms[0].j_username.focus();" style="margin:0; overflow:hidden;">

<%
	Exception lastException = (Exception) session.getAttribute(AbstractAuthenticationProcessingFilter.SPRING_SECURITY_LAST_EXCEPTION_KEY);
%>

<center>
 <p><img src="img/logo_big.png" height="69" style="margin-top:30px;"><br>Genotype Investigator for Genome Wide Analyses</p>
 <br>
 <table style="border:1px dashed #303030; background-color:#f0f0f0;">
	<tr>
		<th colspan='2' style="background-color:#21A32C; color:lightYellow; height:30px; font-size:14px;">User authentication</th>
	</tr>
	
	<tr align="center">
		<td width="5%"></td>
		<td>
		<form name='f' action='j_spring_security_check' method='POST'>
			<table cellspacing="0">
			<tbody>
			<tr>
				<td>
					<br>
					<table style="width:200px;" cellspacing="0" border="0">
					<tbody>
					<tr>
						<td class="default_left"></td>
						<td class="default_center">
						<table border='0'>
							<tbody>
							<tr>
								<td width="50">Login</td>
								<td>
									<input type='text' name='j_username' value=''>
								</td>
							</tr>
							<tr>
								<td>Password</td>
								<td>
									<input type='password' name='j_password'>
								</td>
							</tr>
							<tr>
								<td style="height: 26px;"></td>
								<td colspan="2" style="height: 26px; text-align:left;">
								<input type="submit" name="connexion" value="Submit" style="margin-bottom:5px;">
								<br><%= lastException != null && lastException instanceof org.springframework.security.authentication.BadCredentialsException ? "&nbsp;&nbsp;&nbsp;<span style='color:#F2961B;'>Authentication failed!</span>" : "" %>&nbsp;
								</td>
							</tr>
							</tbody>
						</table>
						</td>
						<td class="default_right"></td>
					</tr>
					<tr>
						<td class="default_bottom_left"></td>
						<td class="default_bottom"></td>
						<td class="default_bottom_right"></td>
					</tr>
					</tbody>
				</table>
				</td>
				<td style="width: 10px;">
				</td>
			</tr>
			</tbody>
			</table>
		</form>
		</td>
	</tr>	
 </table>
<input type="button" value="Click here to access public data" style="margin-top:15px;" onclick="window.location.href='gigwa/';">
<fmt:message var="adminEmail" key="adminEmail" />
<c:if test='${!fn:startsWith(adminEmail, "??") && !empty adminEmail}'>
	<p>Please contact <a href="mailto:${adminEmail}?subject=Gigwa account request">${adminEmail}</a> to apply for an account.</p>
</c:if>
</center>

<div style="width:100%; margin-top:110px; padding:15px; text-align:center; border-top:4px double #e0e0e0; border-bottom:4px double #e0e0e0;">
<a href="http://www.southgreen.fr/" target="_blank"><img height="34" src="img/logo-southgreen.png" /></a>
<a href="http://www.cirad.fr/" target="_blank" style="margin-left:60px;"><img height="32" src="img/logo-cirad.png" /></a>
<a href="http://www.ird.fr/" target="_blank" style="margin-left:60px;"><img height="32" src="img/logo-ird.png" /></a>
<a href="http://www.inra.fr/" target="_blank" style="margin-left:60px;"><img height="34" src="img/logo-inra.png" /></a>
<a href="http://www.arcad-project.org/" target="_blank" style="margin-left:60px;"><img height="32" src="img/logo-arcad.png" /></a>
</div>

<%
	session.setAttribute(AbstractAuthenticationProcessingFilter.SPRING_SECURITY_LAST_EXCEPTION_KEY, null);
%>
						  
</body>
</html>