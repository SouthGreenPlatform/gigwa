<%--
		GIGWA - Genotype Investigator for Genome Wide Analyses
		Copyright (C) 2016 <South Green>
		    
		This program is free software: you can redistribute it and/or modify
		it under the terms of the GNU General Public License, version 3 as
		published by the Free Software Foundation.
		
		This program is distributed in the hope that it will be useful,
		but WITHOUT ANY WARRANTY; without even the implied warranty of
		MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
		GNU General Public License for more details.
		
		See <http://www.gnu.org/licenses/gpl-3.0.html> for details about
		GNU General Public License V3.
--%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="UTF-8" import="fr.cirad.mgdb.model.mongo.subtypes.VariantRunData" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<html>

<head>
	<link rel ="stylesheet" type="text/css" href="../css/main.css" title="style">
	<script type="text/javascript" src="../js/jquery-1.8.2.min.js"></script>
	<script type="text/javascript">	
	function showHideUnselectedIndividuals(tableObj, show)
	{
		tableObj.find("tr:gt(1)").each(function() {
			if (show == false && !$(this).hasClass("selectedIndividual"))
				$(this).hide();
			else
				$(this).show();
		});
	}
	</script>
</head>

<jsp:useBean id="variant" scope="page" class="fr.cirad.mgdb.model.mongo.maintypes.VariantData" /><%-- dummy variant just to be able to invoke a static method --%>

<body style='background-color:#e0e0e0; margin:5px; padding:0px; overflow:hidden;'>
<div style="margin:auto; width:820px; height:390px; overflow-y:auto;">
<table class="variantDetails" style="width:800px;" border="0" cellpadding="0" cellspacing="0">
	<tr bgcolor="#eeeeee">
		<th align='right' nowrap>ID&nbsp;<br/>Variant type&nbsp;</th>
		<td nowrap>${param.variantId}<br/>${variantType}</td>
		<td width="40%" align="center" nowrap style="color:#21A32C;"><c:if test="${fn:length(runAdditionalInfoDesc) > 0}">You may drag your mouse pointer over<br />abbreviated field names for a description</c:if></td>
		<th align='right' width='40%'>
			<c:if test="${refPos.sequence ne null}">Sequence&nbsp;</c:if>
			<c:if test="${refPos.startSite ne null}"><br/>Position&nbsp;</c:if>
		</th>
		<td nowrap>
			${refPos.sequence}
			<c:if test="${refPos.startSite ne null}">
				<br/>${refPos.startSite}
				<c:if test="${refPos.endSite ne null}"> - ${refPos.endSite}</c:if>							
			</c:if>					
		</td>
	</tr>
	<c:set var="effectFieldName" value="<%=VariantRunData.FIELDNAME_ADDITIONAL_INFO_EFFECT_NAME%>" />
	<c:set var="geneFieldName" value="<%=VariantRunData.FIELDNAME_ADDITIONAL_INFO_EFFECT_GENE%>" />
	<c:forEach var="runData" items="${dataByRun}">
	<c:set var="gotMissingData" value="false" />
	<tr>
		<td colspan="2" valign="top">			
			<table cellpadding="0" cellspacing="0" style="margin-top:7px;">
			<c:forEach var="runAdditionalField" items="${runAdditionalInfo[runData.key]}">
			<c:if test="${runAdditionalField.key ne effectFieldName && runAdditionalField.key ne geneFieldName}">
				<tr>
					<th valign="top" align='right' title="${runAdditionalInfoDesc[runData.key][runAdditionalField.key].description}">${runAdditionalField.key}&nbsp;</th><td style="word-wrap:break-word; max-width:200px;">${runAdditionalField.value}</td>
				</tr>
			</c:if>
			</c:forEach>
			</table>
		</td>
		<td colspan="3" align="center" valign="top">		
			<input type="checkbox" style="margin-top:5px;" name="showHide" value="hide" onclick="showHideUnselectedIndividuals($('#genotypeTable_${runData.key}'), checked);"> Show unselected individuals 
			<table id="genotypeTable_${runData.key}" style='border:1px dashed #000; background-color:#70ffa0; margin-bottom:10px; margin-top:10px;' cellpadding="3" cellspacing="0">
				<tr>
					<th colspan="50" bgcolor="#eeeeee" title="${run}">Run "${runData.key}"</th>
				</tr>							
				<tr bgcolor="#ffffff"> 
					<th>Individual</th> 
					<th>Genotype</th> 
					<c:forEach var="additionalInfoField" items="${headerAdditionalInfo}" varStatus="i"><th title="${headerAdditionalInfoDesc[i.index]}"> ${additionalInfoField}</th></c:forEach>
				</tr>
				<c:forEach var="individualGenotypes" items="${runData.value}">
					<tr<c:choose><c:when test='${individualMap[individualGenotypes.key]}'> class="selectedIndividual"</c:when><c:otherwise> style="display:none;"</c:otherwise></c:choose>>
					<th align='left'>${individualGenotypes.key}</th><c:set var="colIndex" value="-1" />					
					<c:forEach var="aCellData" items="${individualGenotypes.value}"><c:set var="belowThreshold" value="${(headerAdditionalInfo[colIndex] eq 'DP' && aCellData < param.readDepthThreshold) || (headerAdditionalInfo[colIndex] eq 'GQ' && aCellData < param.genotypeQualityThreshold)}" />
						<td<c:choose><c:when test="${belowThreshold}"> bgcolor="#ff0000"<c:set var="gotMissingData" value="true" /></c:when></c:choose>>
						<c:choose><c:when test='${colIndex == -1}'><c:set var="genotype" value="${variant.staticGetAllelesFromGenotypeCode(knownAlleles, aCellData)}" /><c:forEach var="allele" items="${genotype}"><div class='allele'<c:if test="${fn:length(allele) > 5}"> title='size: ${fn:length(allele)}'</c:if>>${allele}</div></c:forEach></c:when>
						<c:otherwise>${aCellData}</c:otherwise>
						</c:choose></td>
					<c:set var="colIndex" value="${colIndex + 1}" /></c:forEach></tr>
				</c:forEach>
			</table>
			<div class="legende">
			<c:if test="${gotMissingData == true}">
				<div class="missingData"></div> 
				<span class="text">Treated as missing data</span>
			</c:if>
			</div>
		</td>
	</tr>
	<tr>
	<td colspan="4"><hr/></td>
	</tr>
	</c:forEach>
</table>
<br/>
</div>
</body>

</html>