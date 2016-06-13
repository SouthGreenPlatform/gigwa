/*******************************************************************************
 * GIGWA - Genotype Investigator for Genome Wide Analyses
 * Copyright (C) 2016 <CIRAD>
 *     
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License, version 3 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * See <http://www.gnu.org/licenses/gpl-3.0.html> for details about
 * GNU Affero General Public License V3.
 *******************************************************************************/
filtersToColumns = new Array();
/*
Array.prototype.contains = function (element) 
{
	for (var i = 0; i < this.length; i++) 
        if (this[i] == element) 
        	return true;
	return false;
};

Array.prototype.containsIgnoreCase = function (element) 
{
	for (var i = 0; i < this.length; i++) 
        if ((this[i] == null && element == null) || (this[i] != null && element != null && this[i].toLowerCase() == element.toLowerCase())) 
        	return true;
	return false;
};
*/

if (typeof String.prototype.trim !== 'function') {
	String.prototype.trim = function() {
		return this.replace(/^\s+|\s+$/g, '');
	};
}

function arrayContains(array, element) 
{
	for (var i = 0; i < array.length; i++) 
        if (array[i] == element) 
        	return true;
	return false;
};

function arrayContainsIgnoreCase(array, element)
{
	for (var i = 0; i < array.length; i++) 
        if ((array[i] == null && element == null) || (array[i] != null && element != null && array[i].toLowerCase() == element.toLowerCase())) 
        	return true;
	return false;
};

function containsHtmlTags(xStr)
{
	return xStr != xStr.replace(/<\/?[^>]+>/gi,"");
}

var filtersAdded = false;

function enableColumnFilters(tableId, filterColumnClassName, filterDivId)
{
	var filterDiv = document.getElementById(filterDivId);
	var tableObj = document.getElementById(tableId);

	columnCount = tableObj.rows[0].cells.length;
	mainLoop : for (c=0; c<columnCount; c++)
	{				
		if (tableObj.rows[0].cells[c].className != filterColumnClassName)
			continue mainLoop;
		
		distinctValuesForColumn = new Array();
		for (r=1; r<tableObj.rows.length; r++)
		{
			if (containsHtmlTags($(tableObj.rows[r].cells[c]).text()))
				continue mainLoop;	// we don't create filter drop-downs for HTML contents
			
			var foundInArray = false;
			for (var i=0; i < distinctValuesForColumn.length; i++) 
		        if (distinctValuesForColumn[i] == $(tableObj.rows[r].cells[c]).text())
		        {
		        	foundInArray = true;
		        	break;
		        }
			if (!foundInArray)
				distinctValuesForColumn[distinctValuesForColumn.length] = $(tableObj.rows[r].cells[c]).text();
		}

		distinctValuesForColumn.sort();

		if (!filtersAdded && distinctValuesForColumn.length < tableObj.rows.length - 1)
		{
			dropDown = document.createElement("select");
			dropDown.onchange = function() {
					applyFilters(tableId); 
					if (typeof customDropDownOnChange != 'undefined')
						customDropDownOnChange();
				};

			dropDown.options[dropDown.length] = new Option("", "");
			for (i=0; i<distinctValuesForColumn.length; i++)
			{
				anOption = new Option(distinctValuesForColumn[i], distinctValuesForColumn[i]);
				if (distinctValuesForColumn[i] == parent.frames['gpList'].locusFilters[tableObj.rows[0].cells[c].innerHTML])
					anOption.selected = true;
				dropDown.options[dropDown.length] = anOption;
			}

			filtersToColumns[c] = dropDown;
			dropDownPara = document.createElement("p");
			dropDownPara.innerHTML = "Filter by " + tableObj.rows[0].cells[c].innerHTML;
			dropDownPara.appendChild(document.createElement("br"));
			dropDownPara.appendChild(dropDown);
			filterDiv.appendChild(dropDownPara);
		}
	}
	filtersAdded = true;
	applyFilters(tableId);
}

function applyFilters(tableId)
{
//	alert("applyFilters");
	tableObj = document.getElementById(tableId);
	if (tableObj.rows.length < 1)
		return;

	columnCount = tableObj.rows[0].cells.length;
	for (r=1; r<tableObj.rows.length; r++)
	{
		displayVal = "";
		for (c=0; c<columnCount; c++)
		{
			if (filtersToColumns[c] != null)
			{
				filterVal = filtersToColumns[c].options[filtersToColumns[c].selectedIndex].value;
				if (filterVal != "" && filterVal.toLowerCase().trim() != $(tableObj.rows[r].cells[c]).text().toLowerCase().trim())
				{
					displayVal = "none";
					if (r > 1)
						break;
				}
				if (r == 1)
					parent.frames['gpList'].locusFilters[tableObj.rows[0].cells[c].innerHTML] = filterVal;
			}
		}
		$(tableObj.rows[r]).css("display", displayVal);
	}
	
	if (typeof afterSortFunction == 'function')
		afterSortFunction();
}

function hideRedundantCells(tableId, upToColumnIndex)
{
	var rowAboveThisOne = $("#" + tableId + " tr:eq(0)"); 
	$("#" + tableId + " tr:visible").each(function(rowIdx) {
		if (rowIdx > 0)
		{
			for (var colIdx = 0; colIdx<=upToColumnIndex; colIdx++)
			{
				var thisCell = $($(this).children()[colIdx]);
				var cellAboveThisOne = $(rowAboveThisOne.children()[colIdx]);
				var hiddenCell = rowIdx > 1 && cellAboveThisOne.text() == thisCell.text() && "none" != cellAboveThisOne.css("display");

				$(thisCell.children()[0]).css("visibility", hiddenCell ? "hidden" : "visible");
				thisCell.css("border-bottom-color", thisCell.css("border-left-color"));
				cellAboveThisOne.css("border-bottom-color", hiddenCell ? thisCell.css("background-color") : thisCell.css("border-left-color"));
			}
			rowAboveThisOne = $(this);
		}
	});
}

$.extend($.expr[":"], { 
  "containsIgnoreCase": function(elem, i, match, array) { 
     return (elem.textContent || elem.innerText || "").toLowerCase().indexOf((match[3] || "").toLowerCase()) >= 0; 
  }
});

$.extend($.expr[":"], { 
  "optionValueEqualsIgnoreCase": function(elem, i, match, array) { 
     return (elem.textContent || elem.innerText || "").toLowerCase() == (match[3] || "").toLowerCase(); 
  }
});

function postDataToIFrame(frameName, url, params)
{
     var form = document.createElement("form");
     form.setAttribute("method", "post");
     form.setAttribute("action", url);
     form.setAttribute("target", frameName);

     for (var i in params) {
         var input = document.createElement('input');
         input.type = 'hidden';
         input.name = i;
         input.value = params[i];
         form.appendChild(input);
     }
     
     document.body.appendChild(form);
     form.submit();
     document.body.removeChild(form);
	}