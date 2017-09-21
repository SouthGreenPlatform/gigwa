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
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" import="fr.cirad.web.controller.gigwa.base.AbstractVariantController" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jstl/fmt" %>
<fmt:setBundle basename="config" />

<fmt:message var="igvDataLoadPort" key="igvDataLoadPort" />
<fmt:message var="igvGenomeListUrl" key="igvGenomeListUrl" />

<html>

<head>
	<link rel ="stylesheet" type="text/css" href="../css/main.css" title="style">	
	<script type="text/javascript" src="../js/jquery-1.8.2.min.js"></script>
	<script type="text/javascript" src="../js/main.js"></script>
	<script type="text/javascript">
		$.ajaxSetup({ cache:false });
	
		var exportID;
		function exportVariants()
		{
			exportID = "${param.processID}";
			
			postDataToIFrame("outputFrame", '<c:url value="<%= AbstractVariantController.variantExportDataURL %>" />', {module:'${param.module}',exportFormat:'${param.exportFormat}',project:'${param.project}',keepExportOnServer:'${param.keepExportOnServer}',variantTypes:'${param.variantTypes}',sequences:'${param.sequences}',gtPattern:'${param.gtPattern}',genotypeQualityThreshold:'${param.genotypeQualityThreshold}',readDepthThreshold:'${param.readDepthThreshold}',missingData:'${param.missingData}',minmaf:'${param.minmaf}',maxmaf:'${param.maxmaf}',minposition:'${param.minposition}',maxposition:'${param.maxposition}',alleleCount:'${param.alleleCount}',geneName:encodeURIComponent('${param.geneName}'.trim().replace(new RegExp(' , ', 'g'), ',')),variantEffects:'${param.variantEffects}',individuals:'${param.individuals}',exportID:exportID});

			setTimeout("checkProcessProgress(\"" + exportID + "\");", minimumProcessQueryIntervalUnit);
			
			document.getElementById('progressDiv').innerHTML = '';
			document.getElementById('progressDiv').style.display = 'block';
		}
				
		var minimumProcessQueryIntervalUnit = 1000;
		var currentProgressQueryID = null;
		
		function checkProcessProgress(processID)
		{
			$.getJSON('<c:url value="<%= AbstractVariantController.progressIndicatorURL %>" />', { module:'${param.module}',processID:processID }, function(jsonResult){
				if (jsonResult != null)
				{
					if (jsonResult['error'] != null)
					{
						if ("<%= AbstractVariantController.MESSAGE_TEMP_RECORDS_NOT_FOUND %>" == jsonResult['error'])
							parent.totalRecordCount = 0;

						alert("Error occured:\n\n" + jsonResult['error']);
						setTimeout("parent.closeDialog();", 0);	// without setTimeout it closes immediately and the alert message cannot even be seen
					}
					else
					{
						$('div#progressDiv').html(jsonResult['progressDescription']);
						setTimeout("checkProcessProgress(\"" + processID + "\");", minimumProcessQueryIntervalUnit);
					}
				}
				else
				{
					exportID = null;
					setTimeout("handleExportSuccess();", 100);
				}
			}).error(function(xhr, ajaxOptions, thrownError) {
				handleJsonError(xhr, ajaxOptions, thrownError);
				exportID = null;
        	});
		}
		
		function handleExportSuccess()
		{			
//  			console.log($('#outputFrame').contents().context.contentType);
			var exportOutputUrl = $('#outputFrame').contents().find('body').text();
			if (exportOutputUrl == "")
				setTimeout("parent.closeDialog();", minimumProcessQueryIntervalUnit*3);
			else
			{
				$('div#progressDiv').html("Export output will be available at this link's URL for a week:<p>&nbsp;<br/><a id='exportOutputUrl' href='" + exportOutputUrl + "' style='background-color:#e0e0e0; border:2px outset #777777; padding:3px 25px 3px 3px;'>" + exportOutputUrl.substring(exportOutputUrl.lastIndexOf("/") + 1) + " <img style='position:absolute; margin:-4px 0px 0px 3px;' src='../img/download.gif'/></a></p>");
				if (addIgvExportIfRunning != null)
					addIgvExportIfRunning();
			}
		}
		
		<c:if test='${param.exportFormat eq "VCF" && !fn:startsWith(igvDataLoadPort, "??") && !empty igvDataLoadPort}'>
		var igvGenomeOptions = null;
        function addIgvExportIfRunning() { 
            $.ajax({
			    type:"GET",
			    url:"http://127.0.0.1:${igvDataLoadPort}",
			    success:function(jsonResult) {
			    	if ("ERROR Unknown command: /" == jsonResult)
			    	{
						if (igvGenomeOptions == null)
						{
							var genomeList = $.ajax({
				            	async:false,
							    type:"GET",
							    url:"${igvGenomeListUrl}",
				 			    crossDomain : true,
								error:function(xhr, ajaxOptions, thrownError) {}
							});
							
					    	igvGenomeOptions = "<option>&nbsp;</option>";
					    	if (genomeList.responseText != null)
					    	{
						    	var genomeLines = genomeList.responseText.split("\n");
						    	for (var i=0; i<genomeLines.length; i++)
						    		if (i > 0 || !genomeLines[i].startsWith("<"))	// skip header
						    		{
						    			var genomeFields = genomeLines[i].split("\t");
						    			if (genomeFields.length == 3)
						    				igvGenomeOptions += "<option value='" + genomeFields[2] + "'>" + genomeFields[0] + "</option>";
						    		}
					    	}
					    	$('div#progressDiv').append("<center><table><tr><th valign='top'>View in IGV within genomic/structural context&nbsp;</th><td align='center'><select id='igvGenome' style='min-width:175px;'>" + igvGenomeOptions + "</select><br/>(you may select a genome to switch to)</td><td valign='top'>&nbsp;<input type='button' value='Send' onclick='sendToIGV();'/></td></tr></table></center>");
						}
			    	}
				},
				error:function(xhr, ajaxOptions, thrownError) {
					handleJsonError(xhr, ajaxOptions, thrownError);
	        	}
			});
        }
        
        function sendToIGV(genomeID)
        {
        	var genomeID = $("select#igvGenome").val();
            $.ajax({
			    type:"GET",
			    url:"http://127.0.0.1:${igvDataLoadPort}/load?" + (genomeID != "" ? "genome=" + genomeID + "&" : "") + "file=" + location.origin + $("a#exportOutputUrl").attr("href").replace(new RegExp('.zip$'), '.vcf'),
			    success:function(tsvResult) {
					alert("Variant list was sent to IGV!");
				},
				error:function(xhr, ajaxOptions, thrownError) {
					handleJsonError(xhr, ajaxOptions, thrownError);
	        	}
			});
        }
		</c:if>

		
		function handleJsonError(xhr, ajaxOptions, thrownError) {
          	alert($.parseJSON(xhr.responseText)['errorMsg']);
			setTimeout("parent.closeDialog();", 0);	// without setTimeout it closes immediately and the alert message cannot even be seen
        }
	</script>
	</head>

<body style='background-color:#f0f0f0; overflow:hidden;' onload="exportVariants();">
	<div id='progressDiv' style="margin-top:60px;"></div>
	<iframe style='display:none;' id='outputFrame' name='outputFrame'></iframe>
</body>

</html>