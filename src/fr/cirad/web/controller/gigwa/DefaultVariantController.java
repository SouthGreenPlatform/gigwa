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
package fr.cirad.web.controller.gigwa;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Controller;

import fr.cirad.mgdb.model.mongo.maintypes.GenotypingProject;
import fr.cirad.mgdb.model.mongo.maintypes.VariantData;
import fr.cirad.mgdb.model.mongo.maintypes.VariantRunData;
import fr.cirad.mgdb.model.mongo.maintypes.VariantRunData.VariantRunDataId;
import fr.cirad.mgdb.model.mongo.subtypes.GenotypingSample;
import fr.cirad.mgdb.model.mongo.subtypes.SampleGenotype;
import fr.cirad.tools.mongo.MongoTemplateManager;
import fr.cirad.web.controller.gigwa.base.AbstractVariantController;

/**
 * The Class DefaultVariantController.
 */
@Controller
public class DefaultVariantController extends AbstractVariantController
{

	/** The Constant LOG. */
	private static final Logger LOG = Logger.getLogger(DefaultVariantController.class);

	/* (non-Javadoc)
	 * @see fr.cirad.web.controller.gigwa.base.AbstractVariantController#getProjectEffectAnnotations(java.lang.String, int)
	 */
	@Override
	protected TreeSet<String> getProjectEffectAnnotations(String sModule, int projId) throws Exception
	{
		MongoTemplate mongoTemplate = MongoTemplateManager.get(sModule);
		GenotypingProject proj = mongoTemplate.findById(projId, GenotypingProject.class);
		return proj.getEffectAnnotations();
	}

	/* (non-Javadoc)
	 * @see fr.cirad.web.controller.gigwa.base.AbstractVariantController#getProjectPloidyLevel(java.lang.String, int)
	 */
	@Override
	protected int getProjectPloidyLevel(String sModule, int projId) throws Exception
	{
		MongoTemplate mongoTemplate = MongoTemplateManager.get(sModule);
		GenotypingProject proj = mongoTemplate.findById(projId, GenotypingProject.class);
		return proj.getPloidyLevel();
	}

	/* (non-Javadoc)
	 * @see fr.cirad.web.controller.gigwa.base.AbstractVariantController#getProjectSequences(java.lang.String, int)
	 */
	@Override
	protected List<String> getProjectSequences(String sModule, int projId) throws Exception
	{
		MongoTemplate mongoTemplate = MongoTemplateManager.get(sModule);
		GenotypingProject proj = mongoTemplate.findById(projId, GenotypingProject.class);
		return new ArrayList<String>(proj.getSequences());
	}

	/* (non-Javadoc)
	 * @see fr.cirad.web.controller.gigwa.base.AbstractVariantController#getProjectDistinctAlleleCounts(java.lang.String, int)
	 */
	@Override
	protected TreeSet<Integer> getProjectDistinctAlleleCounts(String sModule, int projId) throws Exception
	{
		MongoTemplate mongoTemplate = MongoTemplateManager.get(sModule);
		GenotypingProject proj = mongoTemplate.findById(projId, GenotypingProject.class);
		return proj.getAlleleCounts();
	}

	/* (non-Javadoc)
	 * @see fr.cirad.web.controller.gigwa.base.AbstractVariantController#getProjectVariantTypes(java.lang.String, int)
	 */
	@Override
	protected List<String> getProjectVariantTypes(String sModule, int projId) throws Exception
	{
		MongoTemplate mongoTemplate = MongoTemplateManager.get(sModule);
		GenotypingProject proj = mongoTemplate.findById(projId, GenotypingProject.class);
		return proj.getVariantTypes();
	}

	/* (non-Javadoc)
	 * @see fr.cirad.web.controller.gigwa.base.AbstractVariantController#getProjectIdToNameMap(java.lang.String)
	 */
	@Override
	protected Map<Comparable, String> getProjectIdToNameMap(String sModule) throws Exception
	{
		MongoTemplate mongoTemplate = MongoTemplateManager.get(sModule);
		Map<Comparable, String> result = new LinkedHashMap<Comparable, String>();
		for (GenotypingProject proj : mongoTemplate.findAll(GenotypingProject.class))
			result.put(proj.getId(), proj.getName());
		return result;
	}

	/* (non-Javadoc)
	 * @see fr.cirad.web.controller.gigwa.base.AbstractVariantController#getIndividualsInDbOrder(java.lang.String, int)
	 */
	@Override
	protected List<String> getIndividualsInDbOrder(String sModule, int project) throws Exception
	{
		MongoTemplate mongoTemplate = MongoTemplateManager.get(sModule);
		GenotypingProject proj = mongoTemplate.findById(project, GenotypingProject.class);
		List<String> result = new ArrayList<String>();
		for (GenotypingSample gs : proj.getSamples().values())
			if (!result.contains(gs.getIndividual()))
				result.add(gs.getIndividual());
		return result;
	}

	/* (non-Javadoc)
	 * @see fr.cirad.web.controller.gigwa.base.AbstractVariantController#doesProjectContainGQField(java.lang.String, int)
	 */
	@Override
	protected boolean doesProjectContainGQField(String sModule, int projId) {
		MongoTemplate mongoTemplate = MongoTemplateManager.get(sModule);
		Query q = new Query(Criteria.where("_id." + VariantRunDataId.FIELDNAME_PROJECT_ID).is(projId));
		q.limit(3);
		q.fields().include(VariantRunData.FIELDNAME_SAMPLEGENOTYPES);
		Iterator<VariantRunData> it = mongoTemplate.find(q, VariantRunData.class).iterator();
		while (it.hasNext())
			for (SampleGenotype sg : it.next().getSampleGenotypes().values())
				if (sg.getAdditionalInfo().containsKey(VariantData.GT_FIELD_GQ))
					return true;
		return false;
	}
}
