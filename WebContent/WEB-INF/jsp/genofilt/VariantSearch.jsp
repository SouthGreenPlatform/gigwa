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
		
		Author(s): Guilhem SEMPERE, Florian PHILIPPE
--%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="UTF-8" import="fr.cirad.mgdb.model.mongo.subtypes.VariantRunData,fr.cirad.mgdb.model.mongo.subtypes.ReferencePosition,fr.cirad.mgdb.model.mongo.maintypes.VariantData,fr.cirad.web.controller.gigwa.base.AbstractVariantController"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<html>

<head>
<meta http-equiv="pragma" content="no-cache" />
<link rel="stylesheet" type="text/css" href="../css/chosen.min.css" title="style">
<link rel="stylesheet" type="text/css" href="../css/main.css" title="style">
<script type="text/javascript" src="../js/jquery-1.8.2.min.js"></script>
<script type="text/javascript" src="../js/jquery.simplemodal.1.4.4_removeExpression_fixed.js"></script>
<script type="text/javascript" src="../js/jquery.cookie.js"></script>
<script type="text/javascript" src="../js/main.js"></script>
<script type="text/javascript" src="../js/listNav.js"></script>
<script type="text/javascript" src="../js/chosen.jquery.min.js"></script>
<script type="text/javascript">
		$.ajaxSetup({ cache:false });

		var totalRecordCount = -1;
		var minimumProcessQueryIntervalUnit = 500;
		var currentProgressQueryID = null;
		var isFirstProgressCheck;
		var interfaceID = "${param.module}|${pageContext.session.id}|" + new Date().getTime();
		
		function isBusySearching(fBusy)
		{
			if (fBusy)
			{
				$('#abortButton').removeAttr('disabled');
				$('#searchButton').attr('disabled', 'disabled');
				$('input#browsingAndExportingEnabled').attr('disabled', 'disabled');
				$('input#exportButton').attr('disabled', 'disabled');
				$('td#chartLink').hide();
			}
			else
			{
				$("#LoadingOverlay").hide();
				$('#abortButton').attr('disabled', 'disabled');
				$('#searchButton').removeAttr('disabled');
				$('input#browsingAndExportingEnabled').removeAttr('disabled');
				$('input#exportButton').removeAttr('disabled');
				if (totalRecordCount > 0 && ($("#sequences option").length > 0 || $("#sequenceLoader:visible").length > 0))
					$('td#chartLink').show();
				$('div#progressDiv').html('');
			}
		}

		/* launches a search using current parameters */
		function loadCurrentPage()
		{
			if ($("input#browsingAndExportingEnabled").is(":checked") && totalRecordCount > 0)
			{	// counting already done: skip it
				loadPageRows(true);
				return;
			}
				
			isBusySearching(true);			
			if (getVariable("pageNumber") == 0)
			{
				$("table.resultTableNavButtons td#listCounter").html("");
				$("div#resultCountDiv").html("<img src='../img/progress.gif' />");
			}

			var countProcessID = "variantCount" + new Date().getTime() + "|" + interfaceID;
			currentProgressQueryID = countProcessID;
			$.ajax({
			    type:"POST",
			    url:'<c:url value="<%=AbstractVariantController.variantCountURL%>" />',
			    traditional:true,
			    data:{module:'${param.module}',project:$('#project').val(),variantTypes:getSelectedVariantTypes(),sequences:getSelectedSequences(),individuals:getSelectedIndividuals(),gtCode:$('#gtCode').val(),genotypeQualityThreshold:$('#genotypeQualityThreshold').val(),readDepthThreshold:$('#readDepthThreshold').val(),missingData:$('#missingdata').val(),minmaf:$('#minmaf').val(),maxmaf:$('#maxmaf').val(),minposition:$('#minposition').val(),maxposition:$('#maxposition').val(),alleleCount:getSelectedNumberOfAlleles(false),geneName:$('#geneName').val().trim().replace(new RegExp(' , ', 'g'), ','),variantEffects:$('#variantEffects').val(),processID:countProcessID},
			    success:function(jsonCountResult) {
			    	if (currentProgressQueryID != countProcessID)
			    		return;	// obsolete
			    	
					totalRecordCount = jsonCountResult == null ? 0 :parseInt(jsonCountResult);
			    		
			    	var countString = totalRecordCount + " result" + (totalRecordCount <= 1 ? "" :"s");
					$("div#resultCountDiv").text(countString);
					if ($("table.resultTableNavButtons td#listCounter").text() == '')
						$("table.resultTableNavButtons td#listCounter").text(countString);
					
					if (totalRecordCount == 0)
					{
						isBusySearching(false);
						$("#variantResultTable tr:gt(0)").remove();
				   		setTimeout("$('div#progressDiv').html('')", minimumProcessQueryIntervalUnit*3);
						onPageLoaded(0);
						return;
					}

					$("#variantResultTable th.sortableTableHeader").each(function() {
						this.onclick = function() { applySorting(this.abbr); };								
					});
										
					if ($("input#browsingAndExportingEnabled").is(":checked") && totalRecordCount > 0)
						findData();
					else
						isBusySearching(false);	// not listing results
			    },
			    error:function(xhr, ajaxOptions, thrownError) {
					handleJsonError(xhr, ajaxOptions, thrownError);
			    }
        	});
			setTimeout("isFirstProgressCheck = true; checkProcessProgress(\"" + countProcessID + "\");", minimumProcessQueryIntervalUnit);
		}
		
		function findData()
		{
			isBusySearching(true);

			$("#variantResultTable").css("minWidth", ($("#variantResultTable").width() - 13) + "px");
			$("#variantResultTable tr:gt(0)").remove();
			$("#variantResultTable").append("<tr><td colspan='7' class='progressBackground'>&nbsp;</td></tr>");

			$('#previousButton').hide();
			$('#nextButton').hide();

			var findProcessID = "variantFind" + new Date().getTime() + "|" + interfaceID;
			currentProgressQueryID = findProcessID;

			$.ajax({
			    type:"POST",
			    url:'<c:url value="<%=AbstractVariantController.variantFindURL%>" />',
			    traditional:true,
			    data:{ module:'${param.module}',project:$('#project').val(),variantTypes:getSelectedVariantTypes(), sequences:getSelectedSequences(),individuals:getSelectedIndividuals(),gtCode:$('#gtCode').val(),genotypeQualityThreshold:$('#genotypeQualityThreshold').val(),readDepthThreshold:$('#readDepthThreshold').val(),missingData:$('#missingdata').val(),minmaf:$('#minmaf').val(),maxmaf:$('#maxmaf').val(),minposition:$('#minposition').val(),maxposition:$('#maxposition').val(),alleleCount:getSelectedNumberOfAlleles(false),geneName:$('#geneName').val().trim().replace(new RegExp(' , ', 'g'), ','),variantEffects:$('#variantEffects').val(),wantedFields:getResultTableFields(),sortBy:encodeURIComponent(getVariable('sortBy')),sortDir:getVariable('sortDir'),page:getVariable('pageNumber'),size:getVariable('pageSize'),processID:findProcessID },
			    success:function(jsonResult) {
			    	if (currentProgressQueryID != findProcessID)
			    		return;	// obsolete
				},
				error:function(xhr, ajaxOptions, thrownError) {
					handleJsonError(xhr, ajaxOptions, thrownError);
	        	}
			});
			
 			setTimeout("isFirstProgressCheck = true; checkProcessProgress(\"" + findProcessID + "\", 'loadPageRows');", minimumProcessQueryIntervalUnit);
		}

		function loadPageRows(displayLoadingOverlay)
		{
			isBusySearching(true);
			
			if (displayLoadingOverlay)
			{
				$("#progressDiv").html("Loading...");
				$("#LoadingOverlay").height("100%");
				$("#LoadingOverlay").width($("#variantResultTable").width());
				$("#LoadingOverlay").addClass("progressBackground");
				$("#LoadingOverlay").show();
			}
			
			$.ajax({
			    type:"POST",
			    url:'<c:url value="<%=AbstractVariantController.variantListURL%>" />',
			    traditional:true,
			    data:{ module:'${param.module}',project:$('#project').val(),variantTypes:getSelectedVariantTypes(), sequences:getSelectedSequences(),individuals:getSelectedIndividuals(),gtCode:$('#gtCode').val(),genotypeQualityThreshold:$('#genotypeQualityThreshold').val(),readDepthThreshold:$('#readDepthThreshold').val(),missingData:$('#missingdata').val(),minmaf:$('#minmaf').val(),maxmaf:$('#maxmaf').val(),minposition:$('#minposition').val(),maxposition:$('#maxposition').val(),alleleCount:getSelectedNumberOfAlleles(false),geneName:$('#geneName').val().trim().replace(new RegExp(' , ', 'g'), ','),variantEffects:$('#variantEffects').val(),wantedFields:getResultTableFields(),sortBy:encodeURIComponent(getVariable('sortBy')),sortDir:getVariable('sortDir'),page:getVariable('pageNumber'),size:getVariable('pageSize'),processID:currentProgressQueryID },
			    success:function(jsonResult) {
				   	if (jsonResult == "")
				   	{
				   		totalRecordCount = 0;
				   		alert("<%= AbstractVariantController.MESSAGE_TEMP_RECORDS_NOT_FOUND %>");
				   		isBusySearching(false);
				   		return;
				   	}
				   	
					nAddedRows = 0;
					
					$("#variantResultTable tr:gt(0)").remove();
				   	var newContents = "";
					for (var key in jsonResult)
					{
						var rowContents = "";
				   		if (jsonResult[key] != null)
				   		{
						   	for (var subkey in jsonResult[key])
							{
						   		cellData = jsonResult[key][subkey] == null ? "" :jsonResult[key][subkey];
						   		if (subkey == 4)
					   			{
						   			var alleleArray = cellData.split("; ");
						   			cellData = "";
						   			for (var allIdx=0; allIdx<alleleArray.length; allIdx++)
						   				cellData += (allIdx == 0 ? "" :" ") + "<div class='allele'" + (alleleArray[allIdx].length < 6 ? "" :" title='size:" + alleleArray[allIdx].length + "'") + ">" + alleleArray[allIdx] + "</div>";
					   			}
						   		else if (subkey == 5 || subkey == 6)
						   		{
						   			cellData = cellData.replace(/ , /g, "<br>");
						   		}
						   		if (subkey < jsonResult[key].length - 1)
						   		{
						   			var isHiddenId = subkey == 0 && isValidMongoObjectId(cellData);
									rowContents += "<td nowrap" + (subkey == 5 || subkey == 6 ? " style='vertical-align:top;'" :"") + ">" + (isHiddenId ? "<div style='display:none;'>" :"") + ("" + cellData).replace(/\n/g, "<br>") + (isHiddenId ? "</div>" :"") + "</td>";
						   		}
							}
						   	for (var i=jsonResult[key].length; i<=$("#variantResultTable th:visible").size(); i++)
						   		rowContents += "<td></td>";

							rowContents += "<td style='border:none;' nowrap><a href='javascript:showDetails(\"" + encodeURIComponent(jsonResult[key][jsonResult[key].length - 1]) + "\", \"" + getSelectedIndividuals() + "\");' title='Variant details' onclick='selectedRowId=\"" + jsonResult[key][jsonResult[key].length - 1] + "\"; highlightSelectedVariant();'><img src='../img/magnifier.gif'></a></td>";
							newContents += "<tr class='hideable'>" + rowContents + "</tr>";
					   		nAddedRows++;
				   		}
				   		else
				   			break;
					}
			   	    if ($("#variantResultTable").length > 0)
			   	    {
			   	        if ($('#variantResultTable > tbody').length==0) $("#variantResultTable").append('<tbody />');
			   	        	($('#variantResultTable > tr').length>0)?$("#variantResultTable").children('tbody:last').children('tr:last').append(rowcontent):$("#variantResultTable").children('tbody:last').append(newContents);
			   	    }
			   	    onPageLoaded(nAddedRows);
			   		isBusySearching(false);
				},
				error:function(xhr, ajaxOptions, thrownError) {
					handleJsonError(xhr, ajaxOptions, thrownError);
	        	}
			});
		}

		function checkProcessProgress(processID, successFunctionName)
		{			
			if (processID == currentProgressQueryID)
				$.ajax({
					url:"<c:url value='<%= AbstractVariantController.progressIndicatorURL %>' />",
					data:{ module:'${param.module}', processID:processID },
					success:function(jsonResult){
						if (jsonResult != null && jsonResult['complete'] != true)
						{
							if (jsonResult['error'] != null)
							{
								alert("Error occured:\n\n" + jsonResult['error']);
								setTimeout("parent.closeExportDialog();", 0);	// without setTimeout it closes immediately and the alert message cannot even be seen
								isBusySearching(false);
							}
							else
							{						
	 							var isComplete = jsonResult['complete'] == true;

								if (isFirstProgressCheck != true || !isComplete)	// otherwise it was immediate
								{
									$('div#progressDiv').html(isComplete ? (totalRecordCount == 0 ? "No matching results" :"") : jsonResult['progressDescription']);
									if (!isComplete)
									{
										setTimeout("isFirstProgressCheck = false; checkProcessProgress(\"" + processID + "\", \"" + successFunctionName + "\");", minimumProcessQueryIntervalUnit);
										$('#abortButton').removeAttr('disabled');
										$('#searchButton').attr('disabled', 'disabled');
										
									}
								}
							}
						}
						else if (successFunctionName != null)
							eval(successFunctionName)();
					},
					error:function(xhr, ajaxOptions, thrownError) {
						handleJsonError(xhr, ajaxOptions, thrownError);
		        	}
				});
		}
		
		function abortCurrentOperation()
		{
			$('#abortButton').attr('disabled', 'disabled');
			
			$.ajax({
				url:"<c:url value='<%= AbstractVariantController.processAbortURL %>' />",
				data:{ processID:currentProgressQueryID },
				success:function(jsonResult){
					if (jsonResult == true)
					{
						currentProgressQueryID = null;
						document.getElementById('progressDiv').innerHTML = 'Aborting...';	
						$("#variantResultTable tr:gt(0)").remove();
						$('#resultCountDiv').html("");
					}
					else
						alert("Unable to abort!");
				},
				error:function(xhr, ajaxOptions, thrownError) {
					handleJsonError(xhr, ajaxOptions, thrownError);
	        	}
			});
			
			var codeToExecuteAfterReset = "";
			codeToExecuteAfterReset += "$('#progressDiv').html(''); $('#searchButton').removeAttr('disabled'); $('input#browsingAndExportingEnabled').removeAttr('disabled'); $('input#exportButton').attr('disabled', 'disabled'); if (totalRecordCount > 0) $('td#chartLink').show();";

			setTimeout(codeToExecuteAfterReset, minimumProcessQueryIntervalUnit * 3);
		}
				
		function onPageLoaded(nAddedRows)
		{									
			if (totalRecordCount > 0)
			{
				var lastRecordIndex = getVariable('pageSize') * getVariable('pageNumber') + nAddedRows;
				$("table.resultTableNavButtons td#listCounter").text(Math.min(lastRecordIndex, 1 + getVariable('pageSize') * getVariable('pageNumber')) + " - " + lastRecordIndex + " / " + totalRecordCount);
			}

			isBusySearching(false);

			highlightSelectedVariant();
			showHideNavigationLinks();
		}
		
		function showHideNavigationLinks()
		{
			var fEnabled = $("input#browsingAndExportingEnabled").is(":checked");
			var fAtFirstPage = getVariable('pageNumber') == 0;
			var fAtLastPage = true;
			try
			{
				var recordIndexes = $("table.resultTableNavButtons td#listCounter").text().split(" - ")[1].split(" / ");	// first will be last displayed on page, second will be last displayed current selection
				fAtLastPage = recordIndexes[0] == recordIndexes[1];
			}
			catch (error)
			{}
			
			if (fEnabled && !fAtFirstPage)
				$('#previousButton').show();
			else
				$('#previousButton').hide();
			
			if (!fEnabled || fAtLastPage)
				$('#nextButton').hide();
			else
				$('#nextButton').show();
		}
		
		function dirtyForm()
		{
			totalRecordCount = -1;
			$("input#exportButton").attr('disabled', 'disabled');
			$('td#chartLink').hide();
			$('#resultCountDiv').html("");
			$("table.resultTableNavButtons td#listCounter").html('');
			setVariable("pageNumber", 0);
			showHideNavigationLinks();
			$("#variantResultTable tr:gt(0)").remove();
		}
		
		var selectedRowId = null;
		function highlightSelectedVariant() {
			var bodyColor = $("body").css("background-color");
			$("#variantResultTable tr:gt(0)").each(function() {
				$(this).find("td:lt(" + ($(this).find('td').length - 1) + ")").css("background-color", $(this).find("td:eq(0)").text() == selectedRowId ? "#70ff50" :bodyColor);
			});
		}
		
		function showDetails(variantId, selectedIndividuals)
		{
	        $("#variantInfoDialog").modal({
	        	opacity:80,
	        	overlayCss:{backgroundColor:"#111"}
	        });
	        $("#variantInfoFrame").attr('src', "<c:url value="<%=AbstractVariantController.variantDetailsURL%>" />?module=${param.module}&project=" + $('#project').val() + "&variantId=" + variantId + "&individuals=" + selectedIndividuals + "&genotypeQualityThreshold=" + $('#genotypeQualityThreshold').val() + "&readDepthThreshold=" + $('#readDepthThreshold').val());
		}
		
		function loadSequenceList(evenIfBig)
		{
			$("#sequences").empty();

			$.ajax({
				url:"<c:url value='<%= AbstractVariantController.sequenceListURL %>' />",
				data:{ module:'${param.module}',project:$('#project').val() },
				success:function(jsonResult){
					if (jsonResult.length > 100 && !evenIfBig)
					{
						$("#sequenceLoader").html("<p style='color:#ee1111;'>Sequence list contains " + jsonResult.length + " items.</p><p><a style='font-weight:bold;' href='#' onclick='$(\"#sequenceLoader\").html(\"<p class=\\\"progressBackground\\\"><br/><br/><b>Loading...</b></p>\"); setTimeout(\"loadSequenceList(true);\", 1);'>Click here to load them</a></p>");
						$("#sequenceLoader").show();
					}
					else
					{
						var options = "";
						for (var key in jsonResult)
						{
							options += "<option value='" + jsonResult[key] + "' title='" + jsonResult[key] + "'>" + jsonResult[key] + "</option>";
							if (key%20 == 0 || key == jsonResult.length - 1)
							{
								setTimeout('$("#sequences").append("' + options + '");' + (key == jsonResult.length - 1 ? '$("#sequenceLoader").hide(); applySequenceSelection();' :''), key/40);
								options = "";
							}								
						}
					}
				},
				error:function(xhr, ajaxOptions, thrownError) {
					handleJsonError(xhr, ajaxOptions, thrownError);
	        	}
			});
		}
		
		function applyProjectSelectionToProjectDependantWidgets()
		{
			if ($('#project').val() != null) {
				$.getJSON('<c:url value="<%=AbstractVariantController.individualListURL%>" />', { module:'${param.module}',project:$('#project').val() }, function(jsonResult){				
					$("#individuals").empty();
					for (var key in jsonResult) {
						$("#individuals").append("<option value='" + jsonResult[key] + "' title='" + jsonResult[key] + "'>" + jsonResult[key] + "</option>");
					}
					applyIndividualSelection();
				}).error(function(xhr, ajaxOptions, thrownError) {
					handleJsonError(xhr, ajaxOptions, thrownError);
	        	});

				$.getJSON('<c:url value="<%=AbstractVariantController.variantTypesListURL%>" />', { module:'${param.module}',project:$('#project').val() }, function(jsonResult){				
					$("#variantTypes").empty();
					for (var key in jsonResult)
						$("#variantTypes").append("<option value='" + jsonResult[key] + "' title='" + jsonResult[key] + "'>" + jsonResult[key] + "</option>");							
				}).error(function(xhr, ajaxOptions, thrownError) {
					handleJsonError(xhr, ajaxOptions, thrownError);
	        	});

				$.getJSON('<c:url value="<%=AbstractVariantController.ploidyURL%>" />', { module:'${param.module}',project:$('#project').val() }, function(jsonResult){
					ploidy = jsonResult;
					$.getJSON('<c:url value="<%=AbstractVariantController.numberOfAlleleListURL%>" />', { module:'${param.module}',project:$('#project').val() }, function(jsonResult){				
						$("#alleleCount").empty();
						for (var key in jsonResult)
							$("#alleleCount").append("<option value='" + jsonResult[key] + "'>" + jsonResult[key] + "</option>");	
						var altAllele = document.getElementById('alleleCount');
						if (altAllele.length > 1)
							$('div#filterAlternateAllele').show();
						else{
							$('div#filterAlternateAllele').hide();
							if (altAllele.length == 1 && altAllele.options[0].value != '2')
								$('div#filterOnMaf').hide();
							else
								$('div#filterOnMaf').show();						
						}
						$('#alleleCount').change();
					}).error(function(xhr, ajaxOptions, thrownError) {
						handleJsonError(xhr, ajaxOptions, thrownError);
		        	});			
				}).error(function(xhr, ajaxOptions, thrownError) {
					handleJsonError(xhr, ajaxOptions, thrownError);
	        	});
				
				loadSequenceList();

				$.getJSON('<c:url value="<%=AbstractVariantController.gotGQFieldURL%>" />', { module:'${param.module}',project:$('#project').val() }, function(jsonResult) {
					
					$("#filterOnGQandDP div").toggle(jsonResult);
				}).error(function(xhr, ajaxOptions, thrownError) {
					handleJsonError(xhr, ajaxOptions, thrownError);
	        	});
				
				$.getJSON('<c:url value="<%=AbstractVariantController.projectEffectAnnotationListURL%>" />', { module:'${param.module}',project:$('#project').val() }, function(jsonResult) {
					if (jsonResult.length == 0)
	 				{	// obviously no annotations
						$("#geneNameHeader").hide();
						$("#snpEffectHeader").hide();
						$('div#annotationFilters').hide();
					}
					else
					{
						$("#geneNameHeader").show();
						$("#snpEffectHeader").show();
						$('div#annotationFilters').show();
						$("#alleleCount").empty();
						$("#variantEffects").empty();
						$("#variantEffects").append("<option value=''></option>");
						for (var key in jsonResult)
							$("#variantEffects").append("<option value='" + jsonResult[key] + "'>" + jsonResult[key] + "</option>");
						$('.chosen-select').chosen();
					}
				}).error(function(xhr, ajaxOptions, thrownError) {
					handleJsonError(xhr, ajaxOptions, thrownError);
	        	});
			}
			// add abbr attribute to rows where project name is used
			$("#snpEffectHeader").attr("abbr","<%=VariantRunData.SECTION_ADDITIONAL_INFO%>." + "<%=VariantRunData.FIELDNAME_ADDITIONAL_INFO_EFFECT_NAME%>");
			$("#geneNameHeader").attr("abbr","<%=VariantRunData.SECTION_ADDITIONAL_INFO%>." + "<%=VariantRunData.FIELDNAME_ADDITIONAL_INFO_EFFECT_GENE%>");
			dirtyForm();
		}
		
		function handleJsonError(xhr, ajaxOptions, thrownError) {
			if ("parsererror" == ajaxOptions && xhr.responseText.indexOf("j_spring_security_check") > -1)
				top.location.reload();	// looks like a session-timeout

           	$('#errorAlertAnchor').attr("onclick", "alert(unescape('" + escape($.parseJSON(xhr.responseText)['errorMsg']) + "'));");
           	$("div#exportFormatDetails").hide();
           	$('#errorBlock').css('visibility', 'visible');
           	setTimeout("$('#errorBlock').css('visibility', 'hidden');", 5000);
           	$('#progressDiv').html('');
           	abortCurrentOperation();
        }
		
		function isValidMongoObjectId(id)
		{
			if(id == null) return false;
			if(id != null && 'number' != typeof id && (id.length != 12 && id.length != 24)) {
				return false;
			} else {
				// Check specifically for hex correctness
				if(typeof id == 'string' && id.length == 24) return new RegExp("^[0-9a-fA-F]{24}$").test(id);
					return false;
			}
		};

		function displaySequenceFilterMessage()
		{
			$.getJSON('<c:url value="<%=AbstractVariantController.sequenceFilterCountURL%>" />', { module:'${param.module}' }, function(jsonResult){
				var nSequenceFilterCount = parseInt(jsonResult);
				if (nSequenceFilterCount < 0)
					$("#sequenceFilterMessage").html("");
				else
					$("#sequenceFilterMessage").html("<div style='text-align:center; margin-right:15px; padding:3px; background-color:#21A32C; border:2px outset #666; color:#fff;'>Variants are currently being searched within a subset of externally selected sequences<input type='button' style='margin-top:3px;' value='Clear this filter' onclick=\"clearSequenceFilterInSession();\"></div>");
			}).error(function(xhr, ajaxOptions, thrownError) {
				handleJsonError(xhr, ajaxOptions, thrownError);
        	});
		}
		
		function clearSequenceFilterInSession()
		{
			$.getJSON('<c:url value="<%=AbstractVariantController.clearSelectedSequenceListURL%>" />', { module:'${param.module}',rand:Math.random() }, function(jsonCountResult){
				displaySequenceFilterMessage();
				loadSequenceList();
			}).error(function(xhr, ajaxOptions, thrownError) {
				handleJsonError(xhr, ajaxOptions, thrownError);
        	});	
		}

		function getResultTableFields()
		{
			var resultTableFields = "";
			$('table#variantResultTable th:visible').each(function() {
				resultTableFields += (resultTableFields == "" ? "" :";") + $(this).attr('abbr');
			})
			return resultTableFields;			
		}
		
		function enableMafOnlyIfGtCodeAndAlleleNumberAllowTo()
		{
			var gtCodebox = document.getElementById('gtCode');
			var gtCodeAllows = !gtCodebox.options[5].selected && !gtCodebox.options[7].selected;
			
			var alleleNumber = $('#alleleCount option').length == 1 ? $('#alleleCount option:eq(0)').val() :getSelectedNumberOfAlleles(true);
			if (ploidy > 2 || !gtCodeAllows || alleleNumber != 2)
			{
				$('#minmaf').attr('disabled', 'disabled');
				$('#minmaf').val("0");
				$('#maxmaf').attr('disabled', 'disabled');
				$('#maxmaf').val("50");
			}
			else
			{
				$('#minmaf').removeAttr('disabled');
				$('#maxmaf').removeAttr('disabled');
			}
		}
		
		function getSelectedVariantTypes(returnEntireListIfNoSelection)
		{
			var selectedVariantTypes = $('#variantTypes').val();
			if (selectedVariantTypes == null && returnEntireListIfNoSelection == true)
			{
				selectedVariantTypes = new Array();
				$('#variantTypes option').each(function() {
					selectedVariantTypes.push($(this).val());
				});
			}
			return selectedVariantTypes == null ? "" :selectedVariantTypes.join(';');
		}

		function getSelectedIndividuals()
		{
			if ($('#individuals option').length == 1)
				return $('#individuals option:eq(0)').val();
			var selectedIndividuals = $('#individuals').val();
			if (selectedIndividuals == null || selectedIndividuals.length == $('#individuals option').length)
				return "";
			return selectedIndividuals.join(';');	
		} 
		
		function getSelectedSequences()
		{
			if ($('#sequences option').length == 1)
				return $('#sequences option:eq(0)').val();
			var selectedSequences = $('#sequences').val();
			if (selectedSequences == null || selectedSequences.length == $('#sequences option').length)
				return "";
			return selectedSequences.join(';');	
		}
		
		function getSelectedNumberOfAlleles(returnEvenIfUnique)
		{
			var selectedNumberOfAlleles = $('#alleleCount').val();
			if (returnEvenIfUnique && selectedNumberOfAlleles == null && $('#alleleCount option').length == 1)
			{
				selectedNumberOfAlleles = new Array();
				selectedNumberOfAlleles.push($('#alleleCount option').val());
			}
			return selectedNumberOfAlleles == null ? "" :selectedNumberOfAlleles.join(';');
		}
		
		function applySequenceSelection()
		{
			var selectIndividualCSV = getSelectedSequences();
			var nSelectedIndividualCount = selectIndividualCSV == "" ? 0 :selectIndividualCSV.split(";").length;
			$("#sequenceCount").html(" (" + (nSelectedIndividualCount == 0 ? $("#sequences option").size() :nSelectedIndividualCount) + " / " + $("#sequences option").size() + ")");
		}	
		
		function applyIndividualSelection()
		{
			var op = document.getElementById('gtCode').getElementsByTagName('option');
			var selectIndividualCSV = getSelectedIndividuals();
			var nSelectedIndividualCount = selectIndividualCSV == "" ? 0 :selectIndividualCSV.split(";").length;
			$("#individualCount").html(" (" + (nSelectedIndividualCount == 0 ? $("#individuals option").size() :nSelectedIndividualCount) + " / " + $("#individuals option").size() + ")");
			
			for (var i=1; i<5; i++)
			{
				if (nSelectedIndividualCount == 1 && op[i].selected)
					op[0].selected = true;
			
				op[i].disabled = nSelectedIndividualCount == 1;
			}		

			if (nSelectedIndividualCount < 3)
				op[11].disabled = true;
		}	
		
 		function disableBiAllelicSpecificQueriesIfNeeded()
 		{
			var opt = document.getElementById('gtCode').getElementsByTagName('option');
			var alleleNumber = $('#alleleCount option').length == 1 ? $('#alleleCount option:eq(0)').val() :getSelectedNumberOfAlleles(true);
			if (ploidy != 2 || alleleNumber != 2)
 			{
				opt[11].disabled = true;
				if (opt[11].selected)
					opt[0].selected = true;
			}
			else
				opt[11].disabled = false;
 			$('#gtCode').change();
 		}		
		
		function isNumberKey(evt)
	    {
	         var charCode = /*(evt.which) ?*/ evt.which /*:evt.keyCode*/;
	         if ((charCode < 48 || charCode > 57) && charCode != 0 && charCode != 8 && charCode != 9 && charCode != 37 && charCode != 39)
	            return false;
	         
	         return true;
	    }   

		function exportSelectedData()
		{
			var allowedVariantTypes = $('#exportFormat option:selected').attr("abbr").split(";");
			if (allowedVariantTypes != "")
			{
				var selectedTypes = getSelectedVariantTypes(true).split(";");
				for (var key in selectedTypes)
				{
					if (!arrayContains(allowedVariantTypes, selectedTypes[key]))
					{
						alert("This export format does not support the following variant type:" + selectedTypes[key]);
						return;
					}
				}
			}
			
	        $("#variantExportDialog #variantExportDialogTitle").html("Exporting selected variants (" + totalRecordCount + ")");
	        $("#variantExportDialog").modal({
	        	opacity:80,
	        	overlayCss:{backgroundColor:"#111"}
	        });
	        
	        var exportProcessID = "variantExport" + new Date().getTime() + "|" + interfaceID;
	        currentProgressQueryID = exportProcessID;
	        $("#variantExportFrame").attr('src', "<c:url value="<%=AbstractVariantController.variantExportPageURL%>" />?module=${param.module}&project=" + $('#project').val() + "&keepExportOnServer=" + ($('#keepExportOnServer').attr('checked') == 'checked' ? 'true' :'false') + "&variantTypes=" + getSelectedVariantTypes() + "&sequences=" + getSelectedSequences() + "&gtCode=" + $('#gtCode').val() + "&individuals=" + getSelectedIndividuals() + "&genotypeQualityThreshold=" + $('#genotypeQualityThreshold').val() + "&readDepthThreshold=" + $('#readDepthThreshold').val() + "&missingData=" + $('#missingdata').val() + "&minmaf=" + $('#minmaf').val() + "&maxmaf=" + $('#maxmaf').val() + "&minposition=" + $('#minposition').val() + "&maxposition=" + $('#maxposition').val() + "&alleleCount=" + getSelectedNumberOfAlleles(false) + "&exportSelectedIndividualsOnly=" + ($('#exportSelectedIndividualsOnly').attr('checked') == 'checked' ? 'true' :'false') + "&geneName=" + encodeURIComponent($('#geneName').val().trim().replace(new RegExp(' , ', 'g'), ',')) + "&variantEffects=" + ($('#variantEffects').val() == null ? "" :$('#variantEffects').val()) + "&exportFormat=" + $('#exportFormat').val() + "&processID=" + exportProcessID);
		}
		
		function closeDialog()
		{
			$.modal.close();
		}
		
		function abortExport()
		{
			$.getJSON('<c:url value="<%= AbstractVariantController.processAbortURL %>" />', { processID:$('#variantExportFrame')[0].contentWindow.exportID }, function(jsonResult){
				if (jsonResult == true)
				{
// 					currentProgressQueryID = null;
					$("#variantExportFrame").attr('src', "../blank.html");
				}
				else
					alert("Unable to abort!");
			});				
		}
		
		function onPageSizeChange()
		{
			if (typeof getVariable('maxPageSize') == 'undefined')
				setVariable("maxPageSize", 1000);
			
			var pageSizerInput = $("#pageSizer input:eq(0)");
			if (isNaN(pageSizerInput.val()) || pageSizerInput.val() > getVariable('maxPageSize'))
				pageSizerInput.val(getVariable('pageSize'));
			else
			{
				setVariable("pageSize", pageSizerInput.val());
				setVariable("pageNumber", 0);
			}
		}
		
		function initialiseNavigationVariables()
		{
			setVariable("pageSize", 100);
			setVariable("maxPageSize", 1000);
			setVariable("pageNumber", 0);
			setVariable("sortBy", "");
			setVariable("sortDir", "asc");
		}
		
		function browsingBoxChanged()
		{
			var fEnabled = $("input#browsingAndExportingEnabled").is(":checked");
			$.cookie('browsingAndExportingEnabled', fEnabled ? 1 :0);
			$('.hideable').toggle(fEnabled);
			$("div#exportLaunchDiv").toggle(fEnabled);
			if (totalRecordCount > 0 && fEnabled && $("#variantResultTable tr:gt(0)").length == 0)
				findData();
			showHideNavigationLinks();
		}

		function checkBrowsingBoxAccordingToCookie()
		{
			if ($.cookie('browsingAndExportingEnabled') == 1)
				$('input#browsingAndExportingEnabled').attr('checked', 'checked');
			else
				$('input#browsingAndExportingEnabled').removeAttr('checked');
		}
		
		function showDensity()
		{
	        $("#chartDialog").modal({
	        	opacity:80,
	        	overlayCss:{backgroundColor:"#111"},
	        	onClose: function(dialog){
	        		$('#chartFrame').attr('src', null);	// force unload event
	        	    $.modal.close();
	        	}
	        });
	        
	        postDataToIFrame("chartFrame", '<c:url value="<%= AbstractVariantController.chartPageURL %>" />', {chartType:'density',module:'${param.module}',project:$('#project').val(),variantTypes:getSelectedVariantTypes(),sequences:getSelectedSequences(),gtCode:$('#gtCode').val(),genotypeQualityThreshold:$('#genotypeQualityThreshold').val(),readDepthThreshold:$('#readDepthThreshold').val(),missingData:$('#missingdata').val(),minmaf:$('#minmaf').val(),maxmaf:$('#maxmaf').val(),minposition:$('#minposition').val(),maxposition:$('#maxposition').val(),alleleCount:getSelectedNumberOfAlleles(false),geneName:encodeURIComponent($('#geneName').val().trim().replace(new RegExp(' , ', 'g'), ',')),variantEffects:($('#variantEffects').val() == null ? "" :$('#variantEffects').val()),individuals:getSelectedIndividuals(),processID:currentProgressQueryID});
		}
		
		window.onbeforeunload = function (e) {
			document.getElementById('progressDiv').innerHTML = 'Cleaning up...';						
			$.ajax({
 				async:false,
				url:"<c:url value='<%= AbstractVariantController.interfaceCleanupURL %>' />",
				data:{ module:"${param.module}", processID:currentProgressQueryID }
			});
		}

		initialiseNavigationVariables();
		</script>
</head>

<body style='background-color:#f0f0f0;' onload="applyProjectSelectionToProjectDependantWidgets(); displaySequenceFilterMessage(); applyIndividualSelection(); checkBrowsingBoxAccordingToCookie(); $('input#browsingAndExportingEnabled').change(); $('select#exportFormat').change();">
	<c:choose>
		<c:when test="${fn:length(projects) > 0}">
			<form onsubmit="return false;" id="theForm" style="margin-bottom:5px;">
				<table width="970" cellpadding='0' cellspacing='2'>
					<tr>
						<td valign="top" width="70"><b>Variant types</b><br> <select size="13" multiple id="variantTypes" name="variantTypes" style="width:65px;" onchange="dirtyForm();"></select> <input
							type="button" style="margin-top:8px; width:65px;"
							onclick="if (confirm('Are you sure?')) {$('form#theForm')[0].reset(); $('.chosen-select option').prop('selected', false).trigger('chosen:updated'); dirtyForm(); initialiseNavigationVariables(); $('table#variantResultTable th').attr('class', 'sortableTableHeader'); checkBrowsingBoxAccordingToCookie();}"
							value="Clear filters"></td>
						<td valign="top" width="75"><b>Sequences</b>
							<div id="sequenceCount">&nbsp;</div>
							<div id="sequenceLoader" style="position:absolute; width:70px; padding-top:10px; padding-bottom:10px; background-color:#cccccc; opacity:0.8; display:none; text-align:center;"></div> <select
							size="14" multiple id="sequences" name="sequences" style="min-width:70px;" onchange="dirtyForm(); applySequenceSelection();"></select></td>
						<td valign="top"><b>Individuals</b>
							<div id="individualCount">&nbsp;</div> <select size="14" multiple id="individuals" name="individuals" style="min-width:70px; max-width:170px;"
							onchange="dirtyForm(); applyIndividualSelection();"></select></td>
						<td>&nbsp;</td>
						<td valign="top" style="min-width:350px;"><b>Genotypes:</b> <select id="gtCode"
							onchange="dirtyForm(); $('#gtCodeDesc').html(options[selectedIndex].title); enableMafOnlyIfGtCodeAndAlleleNumberAllowTo();">
								<c:forEach var="code" items="${genotypeCodes}">
									<option value="${code.key}" title="${code.value}">${code.key}</option>
								</c:forEach>
						</select> <img src="../img/icon_qmark.png" style="position:absolute; margin-top:3px;" onmouseover="$('div#gtCodeDesc').css('display', 'inline');" onmouseout="$('div#gtCodeDesc').hide();" /> <br />
							<div class='infobulle' id='gtCodeDesc'></div>

							<div id="filterOnGQandDP" style="height:50px;">
								<div style="margin-top:5px;">
									<b>Minimum per-sample genotype quality:</b> <input type="text" maxlength="4" style="width:16px;" id="genotypeQualityThreshold" value="1"
										onchange="dirtyForm(); if(!(this.value>1))this.value='1';" onkeypress="return isNumberKey(event);" onfocus="this.select();" /> (other data seen as missing)
								</div>
	
								<div style="margin-top:5px;">
									<b>Minimum per-sample read depth:</b> <input type="text" maxlength="4" style="width:16px;" id="readDepthThreshold" value="1" onchange="dirtyForm(); if(!(this.value>1))this.value='1';"
										onkeypress="return isNumberKey(event);" onfocus="this.select();" /> (other data seen as missing)
								</div>
							</div>

							<div style="margin-top:5px;">
								<b>Authorized missing data ratio:</b> <input type="text" maxlength="3" style="width:25px; margin-bottom:5px;" name="texte" id="missingdata" value="100"
									onchange="dirtyForm(); if(this.value>100)this.value='100';" onkeypress="return isNumberKey(event);" onfocus="this.select();" /> %
							</div>

							<div id="filterOnMaf" style="margin-top:5px;">
								<b>Minor allele frequency:</b> from <input type="text" maxlength="2" style="width:20px;" name="texte" id="minmaf" value="0"
									onchange="dirtyForm(); if (this.value<0 || this.value>50) this.value='0';" onkeypress="return isNumberKey(event);" onfocus="this.select();" />% to <input type="text" maxlength="2"
									style="width:20px;" name="texte" id="maxmaf" value="50" onchange="dirtyForm(); if (this.value<0 || this.value>50) this.value='50';" onkeypress="return isNumberKey(event);"
									onfocus="this.select();" />%
							</div>

							<div id="filterAlternateAllele" style="position:absolute; top:80px; margin-left:240px; white-space:nowrap; text-align:right;">
								<b>Number of alleles:</b> <br /> <select size="4" multiple id="alleleCount" name="alleleCount" onchange="dirtyForm(); disableBiAllelicSpecificQueriesIfNeeded();"></select>
							</div>

							<div id="filterOnPosition" style="margin-top:5px;">
								<b>Position:</b> Min <input type="text" maxlength="9" style="width:52px;" name="texte" id="minposition" onchange="dirtyForm();" onkeypress="return isNumberKey(event);" /> - Max <input
									type="text" maxlength="9" style="width:52px;" name="texte" id="maxposition" onchange="dirtyForm();" onkeypress="return isNumberKey(event);" /> bp
							</div>

							<div id="annotationFilters" style="margin-top:5px;">
								<select data-placeholder="Click to choose variant effects..." class="chosen-select" multiple style="width:350px;" id="variantEffects" name="variantEffects" onchange="dirtyForm();"></select>
								<div style="margin-top:5px;">
									<b>Genes:</b> <input type="text" style="width:300px;" name="texte" id="geneName" onchange="dirtyForm();" onkeydown="if(event.keyCode==32) return false;" /><img src="../img/icon_qmark.png"
										style="position:absolute; margin-top:3px;" onmouseover="$('div#geneNameInfo').css('display', 'inline');" onmouseout="$('div#geneNameInfo').hide();" /> <br />
									<div class='infobulle' id='geneNameInfo'>
										Leave blank to ignore this filter<br />Enter "-" for variants without gene-name annotation<br />Enter "+" for variants with any gene-name annotation<br />Enter comma-separated names for
										specific genes
									</div>
								</div>
							</div></td>
						<td>&nbsp;</td>
						<td valign="top" style="width:260px; height:210px; background-color:#f8f8f8; border:1px outset #888888; padding:8px;">							
							<div align="right" style="margin-bottom:10px;">
								Project <select id="project" onchange="applyProjectSelectionToProjectDependantWidgets();" style="max-width:160px;">
									<c:forEach var="project" items="${projects}">
										<option value="${project.key}">${project.value}</option>
									</c:forEach>
								</select>
								<input type="button" style='margin-left:5px;' onclick="setVariable('pageNumber', 0); loadData();" value="Search" id="searchButton">
							</div>
							<input type="checkbox" id="browsingAndExportingEnabled" onchange="browsingBoxChanged();" style="margin-left:60px; margin-top:0; margin-bottom:10px;" />&nbsp;Enable browsing and exporting
							<div style="height:45px;">
								<div style="width:250px; position:absolute; font-size:18px; font-weight:bold; padding:10px; text-align:center;" id="resultCountDiv"></div>
								<div style="width:260px; position:absolute; white-space:nowrap; background-color:#f8f8f8;" id="exportLaunchDiv">
									<c:if test="${exportFormats.VCF ne null}">
										<img style="position:absolute; float:left; margin-top:-5px; margin-left:30px; cursor:pointer; cursor:hand;" src="../img/lightbulb.gif" title="TIP: You may view selected variants in their genomic context by starting IGV, ticking the 'Keep export file(s) on server' box and exporting in VCF format" />
									</c:if>
									<input type="checkbox" id="keepExportOnServer" style="margin-left:60px; margin-top:0; margin-bottom:0;" />&nbsp;Keep export file(s) on server
									<div align='right' style="margin-bottom:5px; vertical-align:bottom; width:100%;">
										<c:if test="${!empty exportFormats}">
											Export as
											<select id="exportFormat" onchange="$('div#exportFormatDetails').html($('select#exportFormat option:selected').attr('title'));">
												<c:forEach var="exportFormat" items="${exportFormats}">
													<option value="${exportFormat.key}" title="${exportFormat.value.desc}" abbr="${exportFormat.value.supportedVariantTypes}">${exportFormat.key}</option>
												</c:forEach>
											</select>
											<img style="position:relative; top:2px; cursor:pointer; cursor:hand; padding-top:3px;" src="../img/icon_qmark.png" title="Toggle export format details" onclick="$('div#exportFormatDetails').toggle();"/>
											<input type="button" id="exportButton" value="Export selection" onclick="exportSelectedData();" disabled>
										</c:if>
									</div>
								</div>
							</div>

							<div id="sequenceFilterMessage" style="width:170px; height:65px; margin-top:5px; margin-left:60px; position:absolute;"></div>
							<div style="position:absolute; margin-top:5px; margin-left:15px; width:220px; height:65px; background-color:#ffaaaa; text-align:center; visibility:hidden" class="formErrors" id="errorBlock">
								<div style="margin:10px; font-size:15px; font-weight:bold;">Sorry, an error occured</div>
								<a href="#" id="errorAlertAnchor">Click for technical details</a> <br />&nbsp;
							</div>
							<div id="exportFormatDetails" style="display:none; padding:5px; background-color:#ffffff; border:1px #7f7f7f inset; width:240px; height:65px; margin-top:5px; margin-left:5px; position:absolute;"></div>

							<div style="width:260px; position:absolute; margin-top:90px; text-align:right;">
								<table cellspacing="2" cellpadding="0" width="100%">
									<tr>
										<td width="90%" align="right"><div id="progressDiv" style="width:195px;"></div></td>
										<td><input type="button" disabled onclick="abortCurrentOperation();" value="Abort" id="abortButton"></td>
									</tr>
								</table>
							</div>
						</td>
					</tr>
				</table>
			</form>

			<div id="LoadingOverlay"></div>
			<div id="variantResultDiv">
				<table class="resultTableNavButtons">
						<td nowrap style='width:70px;'><a id="previousButton" href="#" onclick="incrementVariable('pageNumber', true); loadData();">&lt; Previous</a></td>
						<td nowrap id='listCounter' style="min-width:100px; text-align:center;"></td>
						<td nowrap style='width:70px;'><a id="nextButton" class="contentDialog" href="#" onclick="incrementVariable('pageNumber', false); loadData();">Next &gt;</a></td>
						<td width='30'>&nbsp;</td>
					</tr>
				</table>

				<table class="adminListTable hideable" id="variantResultTable">
					<tr height="18">
						<th class='sortableTableHeader' abbr='#_id'>&nbsp;ID&nbsp;</th>
						<th class='sortableTableHeader' abbr='#<%=VariantData.FIELDNAME_REFERENCE_POSITION + "." + ReferencePosition.FIELDNAME_SEQUENCE%>'>&nbsp;Sequence&nbsp;</th>
						<th class='sortableTableHeader' abbr='#<%=VariantData.FIELDNAME_REFERENCE_POSITION + "." + ReferencePosition.FIELDNAME_START_SITE%>'>&nbsp;Start&nbsp;</th>
						<th class='sortableTableHeader' abbr='#<%=VariantData.FIELDNAME_REFERENCE_POSITION + "." + ReferencePosition.FIELDNAME_END_SITE%>'>&nbsp;Stop&nbsp;</th>
						<th class='' abbr='#<%=VariantData.FIELDNAME_KNOWN_ALLELE_LIST%>'>Alleles</th>
						<th class='' id="snpEffectHeader">&nbsp;Variant effect&nbsp;</th>
						<th class='' id="geneNameHeader">&nbsp;Gene name&nbsp;</th>
						<td style="border:none; display:none; height:14px;" id="chartLink"><a href="#" onclick="showDensity();"><img style="border:1px solid #4444cc; margin-left:5px;" title="Variant density chart" src="../img/densityIcon.gif" height="14" width="52" /></a></td>
					</tr>
				</table>
			</div>

			<div id="variantInfoDialog" align="center" style="margin:15px; width:100%; display:none; position:absolute; top:0px; left:0px; z-index:100;">
				<table style="border-spacing:0; background:#fff; background-color:#21A32C; border:2px solid #E0E0E0; margin-top:2px;" cellpadding='0' cellspacing='0'>
					<tr>
						<td align='center'><div id="variantInfoDialogTitle"></div></td>
					</tr>
					<tr>
						<td align='center'><iframe style='margin:15px; width:850px; min-height:400px;' id="variantInfoFrame"></iframe> <br>
							<form>
								<input type='button' value='Close' class="simplemodal-close">
							</form></td>
					</tr>
				</table>
			</div>
			<div id="variantExportDialog" align="center" style="margin:15px; width:100%; display:none; position:absolute; top:0px; left:0px; z-index:100;">
				<table style="border-spacing:0; background:#fff; background-color:#21A32C; border:2px solid #E0E0E0; margin-top:2px;" cellpadding='0' cellspacing='0'>
					<tr>
						<td align='center'><div id="variantExportDialogTitle"></div></td>
					</tr>
					<tr>
						<td align='center'><iframe style='margin:15px; width:800px; min-height:190px;' id="variantExportFrame"></iframe> <br>
							<form>
								<input type='button' value='Close' onclick="if ($('#variantExportFrame')[0].contentWindow.exportID != null) abortExport();" class="simplemodal-close">
							</form></td>
					</tr>
				</table>
			</div>
			<div id="chartDialog" align="center" style="margin:15px; width:100%; display:none; position:absolute; top:0px; left:0px; z-index:100;">
				<table style="border-spacing:0; background:#fff; background-color:#21A32C; border:2px solid #E0E0E0; margin-top:2px;" cellpadding='0' cellspacing='0'>
					<tr>
						<td align='center'><div id="chartDialogTitle"></div></td>
					</tr>
					<tr>
						<td align='center'><iframe style='margin:15px; width:900px; min-height:450px;' name="chartFrame" id="chartFrame"></iframe> <br>
							<form>
								<input type='button' value='Close' class="simplemodal-close">
							</form></td>
					</tr>
				</table>
			</div>
			<div id="sequenceLoadDialog" align="center" style="margin:15px; width:100%; display:none; position:absolute; top:0px; left:0px; z-index:100;">Loading sequences...</div>
		</c:when>
		<c:otherwise>
			<h3>No variant data is available for this module</h3>
		</c:otherwise>
	</c:choose>

</body>

</html>