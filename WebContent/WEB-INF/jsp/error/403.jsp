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
<%@ page language="java" import="org.springframework.security.core.userdetails.User,org.springframework.security.core.context.SecurityContextHolder,org.springframework.security.core.Authentication,org.w3c.dom.*,java.util.Vector,javax.xml.parsers.*,org.w3c.dom.*,java.io.*,java.sql.*" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
	<title>Access forbidden</title>
	<link rel="shortcut icon" href="../images/favicon.ico">
</head>

<body link="black" alink="purple">
	<br><br>
	<center>
	<h2>
	<c:choose>
		<c:when test="${SPRING_SECURITY_403_EXCEPTION ne null}">
			${SPRING_SECURITY_403_EXCEPTION.message}
		</c:when>
		<c:otherwise>
<%
			Authentication authToken = SecurityContextHolder.getContext().getAuthentication();
			String sSubjectAndVerb = "You are";
			try
			{
				sSubjectAndVerb = "User '" + ((User) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername() + "' is";
			}
			catch (Exception ignored)
			{}
%>

			<%= sSubjectAndVerb %> not allowed to access this resource!
		</c:otherwise>
	</c:choose>
	</h2>
	</center>
</body>
</html>
