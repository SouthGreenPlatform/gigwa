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
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" import="fr.cirad.web.controller.gigwa.GigwaController,fr.cirad.web.controller.gigwa.base.AbstractVariantController" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<sec:authorize access="hasRole('ROLE_ADMIN')" var="isAdmin"/>
<html>
<head>
	<link rel ="stylesheet" type="text/css" href="../css/main.css" title="style">
	<script type="text/javascript" src="../js/jquery-1.8.2.min.js"></script>
	<script type="text/javascript">
	var projectModules = new Array();
	<c:forEach var="moduleProjectsAndRuns" items="${modulesProjectsAndRuns}">
	projectModules["${moduleProjectsAndRuns.key}"] = new Array();
		<c:forEach var="projectsAndRuns" items="${moduleProjectsAndRuns.value}">
			projectModules["${moduleProjectsAndRuns.key}"]["${projectsAndRuns.key}"] = new Array();
			<c:forEach var="projectAndRuns" items="${projectsAndRuns.value}">
				projectModules["${moduleProjectsAndRuns.key}"]["${projectsAndRuns.key}"].push("${projectAndRuns}");
			</c:forEach>
		</c:forEach>
	</c:forEach>
	
	function moduleChanged()
	{
		var selectedModule = $("#modules").val();
		var existingModuleSelected = selectedModule != '';
		$('#newModuleDiv').toggle(!existingModuleSelected);
		$('input[name=module]').val(existingModuleSelected ? selectedModule : "");
		var projectOptions = existingModuleSelected ? "" : "<option value=''>- New project -</option>";
		for (var pjKey in projectModules[selectedModule])
			projectOptions += "<option value='" + pjKey + "'>" + (pjKey == "" ? "- New project -" : pjKey) + "</option>";
		$("#projects").html(projectOptions);
		projectChanged();
	}

	function projectChanged()
	{
		var selectedModule = $('input[name=module]').val();
		var selectedProject = $("#projects").val();
		var existingProjectSelected = selectedProject != '';
		$('#newProjectDiv').toggle(!existingProjectSelected);
		if (existingProjectSelected)
			$('input[name=clearProjectData]').removeAttr("disabled");
		else
			$('input[name=clearProjectData]').attr("disabled", "disabled");
		$('input[name=project]').val(existingProjectSelected ? selectedProject : "");
		var runOptions = "<option value=''>- New run -</option>";
		for (var runKey in projectModules[selectedModule][selectedProject])
			runOptions += "<option value='" + projectModules[selectedModule][selectedProject][runKey] + "'>" + projectModules[selectedModule][selectedProject][runKey] + "</option>";
		$("#runs").html(runOptions);
		runChanged();
	}

	function runChanged()
	{
		var selectedRun = $("#runs").val();
		var existingRunSelected = selectedRun != '';
		$('#newRunDiv').toggle(!existingRunSelected);
		$('input[name=run]').val(existingRunSelected ? selectedRun : "");
	}
	
	function isValidKeyForNewName(evt)
	{
         return isValidCharForNewName((evt.which) ? evt.which : evt.keyCode);
	}
	
	function isValidCharForNewName(charCode)
	{
		return ((charCode >= 48 && charCode <= 57) || (charCode >= 65 && charCode <= 90) || (charCode >= 97 && charCode <= 122) || charCode == 8 || charCode == 9 || charCode == 35 || charCode == 36 || charCode == 37 || charCode == 39 || charCode == 45 || charCode == 46 || charCode == 95);
	}
	
	function isValidNewName(newName)
	{
		for (var i=0; i<newName.length; i++)
			if (!isValidCharForNewName(newName.charCodeAt(i)))
				return false;
		return true;
	}
	
	function launchImport()
	{
		var host = $("select#host").val();
		var module = $("input[name=module]").val().trim();
		var project = $("input[name=project]").val().trim();
		var run = $("input[name=run]").val().trim();
		var mainFile = $("input[name=mainFile]").val().trim();
		
		if (!isValidNewName(module) || !isValidNewName(project) || !isValidNewName(run))
		{
			alert("Database, project and run names must be simple: digits, accentless letters, dashes and hyphens!");
			return;
		}
		
		if (module == "" || project == "" || run == "" || mainFile == "")
		{
			alert("You must provide a value for each mandatory entry!");
			return;
		}
		if ($('#newModuleDiv').is(":visible") && $("#modules option[value=" + $('input[name=module]').val() + "]").length > 0)
		{
			alert("This database already exists!");
			return;
		}
		if ($('#newProjectDiv').is(":visible") && $("#projects option[value=" + $('input[name=project]').val() + "]").length > 0)
		{
			alert("This project already exists!");
			return;
		}
		if ($('#newRunDiv').is(":visible") && $("#runs option[value=" + $('input[name=run]').val() + "]").length > 0)
		{
			alert("This run already exists!");
			return;
		}
		
		var clearProjectDataBox = $('input[name=clearProjectData]');
		var clearProjectData = clearProjectDataBox.is(":visible") && clearProjectDataBox.attr('checked') == 'checked';
		
		var technology = $("input[name=technology]").val().trim();
		if (technology == exampleTechnologyString)
			technology = "";
		$("#importButton").attr('disabled', 'disabled');
		$("form select").attr('disabled', 'disabled');
		$("form input").attr('disabled', 'disabled');
		$.getJSON('<c:url value="<%= GigwaController.genotypingDataImportSubmissionURL %>" />', { host:host,module:module,project:project,run:run,technology:technology,clearProjectData:clearProjectData,mainFile:mainFile }, function(jsonResult) {
			setTimeout("checkProcessProgress(\"../" + jsonResult + "\");", minimumProcessQueryIntervalUnit);
		}).error(function(xhr, ajaxOptions, thrownError) {
			$('div#progressDiv').html();
// 			alert("Error occured :\n\n" + $($.parseHTML(xhr.responseText)).find("h2").text().trim());
// 			console.log("Error occured 2:\n\n" + xhr.responseText);
			$('#importButton').removeAttr('disabled');
			$('form select').removeAttr('disabled');
			$('form input').removeAttr('disabled');
// 			handleJsonError(xhr, ajaxOptions, thrownError);
			alert($.parseJSON(xhr.responseText)['errorMsg']);
    	});
	}

	var minimumProcessQueryIntervalUnit = 1000;
	var currentProgressQueryID = null;
	
	function checkProcessProgress(processIDCheckURL)
	{		
		$.getJSON(processIDCheckURL, {}, function(jsonResult){
			if (jsonResult != null)
			{
				if (jsonResult['error'] != null)
				{
					$('div#progressDiv').html();
					alert("Error occured:\n\n" + jsonResult['error']);
					$('#importButton').removeAttr('disabled');
					$('form select').removeAttr('disabled');
					$('form input').removeAttr('disabled');
				}
				else
				{
					$('div#progressDiv').html(jsonResult['progressDescription']);
					setTimeout("checkProcessProgress(\"" + processIDCheckURL + "\");", minimumProcessQueryIntervalUnit);
				}
			}
			else
				setTimeout("handleImportSuccess();", 100);
		}).error(function(xhr, ajaxOptions, thrownError) {
			handleJsonError(xhr, ajaxOptions, thrownError);			
    	});
	}
	
	function handleImportSuccess()
	{
		var importedModule = $('input[name=module]').val();
		if ($('select#modules', window.parent.document).find("option[value=" + importedModule + "]").length == 0)
			$('select#modules', window.parent.document).append("<option value=\"" + importedModule + "\">" + importedModule + "</option>");
		$('div#progressDiv').html('Your data is now <a href=\"<c:url value="<%= AbstractVariantController.variantSearchPageURL %>" />?module=' + importedModule + '\" onclick=\"$(\'select#modules\', window.parent.document).val(\'' + importedModule + '\');\">available here</a>');
	}
	
	function handleJsonError(xhr, ajaxOptions, thrownError)
	{
       	alert($.parseJSON(xhr.responseText)['errorMsg']);
       	$('div#progressDiv').html();
	}
	
	var exampleTechnologyString = "chip / sequencer name...";
	</script>
</head>

<body bgcolor="#f0f0f0" onload="moduleChanged(); $('#importButton').removeAttr('disabled'); $('form select').removeAttr('disabled'); $('form input').removeAttr('disabled'); $('form')[0].reset(); $('input[name=technology]').blur(); $('input[name=clearProjectData]').attr('disabled', 'disabled');">
<div style="width:100%;">
	<div style="width:650px; margin:auto;">
	<h2>Data import</h2>
	<blockquote>Import is supported via specification of preliminarily uploaded files. Make sure your file is in a location that is accessible to the user owning the application server process.</blockquote>
	<form>
	<table cellpadding='5' cellspacing='1' bgcolor='#000000' id="formTable">
		<tr bgcolor='#eeeeff' height="53">
		<th><label class="required">Database</label></th>
		<td width="220">
		<select id="modules" style="margin-right:10px;" onchange="moduleChanged();">
			<c:if test="${isAdmin}"><option value="" selected>- New database -</option></c:if>
			<c:forEach var="moduleProjectsAndRuns" items="${modulesProjectsAndRuns}">
				<option value="${moduleProjectsAndRuns.key}">${moduleProjectsAndRuns.key}</option>
			</c:forEach>
		</select>
		</td>
		<td align='right'>
		<c:choose>
			<c:when test="${isAdmin}">
				<div id="newModuleDiv">
				Host
				<select id="host" style="margin-bottom:5px; min-width:130px;">
					<c:forEach var="host" items="${hosts}">
						<option value="${host}">${host}</option>
					</c:forEach>
				</select>
				<br/>
				<label class="required">New database name</label>
				<input type="text" name="module" style="width:130px;" maxlength="30" onkeypress="return isValidKeyForNewName(event);" />
				</div>
			</c:when>
			<c:otherwise>
				<input type="hidden" name="module" />
			</c:otherwise>
		</c:choose>
		</td>
		</tr>
		<tr bgcolor='#eeffee' height="53">
		<th><label class="required">Project</label></th>
		<td>
		<select id="projects" style="margin-right:10px; margin-bottom:5px;" onchange="projectChanged();">
			<option value="" selected>- New project -</option>
		</select>
		<br/><div style="float:right;">Clear existing project data before importing <input type='checkbox' name="clearProjectData" /></div>
		</td>
		<td align='right'>
			<div id="newProjectDiv">
			<label class="required">New project name</label>
			<input type="text" name="project" style="width:130px;" maxlength="30" onkeypress="return isValidKeyForNewName(event);" />
			<br/>Technology
			<input type="text" name="technology" style="width:130px;" maxlength="30" onfocus='if ($(this).val().trim() == exampleTechnologyString) $(this).val("");' onblur='if ($(this).val().trim() == "") $(this).val(exampleTechnologyString);' />
			</div>
		</td>
		</tr>
		<tr bgcolor='#eeeeff' height="53">
		<th><label class="required">Run</label></th>
		<td>
		<select id="runs" style="margin-right:10px;" onchange="runChanged();">
			<option value="" selected>- New run -</option>
		</select>
		</td>
		<td align='right'>
			<div id="newRunDiv">
			<label class="required">New run name</label>
			<input type="text" name="run" style="width:130px;" maxlength="30" onkeypress="return isValidKeyForNewName(event);" />
			</div>
		</td>
		</tr>

		<tr bgcolor='#ffeeee' height="53">
		<th><label class="required">Genotype file path</label><br/><span style='font-weight:normal;'>Supported formats:<br/>VCF, HapMap, PLINK</span></th>
		<td colspan="2" align="center">
		<input type="text" name="mainFile" style="width:460px;" value="" /><br />Please provide absolute path on webserver filesystem (only specify .ped for PLINK format)</td>
		</tr>
	</table>
	<p>
		<label class="required" style="border:1px solid #EE3322; padding:2px;">Mandatory fields</label>
		<input type="button" id="importButton" value="Import" style="position:absolute; margin-top:-5px; margin-left:440px;" onclick="launchImport();" />
	</p>
	<div id='progressDiv' style="margin-top:20px;"></div>
	</form>
	</div>
</div>	
</body>

</html>