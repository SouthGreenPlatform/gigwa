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
package fr.cirad.web.controller.gigwa.base;

import htsjdk.variant.vcf.VCFFormatHeaderLine;
import htsjdk.variant.vcf.VCFInfoHeaderLine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.map.UnmodifiableMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.SimpleMappingExceptionResolver;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;

import com.mongodb.AggregationOptions;
import com.mongodb.AggregationOptions.Builder;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.Bytes;
import com.mongodb.Cursor;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

import fr.cirad.mgdb.exporting.IExportHandler;
import fr.cirad.mgdb.exporting.individualoriented.AbstractIndividualOrientedExportHandler;
import fr.cirad.mgdb.exporting.markeroriented.AbstractMarkerOrientedExportHandler;
import fr.cirad.mgdb.model.mongo.maintypes.DBVCFHeader;
import fr.cirad.mgdb.model.mongo.maintypes.DBVCFHeader.VcfHeaderId;
import fr.cirad.mgdb.model.mongo.maintypes.VariantRunData.VariantRunDataId;
import fr.cirad.mgdb.model.mongo.maintypes.GenotypingProject;
import fr.cirad.mgdb.model.mongo.maintypes.VariantData;
import fr.cirad.mgdb.model.mongo.maintypes.VariantRunData;
import fr.cirad.mgdb.model.mongo.subtypes.ReferencePosition;
import fr.cirad.mgdb.model.mongo.subtypes.SampleGenotype;
import fr.cirad.mgdb.model.mongo.subtypes.SampleId;
import fr.cirad.mgdb.model.mongodao.MgdbDao;
import fr.cirad.tools.AlphaNumericStringComparator;
import fr.cirad.tools.AppConfig;
import fr.cirad.tools.Helper;
import fr.cirad.tools.ProgressIndicator;
import fr.cirad.tools.mgdb.GenotypingDataQueryBuilder;
import fr.cirad.tools.mongo.MongoTemplateManager;

/**
 * The Class AbstractVariantController.
 */
public abstract class AbstractVariantController implements IGigwaViewController
{

	/** The Constant LOG. */
	protected static final Logger LOG = Logger.getLogger(AbstractVariantController.class);

	/** The exception resolver. */
	@Autowired private SimpleMappingExceptionResolver exceptionResolver;

	/** The Constant SEQLIST_FOLDER. */
	static final public String SEQLIST_FOLDER = "selectedSeqs";

	/** The Constant TMP_OUTPUT_FOLDER. */
	static final private String TMP_OUTPUT_FOLDER = "tmpOutput";

	/** The Constant FRONTEND_URL. */
	static final public String FRONTEND_URL = "genofilt";

	/** The Constant CHART_URL. */
	static final public String CHART_URL = "charts";

	/** The Constant JOB_OUTPUT_EXPIRATION_DELAY_MILLIS. */
	static final private long JOB_OUTPUT_EXPIRATION_DELAY_MILLIS = 1000*60*60*24*7;	/* 7 days */

	/** The Constant variantSearchPageURL. */
	static final public String variantSearchPageURL = "/" + FRONTEND_URL + "/VariantSearch.do";

	/** The Constant variantTypesListURL. */
	static final public String variantTypesListURL = "/" + FRONTEND_URL + "/variantTypesList.json";

	/** The Constant numberOfAlleleListURL. */
	static final public String numberOfAlleleListURL = "/" + FRONTEND_URL + "/numberOfAlleleList.json";

	/** The Constant sequenceListURL. */
	static final public String sequenceListURL = "/" + FRONTEND_URL + "/sequenceList.json";

	/** The Constant projectEffectAnnotationListURL. */
	static final public String projectEffectAnnotationListURL = "/" + FRONTEND_URL + "/projectEffectAnnotationList.json";

	/** The Constant gotGQFieldURL. */
	static final public String gotGQFieldURL = "/" + FRONTEND_URL + "/gotGQField.json";

	/** The Constant ploidyURL. */
	static final public String ploidyURL = "/" + FRONTEND_URL + "/ploidy.json";

	/** The Constant individualListURL. */
	static final public String individualListURL = "/" + FRONTEND_URL + "/individualList.json";

	/** The Constant variantCountURL. */
	static final public String variantCountURL = "/" + FRONTEND_URL + "/countVariants.json";

	/** The Constant variantFindURL. */
	static final public String variantFindURL = "/" + FRONTEND_URL + "/findVariants.json";

	/** The Constant variantListURL. */
	static final public String variantListURL = "/" + FRONTEND_URL + "/listVariants.json";

	/** The Constant variantDetailsURL. */
	static final public String variantDetailsURL = "/" + FRONTEND_URL + "/VariantDetails.do";

	/** The Constant variantExportPageURL. */
	static final public String variantExportPageURL = "/" + FRONTEND_URL + "/VariantExport.do";

	/** The Constant variantExportDataURL. */
	static final public String variantExportDataURL = "/" + FRONTEND_URL + "/exportVariants.do";

	/** The Constant progressIndicatorURL. */
	static final public String progressIndicatorURL = "/" + FRONTEND_URL + "/progressIndicator.json";

	/** The Constant sequenceFilterCountURL. */
	static final public String sequenceFilterCountURL = "/" + FRONTEND_URL + "/sequenceFilterCount.json";

	/** The Constant clearSelectedSequenceListURL. */
	static final public String clearSelectedSequenceListURL = "/" + FRONTEND_URL + "/clearSelectedSequences.json";

	/** The Constant processAbortURL. */
	static final public String processAbortURL = "/" + FRONTEND_URL + "/processAbort.json_";

	/** The Constant interfaceCleanupURL. */
	static final public String interfaceCleanupURL = "/" + FRONTEND_URL + "/interfaceCleanup.json";

	/** The Constant chartPageURL. */
	static final public String chartPageURL = "/" + FRONTEND_URL + "/ChartPage.do";

	/** The Constant selectionDensityDataURL. */
	static final public String selectionDensityDataURL = "/" + FRONTEND_URL + "/selectionDensity.json";

	/** The Constant distinctSequencesInSelectionURL. */
	static final public String distinctSequencesInSelectionURL = "/" + FRONTEND_URL + "/distinctSequencesInSelection.json";

	/** The Constant GENOTYPE_CODE_LABEL_ALL. */
	static final protected String GENOTYPE_CODE_LABEL_ALL = "Any";

	/** The Constant GENOTYPE_CODE_LABEL_NOT_ALL_SAME. */
	static final protected String GENOTYPE_CODE_LABEL_NOT_ALL_SAME = "Not all same";

	/** The Constant GENOTYPE_CODE_LABEL_ALL_SAME. */
	static final protected String GENOTYPE_CODE_LABEL_ALL_SAME = "All same";

	/** The Constant GENOTYPE_CODE_LABEL_ALL_DIFFERENT. */
	static final protected String GENOTYPE_CODE_LABEL_ALL_DIFFERENT = "All different";

	/** The Constant GENOTYPE_CODE_LABEL_NOT_ALL_DIFFERENT. */
	static final protected String GENOTYPE_CODE_LABEL_NOT_ALL_DIFFERENT = "Not all different";

	/** The Constant GENOTYPE_CODE_LABEL_ALL_HOMOZYGOUS_REF. */
	static final protected String GENOTYPE_CODE_LABEL_ALL_HOMOZYGOUS_REF = "All Homozygous Ref";

	/** The Constant GENOTYPE_CODE_LABEL_ATL_ONE_HOMOZYGOUS_REF. */
	static final protected String GENOTYPE_CODE_LABEL_ATL_ONE_HOMOZYGOUS_REF = "At least one Homozygous Ref";

	/** The Constant GENOTYPE_CODE_LABEL_ALL_HOMOZYGOUS_VAR. */
	static final protected String GENOTYPE_CODE_LABEL_ALL_HOMOZYGOUS_VAR = "All Homozygous Var";

	/** The Constant GENOTYPE_CODE_LABEL_ATL_ONE_HOMOZYGOUS_VAR. */
	static final protected String GENOTYPE_CODE_LABEL_ATL_ONE_HOMOZYGOUS_VAR = "At least one Homozygous Var";

	/** The Constant GENOTYPE_CODE_LABEL_ALL_HETEROZYGOUS. */
	static final protected String GENOTYPE_CODE_LABEL_ALL_HETEROZYGOUS = "All Heterozygous";

	/** The Constant GENOTYPE_CODE_LABEL_ATL_ONE_HETEROZYGOUS. */
	static final protected String GENOTYPE_CODE_LABEL_ATL_ONE_HETEROZYGOUS = "At least one Heterozygous";

	/** The Constant GENOTYPE_CODE_LABEL_WITHOUT_ABNORMAL_HETEROZYGOSITY. */
	static final protected String GENOTYPE_CODE_LABEL_WITHOUT_ABNORMAL_HETEROZYGOSITY = "Without abnormal heterozygosity";

	/** The Constant genotypeCodeToDescriptionMap. */
	static final protected HashMap<String, String> genotypeCodeToDescriptionMap = new LinkedHashMap<String, String>();

	/** The Constant genotypeCodeToQueryMap. */
	static final protected HashMap<String, String> genotypeCodeToQueryMap = new HashMap<String, String>();

	/** The Constant MESSAGE_TEMP_RECORDS_NOT_FOUND. */
	static final public String MESSAGE_TEMP_RECORDS_NOT_FOUND = "Unable to find temporary records: please SEARCH again!";

	/** The Constant MAX_SORTABLE_RESULT_COUNT. */
	static final public int MAX_SORTABLE_RESULT_COUNT = 1000000;

	/** The Constant NUMBER_OF_SIMULTANEOUS_QUERY_THREADS. */
	static final private int NUMBER_OF_SIMULTANEOUS_QUERY_THREADS = 5;

	/** The nf. */
	static protected NumberFormat nf = NumberFormat.getInstance();
	
    @Autowired
    private AppConfig appConfig;

	static
	{
		nf.setMaximumFractionDigits(4);

		genotypeCodeToDescriptionMap.put(GENOTYPE_CODE_LABEL_ALL, "This will return all variants whithout applying any filters");
		genotypeCodeToDescriptionMap.put(GENOTYPE_CODE_LABEL_NOT_ALL_SAME, "This will return variants where not all selected individuals have the same genotype");
		genotypeCodeToDescriptionMap.put(GENOTYPE_CODE_LABEL_ALL_SAME, "This will return variants where all selected individuals have the same genotype");
		genotypeCodeToDescriptionMap.put(GENOTYPE_CODE_LABEL_ALL_DIFFERENT, "This will return variants where none of the selected individuals have the same genotype");
		genotypeCodeToDescriptionMap.put(GENOTYPE_CODE_LABEL_NOT_ALL_DIFFERENT, "This will return variants where some of the selected individuals have the same genotypes");
		genotypeCodeToDescriptionMap.put(GENOTYPE_CODE_LABEL_ALL_HOMOZYGOUS_REF, "This will return variants where selected individuals are all homozygous with the reference allele");
		genotypeCodeToDescriptionMap.put(GENOTYPE_CODE_LABEL_ATL_ONE_HOMOZYGOUS_REF, "This will return variants where selected individuals are at least one homozygous with the reference allele");
		genotypeCodeToDescriptionMap.put(GENOTYPE_CODE_LABEL_ALL_HOMOZYGOUS_VAR, "This will return variants where selected individuals are all homozygous with an alternate allele");
		genotypeCodeToDescriptionMap.put(GENOTYPE_CODE_LABEL_ATL_ONE_HOMOZYGOUS_VAR, "This will return variants where selected individuals are at least one homozygous with an alternate allele");
		genotypeCodeToDescriptionMap.put(GENOTYPE_CODE_LABEL_ALL_HETEROZYGOUS, "This will return variants where selected individuals are all heterozygous");
		genotypeCodeToDescriptionMap.put(GENOTYPE_CODE_LABEL_ATL_ONE_HETEROZYGOUS, "This will return variants where selected individuals are at least one heterozygous");
		genotypeCodeToDescriptionMap.put(GENOTYPE_CODE_LABEL_WITHOUT_ABNORMAL_HETEROZYGOSITY, "This will return variants where each allele found in heterozygous genotypes is also found in homozygous ones (only for diploid, bi-allelic data)");
		genotypeCodeToQueryMap.put(GENOTYPE_CODE_LABEL_ALL, null);
		genotypeCodeToQueryMap.put(GENOTYPE_CODE_LABEL_ALL_SAME, "$eq");
		genotypeCodeToQueryMap.put(GENOTYPE_CODE_LABEL_NOT_ALL_SAME, "$eq" + GenotypingDataQueryBuilder.AGGREGATION_QUERY_NEGATION_SUFFIX);
		genotypeCodeToQueryMap.put(GENOTYPE_CODE_LABEL_ALL_DIFFERENT, "$ne");
		genotypeCodeToQueryMap.put(GENOTYPE_CODE_LABEL_NOT_ALL_DIFFERENT, "$ne" + GenotypingDataQueryBuilder.AGGREGATION_QUERY_NEGATION_SUFFIX);
		genotypeCodeToQueryMap.put(GENOTYPE_CODE_LABEL_ALL_HOMOZYGOUS_REF, "^0(/0)*$|^$" + GenotypingDataQueryBuilder.AGGREGATION_QUERY_REGEX_APPLY_TO_ALL_IND_SUFFIX);
		genotypeCodeToQueryMap.put(GENOTYPE_CODE_LABEL_ATL_ONE_HOMOZYGOUS_REF, "^0(/0)*$|^$" + GenotypingDataQueryBuilder.AGGREGATION_QUERY_REGEX_APPLY_TO_AT_LEAST_ONE_IND_SUFFIX);
		genotypeCodeToQueryMap.put(GENOTYPE_CODE_LABEL_ALL_HOMOZYGOUS_VAR, "^([1-9][0-9]*)(/\\1)*$|^$" + GenotypingDataQueryBuilder.AGGREGATION_QUERY_REGEX_APPLY_TO_ALL_IND_SUFFIX);
		genotypeCodeToQueryMap.put(GENOTYPE_CODE_LABEL_ATL_ONE_HOMOZYGOUS_VAR, "^([1-9][0-9]*)(/\\1)*$|^$" + GenotypingDataQueryBuilder.AGGREGATION_QUERY_REGEX_APPLY_TO_AT_LEAST_ONE_IND_SUFFIX);
		genotypeCodeToQueryMap.put(GENOTYPE_CODE_LABEL_ALL_HETEROZYGOUS, "([0-9])([0-9])*(/(?!\\1))+([0-9])*|^$" + GenotypingDataQueryBuilder.AGGREGATION_QUERY_REGEX_APPLY_TO_ALL_IND_SUFFIX);
		genotypeCodeToQueryMap.put(GENOTYPE_CODE_LABEL_ATL_ONE_HETEROZYGOUS, "([0-9])([0-9])*(/(?!\\1))+([0-9])*|^$" + GenotypingDataQueryBuilder.AGGREGATION_QUERY_REGEX_APPLY_TO_AT_LEAST_ONE_IND_SUFFIX);
		genotypeCodeToQueryMap.put(GENOTYPE_CODE_LABEL_WITHOUT_ABNORMAL_HETEROZYGOSITY, GenotypingDataQueryBuilder.AGGREGATION_QUERY_WITHOUT_ABNORMAL_HETEROZYGOSITY);
	}

	/**
	 * Gets the project ploidy level.
	 *
	 * @param sModule the module
	 * @param projId the proj id
	 * @return the project ploidy level
	 * @throws Exception the exception
	 */
	protected abstract int getProjectPloidyLevel(String sModule, int projId) throws Exception;

	/**
	 * Does project contain gq field.
	 *
	 * @param sModule the module
	 * @param projId the proj id
	 * @return true, if successful
	 */
	protected abstract boolean doesProjectContainGQField(String sModule, int projId);

	/**
	 * Gets the project effect annotations.
	 *
	 * @param sModule the module
	 * @param projId the proj id
	 * @return the project effect annotations
	 * @throws Exception the exception
	 */
	protected abstract TreeSet<String> getProjectEffectAnnotations(String sModule, int projId) throws Exception;

	/**
	 * Gets the project sequences.
	 *
	 * @param sModule the module
	 * @param projId the proj id
	 * @return the project sequences
	 * @throws Exception the exception
	 */
	protected abstract List<String> getProjectSequences(String sModule, int projId) throws Exception;

	/**
	 * Gets the project distinct allele counts.
	 *
	 * @param sModule the module
	 * @param projId the proj id
	 * @return the project distinct allele counts
	 * @throws Exception the exception
	 */
	protected abstract TreeSet<Integer> getProjectDistinctAlleleCounts(String sModule, int projId) throws Exception;

	/**
	 * Gets the project variant types.
	 *
	 * @param sModule the module
	 * @param projId the proj id
	 * @return the project variant types
	 * @throws Exception the exception
	 */
	protected abstract List<String> getProjectVariantTypes(String sModule, int projId) throws Exception;

	/**
	 * Gets the project id to name map.
	 *
	 * @param sModule the module
	 * @return the project id to name map
	 * @throws Exception the exception
	 */
	protected abstract Map<Comparable, String> getProjectIdToNameMap(String sModule) throws Exception;

	/**
	 * Gets the individuals in db order.
	 *
	 * @param sModule the module
	 * @param projId the proj id
	 * @return the individuals in db order
	 * @throws Exception the exception
	 */
	protected abstract List<String> getIndividualsInDbOrder(String sModule, int projId) throws Exception;

	/* (non-Javadoc)
	 * @see fr.cirad.web.controller.gigwa.base.IGigwaViewController#getViewDescription()
	 */
	@Override
	public String getViewDescription() {
		return "Variant positions";
	}

	/* (non-Javadoc)
	 * @see fr.cirad.web.controller.gigwa.base.IGigwaViewController#getViewURL()
	 */
	@Override
	public String getViewURL() {
		return variantSearchPageURL;
	}

    /**
     * Handle all exceptions.
     *
     * @param request the request
     * @param response the response
     * @param ex the ex
     * @return the model and view
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
    public ModelAndView handleAllExceptions(HttpServletRequest request, HttpServletResponse response, Exception ex) {
    	LOG.error("Error at URL " + request.getRequestURI() + "?" + request.getQueryString(), ex);
    	if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With")))
    	{
    		HashMap<String, String> map = new HashMap<String, String>();
    		map.put("errorMsg", ExceptionUtils.getStackTrace(ex));
    		return new ModelAndView(new MappingJackson2JsonView(), UnmodifiableMap.decorate(map));
    	}
    	else
    		return exceptionResolver.resolveException(request, response, null, ex);
    }

	/**
	 * This method get the query in relation with a specific genotype code.
	 *
	 * @param gtCode the gt code
	 * @return the query for genotype code
	 */
	public static String getQueryForGenotypeCode(String gtCode)
	{
		return genotypeCodeToQueryMap.get(gtCode);
	}

	/**
	 * Setup search page.
	 *
	 * @param request the request
	 * @param sModule the module
	 * @return the model and view
	 * @throws Exception the exception
	 */
	@RequestMapping(variantSearchPageURL)
	protected ModelAndView setupSearchPage(final HttpServletRequest request, final HttpServletResponse response,  @RequestParam("module") String sModule) throws Exception
	{
		new Thread()
		{
			public void run()
			{
				try
				{
					cleanupOldTempData(request);
				}
				catch (IOException e1)
				{
					LOG.error("Unable to cleanup old temporary data", e1);
				}
			}
		}.start();
        
//		response.addHeader("X-Frame-Options", "ALLOW-FROM http://172.20.30.22:8081");
		
		ModelAndView mav = new ModelAndView();
		mav.addObject("projects", getProjectIdToNameMap(sModule));
		mav.addObject("genotypeCodes", genotypeCodeToDescriptionMap);
		TreeMap<String /*format name*/, HashMap<String /*info field name ("desc", "supportedVariantTypes", ...*/, String /*info field value*/>> exportFormats = new TreeMap<String, HashMap<String, String>>();
		for (IExportHandler exportHandler : AbstractIndividualOrientedExportHandler.getIndividualOrientedExportHandlers().values())
		{
			HashMap<String, String> info = new HashMap<String, String>();
			info.put("desc", exportHandler.getExportFormatDescription());
			info.put("supportedVariantTypes", StringUtils.join(exportHandler.getSupportedVariantTypes(), ";"));
			exportFormats.put(exportHandler.getExportFormatName(), info);
		}
		for (IExportHandler exportHandler : AbstractMarkerOrientedExportHandler.getMarkerOrientedExportHandlers().values())
		{
			HashMap<String, String> info = new HashMap<String, String>();
			info.put("desc", exportHandler.getExportFormatDescription());
			info.put("supportedVariantTypes", StringUtils.join(exportHandler.getSupportedVariantTypes(), ";"));
			exportFormats.put(exportHandler.getExportFormatName(), info);
		}
		mav.addObject("exportFormats", exportFormats);
		String genomeBrowserURL = appConfig.get("genomeBrowser-" + sModule);
		mav.addObject("genomeBrowserURL", genomeBrowserURL == null ? "" : genomeBrowserURL);
		return mav;
	}

	/**
	 * List individuals in alpha numeric order.
	 *
	 * @param sModule the module
	 * @param project the project
	 * @return the list
	 * @throws Exception the exception
	 */
	@RequestMapping(individualListURL)
	/**
	 * This method returns the list of individual from cache to supply the interface VariantSearch
	 *
	 * @param sModule
	 * @param projId
	 * @return
	 * @throws Exception
	 */
	protected @ResponseBody List<String> listIndividualsInAlphaNumericOrder(@RequestParam("module") String sModule, @RequestParam("project") int project) throws Exception
	{
		List<String> indArray = new ArrayList(getIndividualsInDbOrder(sModule, project));
		Collections.sort(indArray, new AlphaNumericStringComparator());
		return indArray;
	}

	/**
	 * List variant types sorted.
	 *
	 * @param sModule the module
	 * @param projId the proj id
	 * @return the list
	 * @throws Exception the exception
	 */
	@RequestMapping(variantTypesListURL)
	/**
	 * This method returns the list of variant types from cache to supply the interface VariantSearch
	 *
	 * @param sModule
	 * @param projId
	 * @return
	 * @throws Exception
	 */
	protected @ResponseBody List<String> listVariantTypesSorted(@RequestParam("module") String sModule, @RequestParam("project") int projId) throws Exception
	{
		List<String> variantTypesArray = new ArrayList(getProjectVariantTypes(sModule, projId));
		Collections.sort(variantTypesArray, new AlphaNumericStringComparator());
		return variantTypesArray;
	}

	/**
	 * This method returns the list of sequences for filtering when searching variants.
	 *
	 * @param request the request
	 * @param sModule the module
	 * @param projId the proj id
	 * @return the list
	 * @throws Exception the exception
	 */
	@RequestMapping(sequenceListURL)
	protected @ResponseBody List<String> listSequences(HttpServletRequest request, @RequestParam("module") String sModule, @RequestParam("project") int projId) throws Exception
	{
		List<String> result = getProjectSequences(sModule, projId);

		List<String> externallySelectedSequences = getSequenceIDsBeingFilteredOn(request, sModule);	/* first try to use a list that may have been defined on in a different section of the application (although it may not be limited to the given project) */
		if (externallySelectedSequences != null)
			result = (List<String>) CollectionUtils.intersection(result, externallySelectedSequences);

		if (result != null)
			Collections.sort(result, new AlphaNumericStringComparator());
		return result;
	}

	/**
	 * List distinct allele counts.
	 *
	 * @param sModule the module
	 * @param projId the proj id
	 * @return the list
	 * @throws Exception the exception
	 */
	@RequestMapping(numberOfAlleleListURL)
	/**
	 * This method returns the list of number allele alternate from cache to supply the interface VariantSearch
	 *
	 * @param sModule
	 * @param projId
	 * @return
	 * @throws Exception
	 */
	protected @ResponseBody List<Integer> listDistinctAlleleCounts(@RequestParam("module") String sModule, @RequestParam("project") int projId) throws Exception
	{
		List<Integer> result = new ArrayList(getProjectDistinctAlleleCounts(sModule, projId));
		Collections.sort(result);
		return result;
	}

	/**
	 * Got gq field.
	 *
	 * @param sModule the module
	 * @param projId the proj id
	 * @return true, if successful
	 * @throws Exception the exception
	 */
	@RequestMapping(gotGQFieldURL)
	/**
	 * This method returns a the list of effect annotations found in this project
	 *
	 * @param sModule
	 * @param projId
	 * @return
	 * @throws Exception
	 */
	protected @ResponseBody boolean gotGQField(@RequestParam("module") String sModule, @RequestParam("project") int projId) throws Exception
	{
		return doesProjectContainGQField(sModule, projId);
	}

	/**
	 * Project effect annotation list.
	 *
	 * @param sModule the module
	 * @param projId the proj id
	 * @return the sets the
	 * @throws Exception the exception
	 */
	@RequestMapping(projectEffectAnnotationListURL)
	/**
	 * This method returns a the list of effect annotations found in this project
	 *
	 * @param sModule
	 * @param projId
	 * @return
	 * @throws Exception
	 */
	protected @ResponseBody Set<String> projectEffectAnnotationList(@RequestParam("module") String sModule, @RequestParam("project") int projId) throws Exception
	{
		return getProjectEffectAnnotations(sModule, projId);
	}

	/**
	 * Ploidy.
	 *
	 * @param sModule the module
	 * @param projId the proj id
	 * @return the integer
	 * @throws Exception the exception
	 */
	@RequestMapping(ploidyURL)
	/**
	 * This method returns the ploidy level of data contained in the given project
	 *
	 * @param sModule
	 * @param projId
	 * @return
	 * @throws Exception
	 */
	protected @ResponseBody Integer ploidy(@RequestParam("module") String sModule, @RequestParam("project") int projId) throws Exception
	{
		int ploidy = getProjectPloidyLevel(sModule, projId);
		return ploidy;
	}

	/**
	 * Need to filter on genotyping data.
	 *
	 * @param sModule the module
	 * @param projId the proj id
	 * @param operator the operator
	 * @param genotypeQualityThreshold the genotype quality threshold
	 * @param readDepthThreshold the read depth threshold
	 * @param missingData the missing data
	 * @param minmaf the minmaf
	 * @param maxmaf the maxmaf
	 * @param geneNames the gene names
	 * @param variantEffects the variant effects
	 * @return true, if successful
	 */
	private boolean needToFilterOnGenotypingData(String sModule, int projId, String operator, Integer genotypeQualityThreshold, Integer readDepthThreshold, Double missingData, Float minmaf, Float maxmaf, String geneNames, String variantEffects)
	{
		return operator != null || (genotypeQualityThreshold != null && genotypeQualityThreshold > 1) || (readDepthThreshold != null && readDepthThreshold > 1) || (missingData != null && missingData < 100) || (minmaf != null && minmaf > 0) || (maxmaf != null && maxmaf < 50) || geneNames.length() > 0 || variantEffects.length() > 0 || MongoTemplateManager.get(sModule).count(null, GenotypingProject.class) != 1;
	}

	/**
	 * Builds the variant data query.
	 *
	 * @param sModule the module
	 * @param projId the proj id
	 * @param selectedVariantTypes the selected variant types
	 * @param selectedSequences the selected sequences
	 * @param minposition the minposition
	 * @param maxposition the maxposition
	 * @param alleleCounts the allele counts
	 * @return the basic db list
	 * @throws Exception the exception
	 */
	private BasicDBList buildVariantDataQuery(String sModule, int projId, List<String> selectedVariantTypes, List<String> selectedSequences, Long minposition, Long maxposition, List<String> alleleCounts) throws Exception
	{
        BasicDBList variantFeatureFilterList = new BasicDBList();

		/* Step to match selected variant types */
		if (selectedVariantTypes != null && selectedVariantTypes.size() > 0) {
			BasicDBList orList1 = new BasicDBList();
			DBObject orSelectedVariantTypesList = new BasicDBObject();
			for (String aSelectedVariantTypes : selectedVariantTypes) {
				DBObject orClause1 = new BasicDBObject(VariantData.FIELDNAME_TYPE, aSelectedVariantTypes);
				orList1.add(orClause1);
				orSelectedVariantTypesList.put("$or",orList1);
			}
			variantFeatureFilterList.add(orSelectedVariantTypesList);
		}

		/* Step to match selected chromosomes */
        if (selectedSequences != null && selectedSequences.size() > 0 && selectedSequences.size() != getProjectSequences(sModule, projId).size())
            variantFeatureFilterList.add(new BasicDBObject(VariantData.FIELDNAME_REFERENCE_POSITION + "." + ReferencePosition.FIELDNAME_SEQUENCE, new BasicDBObject("$in", selectedSequences)));

		/* Step to match variants that have a position included in the specified range */
		if (minposition != null || maxposition != null) {
				if (minposition != null) {
					DBObject firstPosStart = new BasicDBObject(VariantData.FIELDNAME_REFERENCE_POSITION + "." + ReferencePosition.FIELDNAME_START_SITE, new BasicDBObject("$gte", minposition ));
					variantFeatureFilterList.add(firstPosStart);
				}
				if (maxposition != null) {
					DBObject lastPosStart = new BasicDBObject(VariantData.FIELDNAME_REFERENCE_POSITION + "." + ReferencePosition.FIELDNAME_START_SITE, new BasicDBObject("$lte", maxposition ));
					variantFeatureFilterList.add(lastPosStart);
				}
		}

		/* Step to match selected number of alleles */
		if (alleleCounts != null) {
				BasicDBList orList3 = new BasicDBList();
				DBObject orSelectedNumberOfAllelesList = new BasicDBObject();
				for (String aSelectedNumberOfAlleles : alleleCounts) {
					int alleleNumber = Integer.parseInt(aSelectedNumberOfAlleles);
					orList3.add(new BasicDBObject(VariantData.FIELDNAME_KNOWN_ALLELE_LIST, new BasicDBObject("$size", alleleNumber)));
					orSelectedNumberOfAllelesList.put("$or", orList3);
				}
				variantFeatureFilterList.add(orSelectedNumberOfAllelesList);
		}

		return variantFeatureFilterList;
	}


	/**
	 * This method returns the number of variants that match provided parameters.
	 *
	 * @param request the request
	 * @param sModule the module
	 * @param projId the proj id
	 * @param selectedVariantTypes the selected variant types
	 * @param selectedSequences the selected sequences
	 * @param selectedIndividuals the selected individuals
	 * @param gtCode the gt code
	 * @param genotypeQualityThreshold the genotype quality threshold
	 * @param readDepthThreshold the read depth threshold
	 * @param missingData the missing data
	 * @param minmaf the minmaf
	 * @param maxmaf the maxmaf
	 * @param minposition the minposition
	 * @param maxposition the maxposition
	 * @param alleleCount the allele count
	 * @param geneName the gene name
	 * @param variantEffects the variant effects
	 * @param processID the process id
	 * @return the long
	 * @throws Exception the exception
	 */
	@RequestMapping(variantCountURL)
	protected @ResponseBody long countVariants(HttpServletRequest request, @RequestParam("module") String sModule, @RequestParam("project") int projId, @RequestParam("variantTypes") String selectedVariantTypes, @RequestParam("sequences") String selectedSequences, @RequestParam("individuals") String selectedIndividuals, @RequestParam("gtCode") String gtCode, @RequestParam("genotypeQualityThreshold") Integer genotypeQualityThreshold, @RequestParam("readDepthThreshold") Integer readDepthThreshold, @RequestParam("missingData") Double missingData, @RequestParam(value="minmaf", required=false) Float minmaf, @RequestParam(value="maxmaf", required=false) Float maxmaf, @RequestParam("minposition") Long minposition, @RequestParam("maxposition") Long maxposition, @RequestParam("alleleCount") String alleleCount, @RequestParam("geneName") String geneName, @RequestParam("variantEffects") String variantEffects, @RequestParam("processID") final String processID) throws Exception
	{
		final ProgressIndicator progress = new ProgressIndicator(processID.substring(1 + processID.indexOf('|')), new String[0]);
		ProgressIndicator.registerProgressIndicator(progress);

		DBCollection tmpVarColl = getTemporaryVariantCollection(sModule, progress.getProcessId(), true /*empty it*/);
		try
		{
			String queryKey = getQueryKey(request, sModule, projId, selectedVariantTypes, selectedSequences, selectedIndividuals, gtCode, genotypeQualityThreshold, readDepthThreshold, missingData, minmaf, maxmaf, minposition, maxposition, alleleCount, geneName, variantEffects);

			final MongoTemplate mongoTemplate = MongoTemplateManager.get(sModule);
			DBCollection cachedCountcollection = mongoTemplate.getCollection(MgdbDao.COLLECTION_NAME_CACHED_COUNTS);
//			cachedCountcollection.drop();
			DBCursor countCursor = cachedCountcollection.find(new BasicDBObject("_id", queryKey));
			Long count = null;
			if (countCursor.hasNext())
			{
				count = 0l;
				for (Object aPartialCount : ((BasicDBList) countCursor.next().get(MgdbDao.FIELD_NAME_CACHED_COUNT_VALUE)).toArray())
					count += (Long) aPartialCount;
			}
			LOG.debug((count == null ? "new" : "existing") + " queryKey hash: " + queryKey);
			if (count == null)
			{
				long before = System.currentTimeMillis();

				progress.addStep("Counting matching variants");
				String sRegexOrAggregationOperator = genotypeCodeToQueryMap.get(gtCode);

				List<String> alleleCountList = alleleCount.length() == 0 ? null : Arrays.asList(alleleCount.split(";"));

				GenotypingProject genotypingProject = mongoTemplate.findById(projId, GenotypingProject.class);
				if (genotypingProject.getAlleleCounts().size() != 1 || genotypingProject.getAlleleCounts().iterator().next() != 2)
				{	// Project does not only have bi-allelic data: make sure we can apply MAF filter on selection
					boolean fExactlyOneNumberOfAllelesSelected = alleleCountList != null && alleleCountList.size() == 1;
					boolean fBiAllelicSelected = fExactlyOneNumberOfAllelesSelected && "2".equals(alleleCountList.get(0));
					boolean fMafRequested = (maxmaf != null && maxmaf < 50) || (minmaf != null && minmaf > 0);
					if (fMafRequested && !fBiAllelicSelected)
					{
						progress.setError("MAF is only supported on biallelic data!");
						return 0l;
					}
				}

				String actualSequenceSelection = selectedSequences;
				if (actualSequenceSelection.length() == 0)
				{
					ArrayList<String> externallySelectedSeqs = getSequenceIDsBeingFilteredOn(request, sModule);
					if (externallySelectedSeqs != null)
						actualSequenceSelection = StringUtils.join(externallySelectedSeqs, ";");
				}

		    	boolean fNeedToFilterOnGenotypingData = needToFilterOnGenotypingData(sModule, projId, sRegexOrAggregationOperator, genotypeQualityThreshold, readDepthThreshold, missingData, minmaf, maxmaf, geneName, variantEffects);

				BasicDBList variantQueryDBList = buildVariantDataQuery(sModule, projId, selectedVariantTypes.length() == 0 ? null : Arrays.asList(selectedVariantTypes.split(";")), actualSequenceSelection.length() == 0 ? null : Arrays.asList(actualSequenceSelection.split(";")), minposition, maxposition, alleleCountList);
				if (variantQueryDBList.isEmpty())
				{
					if (!fNeedToFilterOnGenotypingData && mongoTemplate.count(null, GenotypingProject.class) == 1)
						count = mongoTemplate.count(new Query(), VariantData.class);	// no filter whatsoever
				}
				else
				{
					if (!fNeedToFilterOnGenotypingData)
					{	// filtering on variant features only: we just need a count
						count = mongoTemplate.getCollection(mongoTemplate.getCollectionName(VariantData.class)).count(new BasicDBObject("$and", variantQueryDBList));
					}
					else
					{	// filtering on variant features and genotyping data: we need a list of variant IDs to restrict the genotyping data search to
						long beforeAggQuery = System.currentTimeMillis();
						progress.setProgressDescription("Filtering variants for count...");

						DBCollection variantColl = mongoTemplate.getCollection(mongoTemplate.getCollectionName(VariantData.class));
						List<DBObject> pipeline = new ArrayList<DBObject>();
						pipeline.add(new BasicDBObject("$match", new BasicDBObject("$and", variantQueryDBList)));
						BasicDBObject projectObject = new BasicDBObject("_id", "$_id");
						projectObject.put(VariantData.FIELDNAME_REFERENCE_POSITION + "." + ReferencePosition.FIELDNAME_SEQUENCE, "$" + VariantData.FIELDNAME_REFERENCE_POSITION + "." + ReferencePosition.FIELDNAME_SEQUENCE);
						projectObject.put(VariantData.FIELDNAME_REFERENCE_POSITION + "." + ReferencePosition.FIELDNAME_START_SITE, "$" + VariantData.FIELDNAME_REFERENCE_POSITION + "." + ReferencePosition.FIELDNAME_START_SITE);
						projectObject.put(VariantData.FIELDNAME_TYPE, "$" + VariantData.FIELDNAME_TYPE);
						projectObject.put(VariantData.FIELDNAME_KNOWN_ALLELE_LIST, "$" + VariantData.FIELDNAME_KNOWN_ALLELE_LIST);
						pipeline.add(new BasicDBObject("$project", projectObject));
						pipeline.add(new BasicDBObject("$out", tmpVarColl.getName()));

						variantColl.aggregate(pipeline);
						LOG.debug("Variant preliminary query found " + tmpVarColl.count() + " results in " + (System.currentTimeMillis() - beforeAggQuery)/1000f + "s");

						progress.setProgressDescription(null);
						if (tmpVarColl.count() == 0)
							count = 0l;	// no need to search any further
					}
				}

				if (count != null)
				{
					BasicDBObject dbo = new BasicDBObject("_id", queryKey);
					dbo.append(MgdbDao.FIELD_NAME_CACHED_COUNT_VALUE, new Long[] {count});
					cachedCountcollection.save(dbo);
				}
				else
				{	// now filter on genotyping data
					List<String> selectedIndividualList = selectedIndividuals.length() == 0 ? null : Arrays.asList(selectedIndividuals.split(";"));
			        if (selectedIndividualList == null)
			        	selectedIndividualList = getIndividualsInDbOrder(sModule, projId);

			        GenotypingDataQueryBuilder genotypingDataQueryBuilder = new GenotypingDataQueryBuilder(sModule, projId, tmpVarColl, sRegexOrAggregationOperator, genotypeQualityThreshold, readDepthThreshold, missingData, minmaf, maxmaf, geneName, variantEffects, selectedIndividualList, getProjectEffectAnnotations(sModule, projId), new ArrayList<String>());
			        try
			        {
						final int nChunkCount = genotypingDataQueryBuilder.getNumberOfQueries();
						if (nChunkCount > 1)
							LOG.debug("Query split into " + nChunkCount);

						final Long[] partialCountArray = new Long[nChunkCount];
						final Builder aggOpts = AggregationOptions.builder().allowDiskUse(true);
						final ArrayList<Thread> threadsToWaitFor = new ArrayList<Thread>();
						final AtomicInteger finishedThreadCount = new AtomicInteger(0);

						for (int i=0; i<nChunkCount; i++)
						{
							final List<DBObject> genotypingDataPipeline = genotypingDataQueryBuilder.next();

							// Now the $group operation, used for counting
							DBObject groupFields = new BasicDBObject("_id", null);
							groupFields.put("count", new BasicDBObject("$sum", 1));
							genotypingDataPipeline.add(new BasicDBObject("$group", groupFields));

							if (i == 0 && tmpVarColl.count() <= 5)
								LOG.debug(genotypingDataPipeline);

							if (progress.hasAborted())
							{
								genotypingDataQueryBuilder.cleanup();	// otherwise a pending db-cursor will remain
								return 0l;
							}

							final int chunkIndex = i;

			                Thread t = new Thread() {
			                	public void run() {
									Cursor it = mongoTemplate.getCollection(MongoTemplateManager.getMongoCollectionName(VariantRunData.class)).aggregate(genotypingDataPipeline, aggOpts.build());
									partialCountArray[chunkIndex] = it.hasNext() ? ((Number) it.next().get("count")).longValue() : 0;
									progress.setCurrentStepProgress((short) (finishedThreadCount.incrementAndGet()*100/nChunkCount));
									genotypingDataPipeline.clear();	// release memory (VERY IMPORTANT)
			                	}
			                };

			                if (chunkIndex%NUMBER_OF_SIMULTANEOUS_QUERY_THREADS  == (NUMBER_OF_SIMULTANEOUS_QUERY_THREADS-1))
				            	t.run();	// run synchronously
				            else
			                {
				            	threadsToWaitFor.add(t);
				            	t.start();	// run asynchronously for better speed
			                }
						}

						for (Thread t : threadsToWaitFor)	// wait for all threads before moving to next phase
							t.join();

						progress.setCurrentStepProgress(100);

						count = 0l;
						for (Long partialCount : partialCountArray)
							count += partialCount;

						BasicDBObject dbo = new BasicDBObject("_id", queryKey);
						dbo.append(MgdbDao.FIELD_NAME_CACHED_COUNT_VALUE, partialCountArray);
						cachedCountcollection.save(dbo);
			        }
			        catch (Exception e)
			        {
			        	genotypingDataQueryBuilder.cleanup();	// otherwise a pending db-cursor will remain
			        	throw e;
			        }
				}
				LOG.info("countVariants found " + count + " results in " + (System.currentTimeMillis() - before)/1000d + "s");
			}

			progress.markAsComplete();
			if (progress.hasAborted())
				return 0l;

			return count;
		}
		finally
		{
//			getTemporaryVariantCollection(sModule, progress.getProcessId(), true);	// always empty it
		}
	}

	 /**
 	 * Mem usage.
 	 *
 	 * @return the string
 	 */
 	public static String memUsage()
	 {
		 long maxMem = Runtime.getRuntime().maxMemory()/(1024*1024);
		 long freeMem = Runtime.getRuntime().freeMemory()/(1024*1024);
		 long totalMem = Runtime.getRuntime().totalMemory()/(1024*1024);
		 return "total: " + totalMem + ", free: " + freeMem + ", max: " + maxMem;
	 }

	/**
	 * Find variants.
	 *
	 * @param request the request
	 * @param sModule the module
	 * @param projId the proj id
	 * @param selectedVariantTypes the selected variant types
	 * @param selectedSequences the selected sequences
	 * @param selectedIndividuals the selected individuals
	 * @param gtCode the gt code
	 * @param genotypeQualityThreshold the genotype quality threshold
	 * @param readDepthThreshold the read depth threshold
	 * @param missingData the missing data
	 * @param minmaf the minmaf
	 * @param maxmaf the maxmaf
	 * @param minposition the minposition
	 * @param maxposition the maxposition
	 * @param alleleCount the allele count
	 * @param geneName the gene name
	 * @param variantEffects the variant effects
	 * @param wantedFields the wanted fields
	 * @param page the page
	 * @param size the size
	 * @param sortBy the sort by
	 * @param sortDir the sort dir
	 * @param processID the process id
	 * @return true, if successful
	 * @throws Exception the exception
	 */
	@RequestMapping(variantFindURL)
	/**
	 *  This method build a list of variants in a temporary collection, that may be used later for browsing or exporting results
	 */
	protected @ResponseBody boolean findVariants(HttpServletRequest request, @RequestParam("module") String sModule, @RequestParam("project") int projId, @RequestParam("variantTypes") String selectedVariantTypes, @RequestParam("sequences") String selectedSequences, @RequestParam("individuals") String selectedIndividuals, @RequestParam("gtCode") String gtCode, @RequestParam("genotypeQualityThreshold") int genotypeQualityThreshold, @RequestParam("readDepthThreshold") int readDepthThreshold, @RequestParam("missingData") double missingData, @RequestParam("minmaf") Float minmaf, @RequestParam("maxmaf") Float maxmaf, @RequestParam("minposition") Long minposition, @RequestParam("maxposition") Long maxposition,@RequestParam("alleleCount") String alleleCount, @RequestParam("geneName") String geneName, @RequestParam("variantEffects") String variantEffects, @RequestParam("wantedFields") String wantedFields, @RequestParam("page") int page, @RequestParam("size") int size, @RequestParam("sortBy") String sortBy, @RequestParam("sortDir") String sortDir, @RequestParam("processID") String processID) throws Exception
	{
		long before = System.currentTimeMillis();

		String sShortProcessID = processID.substring(1 + processID.indexOf('|'));

		final ProgressIndicator progress = new ProgressIndicator(sShortProcessID, new String[0]);
		ProgressIndicator.registerProgressIndicator(progress);
		progress.addStep("Loading results");

		String actualSequenceSelection = selectedSequences;
		if (actualSequenceSelection.length() == 0)
		{
			ArrayList<String> externallySelectedSeqs = getSequenceIDsBeingFilteredOn(request, sModule);
			if (externallySelectedSeqs != null)
				actualSequenceSelection = StringUtils.join(externallySelectedSeqs, ";");
		}

		List<String> selectedSequenceList = actualSequenceSelection.length() == 0 ? null : Arrays.asList(actualSequenceSelection.split(";"));
		String queryKey = getQueryKey(request, sModule, projId, selectedVariantTypes, selectedSequences, selectedIndividuals, gtCode, genotypeQualityThreshold, readDepthThreshold, missingData, minmaf, maxmaf, minposition, maxposition, alleleCount, geneName, variantEffects);

		final MongoTemplate mongoTemplate = MongoTemplateManager.get(sModule);
		DBCollection cachedCountCollection = mongoTemplate.getCollection(MgdbDao.COLLECTION_NAME_CACHED_COUNTS);
		DBCursor countCursor = cachedCountCollection.find(new BasicDBObject("_id", queryKey));

		final DBCollection variantColl = mongoTemplate.getCollection(mongoTemplate.getCollectionName(VariantData.class));
		final Object[] partialCountArray = !countCursor.hasNext() ? null : ((BasicDBList) countCursor.next().get(MgdbDao.FIELD_NAME_CACHED_COUNT_VALUE)).toArray();

		final DBCollection tmpVarColl = getTemporaryVariantCollection(sModule, progress.getProcessId(), false);

		String sRegexOrAggregationOperator = genotypeCodeToQueryMap.get(gtCode);
    	boolean fNeedToFilterOnGenotypingData = needToFilterOnGenotypingData(sModule, projId, sRegexOrAggregationOperator, genotypeQualityThreshold, readDepthThreshold, missingData, minmaf, maxmaf, geneName, variantEffects);
		final BasicDBList variantQueryDBList = buildVariantDataQuery(sModule, projId, selectedVariantTypes.length() == 0 ? null : Arrays.asList(selectedVariantTypes.split(";")), selectedSequenceList, minposition, maxposition, alleleCount.length() == 0 ? null : Arrays.asList(alleleCount.split(";")));

		if (!variantQueryDBList.isEmpty() && tmpVarColl.count() == 0 /* otherwise we kept the preliminary list from the count procedure */)
		{	// apply filter on variant features
			progress.setProgressDescription("Filtering variants for display...");
			long beforeAggQuery = System.currentTimeMillis();
			List<DBObject> pipeline = new ArrayList<DBObject>();
			pipeline.add(new BasicDBObject("$match", new BasicDBObject("$and", variantQueryDBList)));
			BasicDBObject projectObject = new BasicDBObject("_id", "$_id");
			projectObject.put(VariantData.FIELDNAME_REFERENCE_POSITION + "." + ReferencePosition.FIELDNAME_SEQUENCE, "$" + VariantData.FIELDNAME_REFERENCE_POSITION + "." + ReferencePosition.FIELDNAME_SEQUENCE);
			projectObject.put(VariantData.FIELDNAME_REFERENCE_POSITION + "." + ReferencePosition.FIELDNAME_START_SITE, "$" + VariantData.FIELDNAME_REFERENCE_POSITION + "." + ReferencePosition.FIELDNAME_START_SITE);
			projectObject.put(VariantData.FIELDNAME_REFERENCE_POSITION + "." + ReferencePosition.FIELDNAME_END_SITE, "$" + VariantData.FIELDNAME_REFERENCE_POSITION + "." + ReferencePosition.FIELDNAME_END_SITE);
			projectObject.put(VariantData.FIELDNAME_TYPE, "$" + VariantData.FIELDNAME_TYPE);
			projectObject.put(VariantData.FIELDNAME_KNOWN_ALLELE_LIST, "$" + VariantData.FIELDNAME_KNOWN_ALLELE_LIST);
			pipeline.add(new BasicDBObject("$project", projectObject));
			pipeline.add(new BasicDBObject("$out", tmpVarColl.getName()));

			variantColl.aggregate(pipeline);
			LOG.debug("Variant preliminary query found " + tmpVarColl.count() + " results in " + (System.currentTimeMillis() - beforeAggQuery)/1000f + "s");

			progress.setProgressDescription(null);
		}
		else if (fNeedToFilterOnGenotypingData)
				LOG.debug("Re-using " + tmpVarColl.count() + " results from count procedure's variant preliminary query");

		if (progress.hasAborted())
			return false;

		if (fNeedToFilterOnGenotypingData)
		{	// now filter on genotyping data
			final ConcurrentLinkedQueue<Thread> threadsToWaitFor = new ConcurrentLinkedQueue<Thread>();
			final AtomicInteger finishedThreadCount = new AtomicInteger(0);
			final ConcurrentSkipListSet<Comparable> allVariantsThatPassRunFilter = new ConcurrentSkipListSet<Comparable>();
			final GenotypingDataQueryBuilder genotypingDataQueryBuilder = new GenotypingDataQueryBuilder(sModule, projId, tmpVarColl, sRegexOrAggregationOperator, genotypeQualityThreshold, readDepthThreshold, missingData, minmaf, maxmaf, geneName, variantEffects, selectedIndividuals == null || selectedIndividuals.length() == 0 ? getIndividualsInDbOrder(sModule, projId) : Arrays.asList(selectedIndividuals.split(";")), getProjectEffectAnnotations(sModule, projId), new ArrayList<String>());
	        try
	        {
				final int nChunkCount = genotypingDataQueryBuilder.getNumberOfQueries();
				if (nChunkCount != partialCountArray.length)
				{
		        	LOG.error("Different number of chunks between counting and listing variant rows!");
		        	progress.setError("Different number of chunks between counting and listing variant rows!");
		        	return false;
				}
				if (nChunkCount > 1)
					LOG.debug("Query split into " + nChunkCount);

				int i = -1;
				while (genotypingDataQueryBuilder.hasNext())
				{
					final List<DBObject> genotypingDataPipeline = genotypingDataQueryBuilder.next();
					if (progress.hasAborted())
					{
						genotypingDataQueryBuilder.cleanup();	// otherwise a pending db-cursor will remain
						return false;
					}

					final int chunkIndex = ++i;
					if (partialCountArray != null && (Long) partialCountArray[chunkIndex] == 0)
						continue;

					Thread t = new Thread() {
						public void run()
						{
							Cursor genotypingDataCursor = mongoTemplate.getCollection(MongoTemplateManager.getMongoCollectionName(VariantRunData.class)).aggregate(genotypingDataPipeline, AggregationOptions.builder().allowDiskUse(true).build());
							final ArrayList<Comparable> variantsThatPassedRunFilter = new ArrayList<Comparable>();
							while (genotypingDataCursor.hasNext())
								variantsThatPassedRunFilter.add((Comparable) genotypingDataCursor.next().get("_id"));

							if (variantQueryDBList.isEmpty())	// otherwise we won't need it
								allVariantsThatPassRunFilter.addAll(variantsThatPassedRunFilter);
							else
							{	// mark the results we want to keep
//								long beforeTempCollUpdate = System.currentTimeMillis();
								WriteResult wr = mongoTemplate.updateMulti(new Query(Criteria.where("_id").in(variantsThatPassedRunFilter)), new Update().set(VariantData.FIELDNAME_VERSION, 0), tmpVarColl.getName());
//								LOG.debug("Chunk N." + (chunkIndex+1) + "/" + genotypingDataQueryBuilder.getNumberOfQueries() + ": " + wr.getN() + " temp records marked in " + (System.currentTimeMillis() - beforeTempCollUpdate)/1000d + "s");
							}
							progress.setCurrentStepProgress((short) (finishedThreadCount.incrementAndGet()*100/nChunkCount));
							genotypingDataPipeline.clear();	// release memory (VERY IMPORTANT)
						}
					};

	                if (chunkIndex%NUMBER_OF_SIMULTANEOUS_QUERY_THREADS == (NUMBER_OF_SIMULTANEOUS_QUERY_THREADS-1))
		            	t.run();	// run synchronously
		            else
	                {
		            	threadsToWaitFor.add(t);
		            	t.start();	// run asynchronously for better speed
	                }
				}

				for (Thread t : threadsToWaitFor)	// wait for all threads before moving to next phase
					t.join();
	        }
	        catch (Exception e)
	        {
	        	genotypingDataQueryBuilder.cleanup();	// otherwise a pending db-cursor will remain
	        	throw e;
	        }

			if (progress.hasAborted())
				return false;

			progress.addStep("Updating temporary results");
			progress.moveToNextStep();
			final long beforeTempCollUpdate = System.currentTimeMillis();
			if (variantQueryDBList.isEmpty())
			{	// we filtered on runs only: keep track of the final dataset
				List<DBObject> pipeline = new ArrayList<DBObject>();
				pipeline.add(new BasicDBObject("$match", new BasicDBObject("_id", new BasicDBObject("$in", allVariantsThatPassRunFilter))));
				BasicDBObject projectObject = new BasicDBObject("_id", "$_id");
				projectObject.put(VariantData.FIELDNAME_REFERENCE_POSITION + "." + ReferencePosition.FIELDNAME_SEQUENCE, "$" + VariantData.FIELDNAME_REFERENCE_POSITION + "." + ReferencePosition.FIELDNAME_SEQUENCE);
				projectObject.put(VariantData.FIELDNAME_REFERENCE_POSITION + "." + ReferencePosition.FIELDNAME_START_SITE, "$" + VariantData.FIELDNAME_REFERENCE_POSITION + "." + ReferencePosition.FIELDNAME_START_SITE);
				projectObject.put(VariantData.FIELDNAME_TYPE, "$" + VariantData.FIELDNAME_TYPE);
				projectObject.put(VariantData.FIELDNAME_KNOWN_ALLELE_LIST, "$" + VariantData.FIELDNAME_KNOWN_ALLELE_LIST);
				projectObject.put(VariantData.FIELDNAME_VERSION, "$" + VariantData.FIELDNAME_VERSION);
				pipeline.add(new BasicDBObject("$project", projectObject));
				pipeline.add(new BasicDBObject("$out", tmpVarColl.getName()));
				variantColl.aggregate(pipeline);
				LOG.debug(tmpVarColl.count() + " temp records created in " + (System.currentTimeMillis() - beforeTempCollUpdate)/1000d + "s");
			}
			else
				new Thread() {
				public void run()
				{	// remove records that were not marked for keeping
					WriteResult wr = tmpVarColl.remove(new BasicDBObject(VariantData.FIELDNAME_VERSION, new BasicDBObject("$exists", false)));
					LOG.debug(wr.getN() + " filtered-out temp records removed in " + (System.currentTimeMillis() - beforeTempCollUpdate)/1000d + "s");
				}
			}.start();
		}

		progress.markAsComplete();
		LOG.info("findVariants took " + (System.currentTimeMillis() - before)/1000d + "s");
		return true;
	}

	/**
	 * List variants.
	 *
	 * @param request the request
	 * @param sModule the module
	 * @param projId the proj id
	 * @param selectedVariantTypes the selected variant types
	 * @param selectedSequences the selected sequences
	 * @param selectedIndividuals the selected individuals
	 * @param gtCode the gt code
	 * @param genotypeQualityThreshold the genotype quality threshold
	 * @param readDepthThreshold the read depth threshold
	 * @param missingData the missing data
	 * @param minmaf the minmaf
	 * @param maxmaf the maxmaf
	 * @param minposition the minposition
	 * @param maxposition the maxposition
	 * @param alleleCount the allele count
	 * @param geneName the gene name
	 * @param variantEffects the variant effects
	 * @param wantedFields the wanted fields
	 * @param page the page
	 * @param size the size
	 * @param sortBy the sort by
	 * @param sortDir the sort dir
	 * @param processID the process id
	 * @return the array list
	 * @throws Exception the exception
	 */
	@RequestMapping(variantListURL)
	/**
	 *  This method returns a list of variants from the current selection
	 */
	protected @ResponseBody ArrayList<Comparable[]> listVariants(HttpServletRequest request, @RequestParam("module") String sModule, @RequestParam("project") int projId, @RequestParam("variantTypes") String selectedVariantTypes, @RequestParam("sequences") String selectedSequences, @RequestParam("individuals") String selectedIndividuals, @RequestParam("gtCode") String gtCode, @RequestParam("genotypeQualityThreshold") int genotypeQualityThreshold, @RequestParam("readDepthThreshold") int readDepthThreshold, @RequestParam("missingData") double missingData, @RequestParam("minmaf") Float minmaf, @RequestParam("maxmaf") Float maxmaf, @RequestParam("minposition") Long minposition, @RequestParam("maxposition") Long maxposition,@RequestParam("alleleCount") String alleleCount, @RequestParam("geneName") String geneName, @RequestParam("variantEffects") String variantEffects, @RequestParam("wantedFields") String wantedFields, @RequestParam("page") int page, @RequestParam("size") int size, @RequestParam("sortBy") String sortBy, @RequestParam("sortDir") String sortDir, @RequestParam("processID") String processID) throws Exception
	{
		String[] usedFields = wantedFields.split(";");

		String sShortProcessID = processID.substring(1 + processID.indexOf('|'));
		String queryKey = getQueryKey(request, sModule, projId, selectedVariantTypes, selectedSequences, selectedIndividuals, gtCode, genotypeQualityThreshold, readDepthThreshold, missingData, minmaf, maxmaf, minposition, maxposition, alleleCount, geneName, variantEffects);
		MongoTemplate mongoTemplate = MongoTemplateManager.get(sModule);
		DBCollection cachedCountcollection = mongoTemplate.getCollection(MgdbDao.COLLECTION_NAME_CACHED_COUNTS);
//		cachedCountcollection.drop();
		DBCursor countCursor = cachedCountcollection.find(new BasicDBObject("_id", queryKey));
		Object[] partialCountArray = !countCursor.hasNext() ? null : ((BasicDBList) countCursor.next().get(MgdbDao.FIELD_NAME_CACHED_COUNT_VALUE)).toArray();

        HashMap<Integer, String> variantFieldMap = new HashMap<Integer, String>(), runDataFieldMap = new HashMap<Integer, String>();
		for (int i=0; i<usedFields.length; i++)
			if (usedFields[i].startsWith("#"))
				variantFieldMap.put(i, usedFields[i].substring(1));
			else
				runDataFieldMap.put(i, usedFields[i]);

		long expectedCount = 0;
		for (Object aPartialCount : partialCountArray)
			expectedCount += (Long) aPartialCount;
		DBCollection tempVarColl = getTemporaryVariantCollection(sModule, sShortProcessID, false);
		long nTempVarCount = tempVarColl.count();

		ArrayList<Comparable[]> result = new ArrayList<Comparable[]>();
		DBCollection variantColl = mongoTemplate.getCollection(mongoTemplate.getCollectionName(VariantData.class));
		if (nTempVarCount > 0 || expectedCount == variantColl.count())	// otherwise we return an empty list because there seems to be a problem (missing temp records)
		{
			boolean fProjectHasAnnotations = getProjectEffectAnnotations(sModule, projId).size() > 0;

			DBCollection varCollForBuildingRows = nTempVarCount == 0 ? variantColl : tempVarColl;
			DBCursor variantsInFilterCursor = varCollForBuildingRows.find(expectedCount == nTempVarCount ? null : new BasicDBObject(VariantData.FIELDNAME_VERSION, new BasicDBObject("$exists", true)));

			ArrayList<Object[]> variantRows = buildVariantRows(mongoTemplate, variantsInFilterCursor, sortBy, sortDir, page, size, variantFieldMap, runDataFieldMap);
			for (Object[] aRow : variantRows)
			{
				List<Comparable> anOutputRow = new ArrayList<Comparable>();
				for (int i=0; i<aRow.length; i++)
				{
					String val = null;
					if (!usedFields[i].startsWith(VariantRunData.SECTION_ADDITIONAL_INFO + "."))
						val = aRow[i] == null ? "" : aRow[i].toString();
					else
						if (aRow[i] != null && fProjectHasAnnotations)
							val = aRow[i].toString().replaceAll("[\\[\\]\"]", "");	// it's an annotation field: make its content look cleaner
					if (val != null)
						anOutputRow.add(val);
				}
				anOutputRow.add(anOutputRow.get(0));	// for details link
				result.add(anOutputRow.toArray(new Comparable[anOutputRow.size()]));
			}
		}
		return result;
	}

	/**
	 * Builds the variant rows.
	 *
	 * @param mongoTemplate the mongo template
	 * @param variantsToBuildRowsFor the variants to build rows for
	 * @param sortBy the sort by
	 * @param sortDir the sort dir
	 * @param page the page
	 * @param size the size
	 * @param variantFieldMap the variant field map
	 * @param runDataFieldMap the run data field map
	 * @return the array list
	 */
	private ArrayList<Object[]> buildVariantRows(MongoTemplate mongoTemplate, DBCursor variantsToBuildRowsFor, String sortBy, String sortDir, int page, int size, HashMap<Integer, String> variantFieldMap, Map<Integer, String> runDataFieldMap)
	{
        if (sortBy != null && sortBy.length() > 0)
        {
        	String cleanSortField = sortBy.replaceFirst("%23", "");
        	variantsToBuildRowsFor.sort(new BasicDBObject(cleanSortField, Integer.valueOf("DESC".equalsIgnoreCase(sortDir) ? -1 : 1)));
        }
        variantsToBuildRowsFor.skip(page*size).limit(size);

		ArrayList<Object[]> variantRows = new ArrayList<Object[]>();
		HashMap<Comparable, Object[]> variantIdToRowMap = new HashMap<Comparable, Object[]>();

		Collection<Comparable> currentVariants = new ArrayList<Comparable>();
		while (variantsToBuildRowsFor.hasNext())
		{
			DBObject record = variantsToBuildRowsFor.next();
			Object[] aRow = new Object[variantFieldMap.size() + runDataFieldMap.size()];
			for (int i : variantFieldMap.keySet())
				aRow[i] = Helper.readPossiblyNestedField(record, variantFieldMap.get(i));
			variantRows.add(aRow);
			variantIdToRowMap.put((Comparable) aRow[0], aRow);
			currentVariants.add((Comparable) aRow[0]);
		}

		if (!runDataFieldMap.isEmpty())
		{ // Query on VariantRunData so we can fill run-related fields
			ArrayList<DBObject> genotypingDataAggregationParams2 = new ArrayList<DBObject>();
			genotypingDataAggregationParams2.add(new BasicDBObject("$match", new BasicDBObject("_id." + VariantRunDataId.FIELDNAME_VARIANT_ID, new BasicDBObject("$in", currentVariants))));
			DBObject project = new BasicDBObject();
			for (String sField : runDataFieldMap.values())
				project.put(sField.replaceAll("\\.", "¤"), "$" + sField);
			genotypingDataAggregationParams2.add(new BasicDBObject("$project", project));

//			long beforeQuery = System.currentTimeMillis();
			Cursor genotypingDataCursor = mongoTemplate.getCollection(MongoTemplateManager.getMongoCollectionName(VariantRunData.class)).aggregate(genotypingDataAggregationParams2, AggregationOptions.builder().allowDiskUse(true).build());
			while (genotypingDataCursor.hasNext())
			{
				DBObject record = genotypingDataCursor.next();
				Object[] aRow = variantIdToRowMap.get(Helper.readPossiblyNestedField(record, "_id." + VariantRunDataId.FIELDNAME_VARIANT_ID));
				for (int fieldIndex : runDataFieldMap.keySet())
					aRow[fieldIndex] = record.get(runDataFieldMap.get(fieldIndex).replaceAll("\\.", "¤"));
			}
//			LOG.debug("Genotyping data main query treated in " + (System.currentTimeMillis() - beforeQuery) + "ms");
		}
		return variantRows;
	}

	/**
	 * Setup detail page.
	 *
	 * @param sModule the module
	 * @param projectId the proj id
	 * @param variantId the variant id
	 * @param selectedIndividuals the selected individuals
	 * @return the model and view
	 * @throws Exception the exception
	 */
	@RequestMapping(value = variantDetailsURL, method = RequestMethod.GET)
	protected ModelAndView setupDetailPage(@RequestParam("module") String sModule, @RequestParam("project") int projectId, @RequestParam("variantId") String variantId, @RequestParam("individuals") String selectedIndividuals) throws Exception
	{
		ModelAndView mav = new ModelAndView();
		MongoTemplate mongoTemplate = MongoTemplateManager.get(sModule);

		GenotypingProject project = mongoTemplate.findById(projectId, GenotypingProject.class);
		mav.addObject("project", project);

		List<String> selectedIndividualList = Arrays.asList(selectedIndividuals.split(";"));
		HashMap<Integer, String> sampleToIndividualMap = new LinkedHashMap<Integer, String>();
		HashMap<String, Boolean> individualMap = new LinkedHashMap<String, Boolean>();
		for (String ind : listIndividualsInAlphaNumericOrder(sModule, projectId))
		{
			for (Integer sampleIndex : project.getIndividualSampleIndexes(ind))
				sampleToIndividualMap.put(sampleIndex, ind);
			individualMap.put(ind, selectedIndividuals.length() == 0 || selectedIndividualList.contains(ind));
		}
		mav.addObject("individualMap", individualMap);

		HashMap<Integer, List<String>> sampleIDsByProject = new HashMap<Integer, List<String>>();
		sampleIDsByProject.put(projectId, selectedIndividualList);
		VariantData var = mongoTemplate.findById(variantId, VariantData.class);
		mav.addObject("variantType", var.getType());
		mav.addObject("refPos", var.getReferencePosition());

		Map<String /* run */, Map<String /* individual */, List<Comparable /* cell value */>>> dataByRun = new TreeMap<String, Map<String, List<Comparable>>>(new AlphaNumericStringComparator());
		Map<String /* run */, Map<String /* info field */, Object>> additionalInfoByRun = new TreeMap<String, Map<String, Object>>(new AlphaNumericStringComparator());
		Map<String /* run */, Map<String /* info field */, VCFInfoHeaderLine>> additionalInfoDescByRun = new HashMap<String, Map<String, VCFInfoHeaderLine>>();
		List<String> headerCols = new ArrayList<String>();
		List<String> headerColDescs = new ArrayList<String>();
		List<Criteria> crits = new ArrayList<Criteria>();
		crits.add(Criteria.where("_id." + VariantRunDataId.FIELDNAME_PROJECT_ID).is(projectId));
		crits.add(Criteria.where("_id." + VariantRunDataId.FIELDNAME_VARIANT_ID).is(var.getId()));
		List<VariantRunData> runs = mongoTemplate.find(new Query(new Criteria().andOperator(crits.toArray(new Criteria[crits.size()]))), VariantRunData.class);
		for (VariantRunData run : runs)
		{
			DBVCFHeader vcfHeader = null;
			BasicDBList andList = new BasicDBList();
			andList.add(new BasicDBObject("_id." + VcfHeaderId.FIELDNAME_PROJECT, projectId));
			andList.add(new BasicDBObject("_id." + VcfHeaderId.FIELDNAME_RUN, run.getRunName()));
			DBCursor headerCursor = mongoTemplate.getCollection(MongoTemplateManager.getMongoCollectionName(DBVCFHeader.class)).find(new BasicDBObject("$and", andList));
			if (headerCursor.size() > 0 && headerCols.isEmpty())
			{
				vcfHeader = DBVCFHeader.fromDBObject(headerCursor.next());
				headerCursor.close();
			}
			Map<String /* individual */, List<Comparable /* cell value */>> genotypeRows = new TreeMap<String, List<Comparable>>(new AlphaNumericStringComparator());

			additionalInfoByRun.put(run.getRunName(), run.getAdditionalInfo());
			if (vcfHeader != null)
				additionalInfoDescByRun.put(run.getRunName(), vcfHeader.getmInfoMetaData());

			dataByRun.put(run.getRunName(), genotypeRows);
			for (Integer sample : run.getSampleGenotypes().keySet()) {
				SampleGenotype sg = run.getSampleGenotypes().get(sample);
				List<Comparable> genotypeRow = new ArrayList<Comparable>();
				genotypeRows.put(sampleToIndividualMap.get(sample), genotypeRow);
				genotypeRow.add(sg.getCode());
				
				for (String gtInfo : sg.getAdditionalInfo().keySet())
				{
					if (!headerCols.contains(gtInfo) /* exclude some fields that we don't want to show */  &&!gtInfo.equals(VariantData.GT_FIELD_PHASED_GT) && !gtInfo.equals(VariantData.GT_FIELD_PHASED_ID) && !gtInfo.equals(VariantRunData.FIELDNAME_ADDITIONAL_INFO_EFFECT_GENE) && !gtInfo.equals(VariantRunData.FIELDNAME_ADDITIONAL_INFO_EFFECT_NAME))
					{
						headerCols.add(gtInfo);
						headerColDescs.add(vcfHeader != null ? ((VCFFormatHeaderLine) vcfHeader.getmFormatMetaData().get(gtInfo)).getDescription() : "");
					}
					if (!headerCols.contains(gtInfo))
						continue;

					int cellIndex = headerCols.indexOf(gtInfo);
					while (genotypeRow.size() < cellIndex + 2)
						genotypeRow.add(null);
					genotypeRow.set(cellIndex + 1, sg.getAdditionalInfo().get(gtInfo));
				}
			}
		}

		mav.addObject("headerAdditionalInfo", headerCols);
		mav.addObject("headerAdditionalInfoDesc", headerColDescs);
		mav.addObject("runAdditionalInfo", additionalInfoByRun);
		mav.addObject("runAdditionalInfoDesc", additionalInfoDescByRun);
		mav.addObject("dataByRun", dataByRun);
		mav.addObject("knownAlleles", var.getKnownAlleleList());

		return mav;
	}

	/**
	 * Setup export page.
	 */
	@RequestMapping(variantExportPageURL)
	protected void setupExportPage()
	{
	}

	/**
	 * Export variants.
	 *
	 * @param request the request
	 * @param response the response
	 * @param sModule the module
	 * @param fKeepExportOnServer whether or not to keep export on server
	 * @param sExportFormat the export format
	 * @param exportID the export id
	 * @param projId the proj id
	 * @param selectedVariantTypes the selected variant types
	 * @param selectedSequences the selected sequences
	 * @param selectedIndividuals the selected individuals
	 * @param gtCode the gt code
	 * @param genotypeQualityThreshold the genotype quality threshold
	 * @param readDepthThreshold the read depth threshold
	 * @param missingData the missing data
	 * @param minmaf the minmaf
	 * @param maxmaf the maxmaf
	 * @param minposition the minposition
	 * @param maxposition the maxposition
	 * @param alleleCount the allele count
	 * @param geneName the gene name
	 * @param variantEffects the variant effects
	 * @throws Exception the exception
	 */
	@RequestMapping(variantExportDataURL)
	protected void exportVariants(HttpServletRequest request, HttpServletResponse response, @RequestParam("module") String sModule, @RequestParam("keepExportOnServer") boolean fKeepExportOnServer, @RequestParam("exportFormat") String sExportFormat, @RequestParam("exportID") final String exportID, @RequestParam("project") int projId, @RequestParam("variantTypes") String selectedVariantTypes, @RequestParam("sequences") String selectedSequences, @RequestParam(value="individuals", required=false) String selectedIndividuals, @RequestParam("gtCode") String gtCode, @RequestParam("genotypeQualityThreshold") int genotypeQualityThreshold, @RequestParam("readDepthThreshold") int readDepthThreshold, @RequestParam("missingData") double missingData, @RequestParam(value="minmaf", required=false) Float minmaf, @RequestParam(value="maxmaf", required=false) Float maxmaf, @RequestParam("minposition") Long minposition, @RequestParam("maxposition") Long maxposition, @RequestParam("alleleCount") String alleleCount, @RequestParam("geneName") String geneName, @RequestParam("variantEffects") String variantEffects) throws Exception
	{
		String sShortProcessID = exportID.substring(1 + exportID.indexOf('|'));

		ProgressIndicator progress = ProgressIndicator.get(sShortProcessID);
		if (progress == null)
		{
			progress = new ProgressIndicator(sShortProcessID, new String[] {"Identifying matching variants"});
			ProgressIndicator.registerProgressIndicator(progress);
		}

		long before = System.currentTimeMillis();
		final MongoTemplate mongoTemplate = MongoTemplateManager.get(sModule);

		List<String> selectedIndividualList = selectedIndividuals.length() == 0 ? getIndividualsInDbOrder(sModule, projId) /* no selection means all selected */ : Arrays.asList(selectedIndividuals.split(";"));

		long count = countVariants(request, sModule, projId, selectedVariantTypes, selectedSequences, selectedIndividuals, gtCode, genotypeQualityThreshold, readDepthThreshold, missingData, minmaf, maxmaf, minposition, maxposition, alleleCount, geneName, variantEffects, "" /* if we pass exportID then the progress indicator is going to be replaced by another, and we don't need it for counting since we cache count values */);
		DBCollection tmpVarColl = getTemporaryVariantCollection(sModule, sShortProcessID, false);
		long nTempVarCount = mongoTemplate.count(new Query(), tmpVarColl.getName());
		boolean fWorkingOnFullDataset = mongoTemplate.count(null, VariantData.class) == count;
		if (!fWorkingOnFullDataset && nTempVarCount == 0)
		{
			progress.setError(MESSAGE_TEMP_RECORDS_NOT_FOUND);
			return;
		}

		// use a cursor to avoid using too much memory
		DBObject query = count == nTempVarCount ? null : new BasicDBObject(VariantData.FIELDNAME_VERSION, new BasicDBObject("$exists", true));
		String sequenceField = VariantData.FIELDNAME_REFERENCE_POSITION + "." + ReferencePosition.FIELDNAME_SEQUENCE;
		String startField = VariantData.FIELDNAME_REFERENCE_POSITION + "." + ReferencePosition.FIELDNAME_START_SITE;
		BasicDBObject sort = new BasicDBObject("_id", 1);	/* necessary for MgdbDao.getSampleGenotypes to work properly */		DBObject projection = new BasicDBObject();
		projection.put(sequenceField, 1);
		projection.put(startField, 1);

		DBCursor markerCursor = mongoTemplate.getCollection(!fWorkingOnFullDataset ? tmpVarColl.getName() : mongoTemplate.getCollectionName(VariantData.class)).find(query, projection).sort(sort);
		markerCursor.addOption(Bytes.QUERYOPTION_NOTIMEOUT);

		try
		{
			AbstractIndividualOrientedExportHandler individualOrientedExportHandler = AbstractIndividualOrientedExportHandler.getIndividualOrientedExportHandlers().get(sExportFormat);
			AbstractMarkerOrientedExportHandler markerOrientedExportHandler = AbstractMarkerOrientedExportHandler.getMarkerOrientedExportHandlers().get(sExportFormat);
			
			GenotypingProject project = mongoTemplate.findById(projId, GenotypingProject.class);
			String filename = sModule + "_" + project.getName() + "_" + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + "_" + count + "variants_" + sExportFormat + "." + (individualOrientedExportHandler != null ? individualOrientedExportHandler : markerOrientedExportHandler).getExportFileExtension();
			OutputStream os;
			LOG.info((fKeepExportOnServer ? "On-server" : "Direct-download") + " export requested: " + sShortProcessID);
			if (fKeepExportOnServer)
			{
				String relativeOutputFolder = File.separator + FRONTEND_URL + File.separator + TMP_OUTPUT_FOLDER + File.separator + sShortProcessID.replaceAll("\\|", "_") + File.separator;
				File outputLocation = new File(request.getSession().getServletContext().getRealPath(relativeOutputFolder));
				if (!outputLocation.exists() && !outputLocation.mkdirs())
					throw new Exception("Unable to create folder: " + outputLocation);
				os = new FileOutputStream(new File(outputLocation.getAbsolutePath() + File.separator + filename));
				response.setContentType("text/plain");
			}
			else
			{
				os = response.getOutputStream();
				response.setContentType("application/zip");
				response.setHeader("Content-disposition", "inline; filename=" + filename);
			}

			ArrayList<SampleId> sampleIDs = new ArrayList<SampleId>();
			for (String individual : selectedIndividualList)
				for (Integer individualSampleIndex : project.getIndividualSampleIndexes(individual))
					sampleIDs.add(new SampleId(projId, individualSampleIndex));

			if (fKeepExportOnServer)
			{
				String relativeOutputFolder = FRONTEND_URL + File.separator + TMP_OUTPUT_FOLDER + File.separator + sShortProcessID.replaceAll("\\|", "_") + File.separator;
				String relativeOutputFolderUrl = request.getContextPath() + "/" + relativeOutputFolder.replace(File.separator, "/");
				String exportURL = relativeOutputFolderUrl + filename;
				LOG.debug("On-server export file for export " + sShortProcessID + ": " + exportURL);
				response.getWriter().write(exportURL);
				response.flushBuffer();
			}
//			else
//			{
//				// The two next lines are an ugly hack that makes the client believe transfer has started. Otherwise we may end-up with a client-side timeout (search for network.http.response.timeout for more details)
//				response.getOutputStream().print(" ");
//				response.getOutputStream().flush();
//			}
			
			if (individualOrientedExportHandler != null)
			{
				progress.addStep("Reading and re-organizing genotypes"); // initial step will consist in organizing genotypes by individual rather than by marker
				progress.moveToNextStep();	// done with identifying variants
				LinkedHashMap<String, File> exportFiles = individualOrientedExportHandler.createExportFiles(sModule, markerCursor.copy(), sampleIDs, sShortProcessID, genotypeQualityThreshold, readDepthThreshold, progress);
				if (!progress.hasAborted()) {
					for (String step : individualOrientedExportHandler.getStepList())
						progress.addStep(step);
					progress.moveToNextStep();
					individualOrientedExportHandler.exportData(os, sModule, exportFiles.values(), true, progress, markerCursor, null, null);
				}
			}
			else if (markerOrientedExportHandler != null)
			{
				for (String step : markerOrientedExportHandler.getStepList())
					progress.addStep(step);
				progress.moveToNextStep();	// done with identifying variants

				markerOrientedExportHandler.exportData(os, sModule, sampleIDs, progress, markerCursor, null, genotypeQualityThreshold, readDepthThreshold, null);
				LOG.debug("done with exportData");
			}
			else
				throw new Exception("No export handler found for format " + sExportFormat);

			if (!progress.hasAborted())
			{
				LOG.info("doVariantExport took " + (System.currentTimeMillis() - before)/1000d + "s to process " + count + " variants and " + selectedIndividualList.size() + " individuals");
				progress.markAsComplete();
			}
		}
		catch (Throwable t)
		{
        	LOG.error("Error exporting data", t);
        	progress.setError("Error exporting data: " + t.getClass().getSimpleName() + (t.getMessage() != null ? " - " + t.getMessage() : ""));
			return;
		}
		finally
		{
			markerCursor.close();
		}
	}

	/**
	 * Gets the progress indicator.
	 *
	 * @param processID the process id
	 * @return the progress indicator
	 * @throws Exception the exception
	 */
	@RequestMapping(progressIndicatorURL)
	protected @ResponseBody ProgressIndicator getProgressIndicator(@RequestParam("processID") String processID) throws Exception
	{
		return ProgressIndicator.get(processID.substring(1 + processID.indexOf('|')));
	}

	/**
	 * Gets the sequence filter count.
	 *
	 * @param request the request
	 * @param sModule the module
	 * @return the sequence filter count
	 * @throws Exception the exception
	 */
	@RequestMapping(sequenceFilterCountURL)
	public @ResponseBody int getSequenceFilterCount(HttpServletRequest request, @RequestParam("module") String sModule) throws Exception
	{
		int result = -1;
		File sequenceListFile = new File(request.getSession().getServletContext().getRealPath(File.separator + SEQLIST_FOLDER + File.separator + request.getSession().getId() + "_" + sModule));
		if (sequenceListFile.exists() && sequenceListFile.length() > 0)
		{
		   LineNumberReader lineNumberReader = new LineNumberReader(new FileReader(sequenceListFile));
		   lineNumberReader.skip(Long.MAX_VALUE);
		   result = lineNumberReader.getLineNumber();
		   lineNumberReader.close();
		}
		return result;
	}

	/**
	 * Gets the sequence i ds being filtered on.
	 *
	 * @param request the request
	 * @param sModule the module
	 * @return the sequence i ds being filtered on
	 * @throws Exception the exception
	 */
	protected ArrayList<String> getSequenceIDsBeingFilteredOn(HttpServletRequest request, String sModule) throws Exception
	{
		ArrayList<String> sequences = new ArrayList<String>();
		File selectionFile = new File(request.getSession().getServletContext().getRealPath(File.separator + SEQLIST_FOLDER) + File.separator + request.getSession().getId() + "_" + sModule);
		if (selectionFile.exists() && selectionFile.length() > 0)
		{
			Scanner sc = new Scanner(selectionFile);
			sc.nextLine();	// skip queryKey line
			while (sc.hasNextLine())
				sequences.add(sc.nextLine().trim());
			sc.close();
		}
		return sequences.size() == 0 ? null : sequences;
	}

	/**
	 * Clear selection file.
	 *
	 * @param request the request
	 * @param sModule the module
	 * @throws Exception the exception
	 */
	@RequestMapping(clearSelectedSequenceListURL)
	public @ResponseBody void clearSequenceFilterFile(HttpServletRequest request, @RequestParam("module") String sModule) throws Exception
	{
		File selectionFile = new File(request.getSession().getServletContext().getRealPath(File.separator + SEQLIST_FOLDER) + File.separator + request.getSession().getId() + "_" + sModule);
		if (selectionFile.exists())
			selectionFile.delete();
	}

	/**
	 * Cleanup old temp data.
	 *
	 * @param request the request
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private void cleanupOldTempData(HttpServletRequest request) throws IOException
	{
		if (request.getSession() == null)
			return;	// working around some random bug

		long nowMillis = new Date().getTime();
		File filterOutputLocation = new File(request.getSession().getServletContext().getRealPath(File.separator + FRONTEND_URL + File.separator + TMP_OUTPUT_FOLDER));
		if (filterOutputLocation.exists() && filterOutputLocation.isDirectory())
			for (File f : filterOutputLocation.listFiles())
				if (f.isDirectory() && nowMillis - f.lastModified() > JOB_OUTPUT_EXPIRATION_DELAY_MILLIS)
				{
					FileUtils.deleteDirectory(f);	// it is an expired job-output-folder
					LOG.info("Temporary output-folder was deleted: " + f.getPath());
				}
	}

	/**
	 * Abort process.
	 *
	 * @param processID the process id
	 * @return true, if successful
	 * @throws Exception the exception
	 */
	@RequestMapping(processAbortURL)
	public @ResponseBody boolean abortProcess(@RequestParam("processID") String processID) throws Exception
	{
		ProgressIndicator progress = ProgressIndicator.get(processID.substring(1 + processID.indexOf('|')));
		if (progress != null)
		{
			progress.abort();
			LOG.debug("Aborting process: " + processID);
			return true;
		}
		return false;
	}

	/**
	 * On interface unload.
	 *
	 * @param sModule the module
	 * @param processID the process id
	 * @throws Exception the exception
	 */
	@RequestMapping(interfaceCleanupURL)
	public @ResponseBody void onInterfaceUnload(@RequestParam("module") String sModule, @RequestParam("processID") String processID) throws Exception
	{
		String collName = getTemporaryVariantCollection(sModule, processID.substring(1 + processID.indexOf('|')), false).getName();
		MongoTemplateManager.get(sModule).dropCollection(collName);
		LOG.debug("Dropped collection " + collName);
	}

	/**
	 * Setup chart page.
	 *
	 * @param chartType the chart type
	 * @return the model and view
	 */
	@RequestMapping(chartPageURL)
	protected ModelAndView setupChartPage(@RequestParam("chartType") String chartType)
	{
		ModelAndView mav = new ModelAndView("/" + CHART_URL + "/" + chartType);
		return mav;
	}

	/**
	 * Distinct sequences in selection.
	 *
	 * @param request the request
	 * @param sModule the module
	 * @param projId the proj id
	 * @param processID the process id
	 * @return the collection
	 * @throws Exception the exception
	 */
	@RequestMapping(distinctSequencesInSelectionURL)
	protected @ResponseBody Collection<String> distinctSequencesInSelection(HttpServletRequest request, @RequestParam("module") String sModule, @RequestParam("project") int projId, @RequestParam("processID") String processID) throws Exception
	{
		String sShortProcessID = processID.substring(1 + processID.indexOf('|'));
		DBCollection tmpVarColl = getTemporaryVariantCollection(sModule, sShortProcessID, false);
		if (tmpVarColl.count() == 0)
			return listSequences(request, sModule, projId);	// working on full dataset

		List<String> distinctSequences = tmpVarColl.distinct(VariantData.FIELDNAME_REFERENCE_POSITION + "." + ReferencePosition.FIELDNAME_SEQUENCE);
		TreeSet<String> sortedResult = new TreeSet<String>(new AlphaNumericStringComparator());
		sortedResult.addAll(distinctSequences);
		return sortedResult;
	}

	/**
	 * Gets the sequence filter query key.
	 *
	 * @param request the request
	 * @param sModule the module
	 * @return the sequence filter query key
	 * @throws Exception the exception
	 */
	private String getSequenceFilterQueryKey(HttpServletRequest request, String sModule) throws Exception
	{
		String qk = null;
		File sequenceListFile = new File(request.getSession().getServletContext().getRealPath(File.separator + SEQLIST_FOLDER + File.separator + request.getSession().getId() + "_" + sModule));
		if (sequenceListFile.exists() && sequenceListFile.length() > 0)
		{
		   LineNumberReader lineNumberReader = new LineNumberReader(new FileReader(sequenceListFile));
		   qk = lineNumberReader.readLine();
		   lineNumberReader.close();
		}
		return qk;
	}

	/**
	 * Gets the query key.
	 *
	 * @param request the request
	 * @param sModule the module
	 * @param projId the proj id
	 * @param selectedVariantTypes the selected variant types
	 * @param selectedSequences the selected sequences
	 * @param selectedIndividuals the selected individuals
	 * @param gtCode the gt code
	 * @param genotypeQualityThreshold the genotype quality threshold
	 * @param readDepthThreshold the read depth threshold
	 * @param missingData the missing data
	 * @param minmaf the minmaf
	 * @param maxmaf the maxmaf
	 * @param minposition the minposition
	 * @param maxposition the maxposition
	 * @param alleleCount the allele count
	 * @param geneName the gene name
	 * @param variantEffects the variant effects
	 * @return the query key
	 * @throws Exception the exception
	 */
	private String getQueryKey(HttpServletRequest request, String sModule, int projId, String selectedVariantTypes, String selectedSequences, String selectedIndividuals, String gtCode, int genotypeQualityThreshold, int readDepthThreshold, double missingData, Float minmaf, Float maxmaf, Long minposition, Long maxposition, String alleleCount, String geneName, String variantEffects) throws Exception
	{
		String queryKey = projId + ":" + selectedIndividuals + ":" + gtCode + ":" + selectedVariantTypes + ":" + selectedSequences + ":" + genotypeQualityThreshold + ":" + readDepthThreshold + ":" + missingData + ":" + minmaf + ":" + maxmaf + ":" + (minposition == null ? "" : minposition) + ":" + (maxposition == null ? "" : maxposition) + ":" + alleleCount + ":" + geneName + ":" + variantEffects;
		String sequenceQK = getSequenceFilterQueryKey(request, sModule);
		if (sequenceQK != null)
			queryKey += ":seq=[" + sequenceQK + "]";
		return Helper.convertToMD5(queryKey);
	}

	/**
	 * Gets the temporary variant collection.
	 *
	 * @param sModule the module
	 * @param processID the process id
	 * @param fEmptyItBeforeHand whether or not to empty it beforehand
	 * @return the temporary variant collection
	 */
	private DBCollection getTemporaryVariantCollection(String sModule, String processID, boolean fEmptyItBeforeHand)
	{
		DBCollection coll = MongoTemplateManager.get(sModule).getCollection(MongoTemplateManager.TEMP_EXPORT_PREFIX + Helper.convertToMD5(processID));
		if (fEmptyItBeforeHand)
		{
			coll.drop();
			coll.createIndex(VariantData.FIELDNAME_REFERENCE_POSITION + "." + ReferencePosition.FIELDNAME_SEQUENCE);
			coll.createIndex(VariantData.FIELDNAME_REFERENCE_POSITION + "." + ReferencePosition.FIELDNAME_START_SITE);
			coll.createIndex(VariantData.FIELDNAME_TYPE);
		}
		return coll;
	}

	/**
	 * Selection density.
	 *
	 * @param request the request
	 * @param sModule the module
	 * @param projId the proj id
	 * @param selectedVariantTypes the selected variant types
	 * @param selectedSequences the selected sequences
	 * @param selectedIndividuals the selected individuals
	 * @param gtCode the gt code
	 * @param genotypeQualityThreshold the genotype quality threshold
	 * @param readDepthThreshold the read depth threshold
	 * @param missingData the missing data
	 * @param minmaf the minmaf
	 * @param maxmaf the maxmaf
	 * @param minposition the minposition
	 * @param maxposition the maxposition
	 * @param alleleCount the allele count
	 * @param geneName the gene name
	 * @param variantEffects the variant effects
	 * @param processID the process id
	 * @param displayedSequence the displayed sequence
	 * @param displayedRangeMin the displayed range min
	 * @param displayedRangeMax the displayed range max
	 * @param displayedRangeIntervalCount the displayed range interval count
	 * @param displayedVariantType the displayed variant type
	 * @return the map
	 * @throws Exception the exception
	 */
	@RequestMapping(selectionDensityDataURL)
	protected @ResponseBody Map<Long, Long> selectionDensity(HttpServletRequest request, @RequestParam("module") String sModule, @RequestParam("project") int projId, @RequestParam("variantTypes") String selectedVariantTypes, @RequestParam("sequences") String selectedSequences, @RequestParam("individuals") String selectedIndividuals, @RequestParam("gtCode") String gtCode, @RequestParam("genotypeQualityThreshold") int genotypeQualityThreshold, @RequestParam("readDepthThreshold") int readDepthThreshold, @RequestParam("missingData") double missingData, @RequestParam("minmaf") Float minmaf, @RequestParam("maxmaf") Float maxmaf, @RequestParam("minposition") Long minposition, @RequestParam("maxposition") Long maxposition, @RequestParam("alleleCount") String alleleCount, @RequestParam("geneName") String geneName, @RequestParam("variantEffects") String variantEffects, @RequestParam("processID") String processID, @RequestParam("displayedSequence") String displayedSequence, @RequestParam(required=false, value="displayedRangeMin") Long displayedRangeMin, @RequestParam(required=false, value="displayedRangeMax") Long displayedRangeMax, @RequestParam(required=false, value="displayedRangeIntervalCount") final Integer displayedRangeIntervalCount, @RequestParam(required=false, value="displayedVariantType") String displayedVariantType) throws Exception
	{
		long before = System.currentTimeMillis();

		String sShortProcessID = processID.substring(1 + processID.indexOf('|'));

		ProgressIndicator progress = new ProgressIndicator(sShortProcessID, new String[] {"Calculating " + (displayedVariantType != null ? displayedVariantType + " " : "") + "variant density on sequence " + displayedSequence});
		ProgressIndicator.registerProgressIndicator(progress);

		final MongoTemplate mongoTemplate = MongoTemplateManager.get(sModule);

		long count = countVariants(request, sModule, projId, selectedVariantTypes, selectedSequences, selectedIndividuals, gtCode, genotypeQualityThreshold, readDepthThreshold, missingData, minmaf, maxmaf, minposition, maxposition, alleleCount, geneName, variantEffects, "" /* if we pass exportID then the progress indicator is going to be replaced by another, and we don't need it for counting since we cache count values */);
		DBCollection tmpVarColl = getTemporaryVariantCollection(sModule, sShortProcessID, false);
		boolean fStillGotUnwantedTempVariants = count < tmpVarColl.count();
		long nTempVarCount = mongoTemplate.count(new Query(), tmpVarColl.getName());
		final boolean fWorkingOnFullDataset = mongoTemplate.count(null, VariantData.class) == count;
		if (!fWorkingOnFullDataset && nTempVarCount == 0)
		{
			progress.setError(MESSAGE_TEMP_RECORDS_NOT_FOUND);
			return null;
		}

		final String actualCollectionName = fWorkingOnFullDataset ? mongoTemplate.getCollectionName(VariantData.class) : tmpVarColl.getName();

		if (displayedRangeMin == null || displayedRangeMax == null)
		{
			BasicDBList matchAndList = new BasicDBList();
			matchAndList.add(new BasicDBObject(VariantData.FIELDNAME_REFERENCE_POSITION + "." + ReferencePosition.FIELDNAME_SEQUENCE, displayedSequence));
			if (displayedVariantType != null)
				matchAndList.add(new BasicDBObject(VariantData.FIELDNAME_TYPE, displayedVariantType));
			BasicDBObject match = new BasicDBObject("$match", new BasicDBObject("$and", matchAndList));

			BasicDBObject groupFields = new BasicDBObject("_id", null);
			groupFields.put("min", new BasicDBObject("$min", "$" + (VariantData.FIELDNAME_REFERENCE_POSITION + "." + ReferencePosition.FIELDNAME_START_SITE)));
			groupFields.put("max", new BasicDBObject("$max", "$" + (VariantData.FIELDNAME_REFERENCE_POSITION + "." + ReferencePosition.FIELDNAME_START_SITE)));
			BasicDBObject group = new BasicDBObject("$group", groupFields);

			List<DBObject> pipeline = new ArrayList<DBObject>();
			pipeline.add(match);
			pipeline.add(group);
			Iterator<DBObject> iterator = mongoTemplate.getCollection(actualCollectionName).aggregate(pipeline).results().iterator();
			if (!iterator.hasNext())
			{
				progress.markAsComplete();
				return null;	// no variants found matching filter
			}

			DBObject aggResult = (DBObject) iterator.next();
			if (displayedRangeMin == null)
				displayedRangeMin = (Long) aggResult.get("min");
			if (displayedRangeMax == null)
				displayedRangeMax = (Long) aggResult.get("max");
		}

		final AtomicInteger finishedThreadCount = new AtomicInteger(0), nTotalTreatedVariantCountfinishedThreadCount = new AtomicInteger(0);
		final ConcurrentHashMap<Long, Long> result = new ConcurrentHashMap<Long, Long>();
		final int intervalSize = Math.max(1, (int) ((displayedRangeMax - displayedRangeMin) / displayedRangeIntervalCount));
		final ArrayList<Thread> threadsToWaitFor = new ArrayList<Thread>();
		final long rangeMin = displayedRangeMin;
		final ProgressIndicator finalProgress = progress;

		for (int i=0; i<displayedRangeIntervalCount; i++)
		{
			List<Criteria> crits = new ArrayList<Criteria>();
			crits.add(Criteria.where(VariantData.FIELDNAME_REFERENCE_POSITION + "." + ReferencePosition.FIELDNAME_SEQUENCE).is(displayedSequence));
			if (fStillGotUnwantedTempVariants)
				crits.add(Criteria.where(VariantData.FIELDNAME_VERSION).exists(true));
			if (displayedVariantType != null)
				crits.add(Criteria.where(VariantData.FIELDNAME_TYPE).is(displayedVariantType));
			String startSitePath = VariantData.FIELDNAME_REFERENCE_POSITION + "." + ReferencePosition.FIELDNAME_START_SITE;
			crits.add(Criteria.where(startSitePath).gte(displayedRangeMin + (i*intervalSize)));
			if (i < displayedRangeIntervalCount - 1)
				crits.add(Criteria.where(startSitePath).lt(displayedRangeMin + ((i+1)*intervalSize)));
			else
				crits.add(Criteria.where(startSitePath).lte(displayedRangeMax));

			final Query query = new Query(new Criteria().andOperator(crits.toArray(new Criteria[crits.size()])));
			final long chunkIndex = i;

            Thread t = new Thread() {
            	public void run() {
            		if (!finalProgress.hasAborted())
            		{
	        			long partialCount = mongoTemplate.count(query, actualCollectionName);
	        			nTotalTreatedVariantCountfinishedThreadCount.addAndGet((int) partialCount);
	        			result.put(rangeMin + (chunkIndex*intervalSize), partialCount);
	        			finalProgress.setCurrentStepProgress((short) (finishedThreadCount.incrementAndGet()*100/displayedRangeIntervalCount));
            		}
            	}
            };

            if (chunkIndex%NUMBER_OF_SIMULTANEOUS_QUERY_THREADS  == (NUMBER_OF_SIMULTANEOUS_QUERY_THREADS-1))
            	t.run();	// run synchronously
            else
            {
            	threadsToWaitFor.add(t);
            	t.start();	// run asynchronously for better speed
            }
		}

		if (progress.hasAborted())
			return null;

		for (Thread t : threadsToWaitFor)	// wait for all threads before moving to next phase
			t.join();

		progress.setCurrentStepProgress(100);
		LOG.debug("selectionDensity treated " + nTotalTreatedVariantCountfinishedThreadCount.get() + " variants in " + (System.currentTimeMillis() - before)/1000f + "s");
		progress.markAsComplete();

		return new TreeMap<Long, Long>(result);
	}
}