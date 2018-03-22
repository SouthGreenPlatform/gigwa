package fr.cirad.tools.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import fr.cirad.mgdb.model.mongo.maintypes.GenotypingProject;
import fr.cirad.mgdb.model.mongo.maintypes.Individual;
import fr.cirad.mgdb.model.mongo.maintypes.VariantRunData;
import fr.cirad.mgdb.model.mongo.maintypes.VariantRunData.VariantRunDataId;
import fr.cirad.mgdb.model.mongodao.MgdbDao;
import fr.cirad.security.base.IModuleManager;
import fr.cirad.tools.mongo.MongoTemplateManager;
import fr.cirad.tools.security.TokenManager;
import fr.cirad.tools.security.base.AbstractTokenManager;

@Component
public class GigwaModuleManager implements IModuleManager {

	private static final Logger LOG = Logger.getLogger(GigwaModuleManager.class);
	
	@Autowired TokenManager tokenManager;
	
	@Override
	public Collection<String> getModules(Boolean fTrueForPublicFalseForPrivateNullForBoth) {
		if (fTrueForPublicFalseForPrivateNullForBoth == null)
			return MongoTemplateManager.getAvailableModules();
		if (Boolean.TRUE.equals(fTrueForPublicFalseForPrivateNullForBoth))
			return MongoTemplateManager.getPublicDatabases();
		return CollectionUtils.disjunction(MongoTemplateManager.getAvailableModules(), MongoTemplateManager.getPublicDatabases());
	}

	@Override
	public Map<String, Map<Comparable, String>> getEntitiesByModule(String entityType, Boolean fTrueIfPublicFalseIfPrivateNullIfAny)
	{
		Map<String, Map<Comparable, String>> entitiesByModule = new LinkedHashMap<String, Map<Comparable, String>>();
		if ("project".equals(entityType))
			for (String sModule : MongoTemplateManager.getAvailableModules())
				if (fTrueIfPublicFalseIfPrivateNullIfAny == null || (MongoTemplateManager.isModulePublic(sModule) == fTrueIfPublicFalseIfPrivateNullIfAny))
				{
					Map<Comparable, String> moduleEntities = entitiesByModule.get(sModule);
					if (moduleEntities == null)
					{
						moduleEntities = new LinkedHashMap<Comparable, String>();
						entitiesByModule.put(sModule, moduleEntities);
					}
					
					Query q = new Query();
					q.with(new Sort("_id"));
					q.fields().include(GenotypingProject.FIELDNAME_NAME);
					for (GenotypingProject project : MongoTemplateManager.get(sModule).find(q, GenotypingProject.class))
						moduleEntities.put(project.getId(), project.getName());
				}		
		return entitiesByModule;
	}

	@Override
	public boolean isModuleHidden(String sModule) {
		return MongoTemplateManager.isModuleHidden(sModule);
	}

	@Override
	public boolean removeDataSource(String sModule, boolean fAlsoDropDatabase) {
		return MongoTemplateManager.removeDataSource(sModule, fAlsoDropDatabase);
	}

	@Override
	public boolean updateDataSource(String sModule, boolean fPublic, boolean fHidden, String sSpeciesName) throws Exception {
		return MongoTemplateManager.saveOrUpdateDataSource(MongoTemplateManager.ModuleAction.UPDATE_STATUS, sModule, fPublic, fHidden, null, sSpeciesName, null);
	}

	@Override
	public boolean createDataSource(String sModule, String sHost, String sSpeciesName, Long expiryDate) throws Exception {
		return MongoTemplateManager.saveOrUpdateDataSource(MongoTemplateManager.ModuleAction.CREATE, sModule, false, false, sHost, sSpeciesName, expiryDate);
	}
	
	@Override
	public Collection<String> getHosts() {
		return MongoTemplateManager.getHostNames();
	}
	
	@Override
	public boolean removeManagedEntity(String sModule, String sEntityType, Comparable entityId) throws Exception {
		if (AbstractTokenManager.ENTITY_PROJECT.equals(sEntityType))
		{
			final int nProjectIdToRemove = Integer.parseInt(entityId.toString());
			if (!tokenManager.canUserWriteToProject(SecurityContextHolder.getContext().getAuthentication(), sModule, nProjectIdToRemove))
				throw new Exception("You are not allowed to remove this project");

			MongoTemplate mongoTemplate = MongoTemplateManager.get(sModule);
			Query query = new Query();
			query.fields().include(GenotypingProject.FIELDNAME_SAMPLES);
			List<String> individualsInThisProject = null, individualsInOtherProjects = new ArrayList<>();
			int nProjCount = 0;
			for (GenotypingProject proj : mongoTemplate.find(query, GenotypingProject.class))
			{
				nProjCount++;
				List<String> projectIndividuals = proj.getSamples().values().stream().map(sp -> sp.getIndividual()).collect(Collectors.toList());
				if (proj.getId() == nProjectIdToRemove)
					individualsInThisProject = projectIndividuals;
				else
					individualsInOtherProjects.addAll(projectIndividuals);
			}
			if (nProjCount == 1 && !individualsInThisProject.isEmpty())
			{
				mongoTemplate.getDb().dropDatabase();
				LOG.debug("Dropped database for module " + sModule + " instead of removing its only project");
				return true;
			}

			Collection<String> individualsToRemove = CollectionUtils.disjunction(individualsInThisProject, CollectionUtils.intersection(individualsInThisProject, individualsInOtherProjects));
			int nRemovedIndCount = mongoTemplate.remove(new Query(Criteria.where("_id").in(individualsToRemove)), Individual.class).getN();
			LOG.debug("Removed " + nRemovedIndCount + " individuals out of " + individualsInThisProject.size());

			if (mongoTemplate.remove(new Query(Criteria.where("_id").is(nProjectIdToRemove)), GenotypingProject.class).getN() > 0)
				LOG.debug("Removed project " + nProjectIdToRemove + " from module " + sModule);
			
			new Thread() {
				public void run() {
					int nRemovedVrdCount = mongoTemplate.remove(new Query(Criteria.where("_id." + VariantRunDataId.FIELDNAME_PROJECT_ID).is(nProjectIdToRemove)), VariantRunData.class).getN();
					LOG.debug("Removed " + nRemovedVrdCount + " VRD records for project " + nProjectIdToRemove + " of module " + sModule);
				}
			}.start();
			LOG.debug("Launched async VRD cleanup for project " + nProjectIdToRemove + " of module " + sModule);
			
            mongoTemplate.getCollection(MgdbDao.COLLECTION_NAME_CACHED_COUNTS).drop();
			return true;
		}
		else
			throw new Exception("Not managing entities of type " + sEntityType);
	}

	@Override
	public boolean doesEntityExistInModule(String sModule, String sEntityType, Comparable entityId) {
		if (AbstractTokenManager.ENTITY_PROJECT.equals(sEntityType))
		{
			final int nProjectId = Integer.parseInt(entityId.toString());
			return MongoTemplateManager.get(sModule).count(new Query(Criteria.where("_id").is(nProjectId)), GenotypingProject.class) == 1;
		}
		else
		{
			LOG.error("Not managing entities of type " + sEntityType);
			return false;
		}
	}

	@Override
	public boolean doesEntityTypeSupportVisibility(String sModule, String sEntityType) {
		return false;
	}

	@Override
	public boolean setManagedEntityVisibility(String sModule, String sEntityType, Comparable entityId, boolean fPublic) throws Exception {
		return false;
	}
}
