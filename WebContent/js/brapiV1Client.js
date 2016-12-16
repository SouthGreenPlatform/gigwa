const URL_CALLS = "calls";
const URL_MAPS = "maps";
const URL_STUDIES = "studies-search";
const URL_MARKERS = "markers";
const URL_MARKER_PROFILES = "markerprofiles";
const URL_ALLELE_MATRIX = "allelematrix-search";

const REQUIRED_CALLS = new Array(URL_MAPS, URL_MARKERS, URL_STUDIES, URL_MARKER_PROFILES, URL_ALLELE_MATRIX);

const TIMEOUT = 10000;

function checkEndPoint()
{
	if (typeof BRAPI_V1_URL_ENDPOINT == 'undefined')
	{
		alert("You must specify an endpoint by setting the BRAPI_V1_URL_ENDPOINT variable!");
		return false;
	}
	
	if (!BRAPI_V1_URL_ENDPOINT.endsWith("/"))
		BRAPI_V1_URL_ENDPOINT += "/";
	
	var unimplementedCalls = new Array();
	
	$.ajax({
	    type:"GET",
	    async:false,
	    url:BRAPI_V1_URL_ENDPOINT + URL_CALLS,
	    traditional:true,
	    timeout:TIMEOUT,
	    success:function(jsonResponse) {
	    	var dataList = getDataList(jsonResponse);
	    	mainLoop: for (var i=0; i<REQUIRED_CALLS.length; i++)
	    	{
		    	for (var j=0; j<dataList.length; j++)
		    		if (REQUIRED_CALLS[i] == dataList[j]['call'])
		    			continue mainLoop;
		    	unimplementedCalls.push(REQUIRED_CALLS[i]);
	    	}
	    },
	    error:function(xhr, ajaxOptions, thrownError) {
	    	alert($.parseJSON(xhr.responseText)['errorMsg']);
	    }
	});
	
	if (unimplementedCalls.length > 0)
	{
		alert("This BRAPI service does not supported the following call(s): " + unimplementedCalls.join(", "));
		return false;
	}
	return true;
}

function getDataList(jsonResponse)
{
	if (jsonResponse['result'] == null)
		throw("No 'result' key in jsonResponse!");
	
	if (jsonResponse['result']['data'] == null)
		throw("No 'data' key in jsonResponse['result']!");
	
	return jsonResponse['result']['data'];
}

function readMapList()
{
	if (!checkEndPoint())
		return null;

	var dataList;
	$.ajax({
	    type:"GET",
	    url:BRAPI_V1_URL_ENDPOINT + URL_MAPS,
	    async:false,
	    timeout:TIMEOUT,
	    success:function(jsonResponse) {
	    	dataList = getDataList(jsonResponse);
	    },
	    error:function(xhr, ajaxOptions, thrownError) {
			handleJsonError(xhr, ajaxOptions, thrownError);
	    }
	});
	return dataList;
}

function readStudyList(studyType)
{
	if (!checkEndPoint())
		return null;
	
	var parameters = studyType == null ? null : {studyType:studyType};

	var result = new Array();
	$.ajax({
	    type:"GET",
	    url:BRAPI_V1_URL_ENDPOINT + URL_STUDIES,
	    async:false,
	    data:parameters,
	    timeout:TIMEOUT,
	    success:function(jsonResponse) {
	    	var dataList = getDataList(jsonResponse);
	    	for (var j=0; j<dataList.length; j++)
	    		if (studyType == dataList[j]['studyType'])
	    			result.push(dataList[j]);
	    },
	    error:function(xhr, ajaxOptions, thrownError) {
			handleJsonError(xhr, ajaxOptions, thrownError);
	    }
	});
	return result;
}