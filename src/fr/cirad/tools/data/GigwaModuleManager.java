package fr.cirad.tools.data;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import fr.cirad.mgdb.model.mongo.maintypes.GenotypingProject;
import fr.cirad.security.base.IModuleManager;
import fr.cirad.tools.mongo.MongoTemplateManager;

@Component
public class GigwaModuleManager implements IModuleManager {

	@Override
	public Collection<String> getModules(Boolean fTrueForPublicFalseForPrivateNullForBoth) {
		if (fTrueForPublicFalseForPrivateNullForBoth == null)
			return MongoTemplateManager.getAvailableModules();
		if (Boolean.TRUE.equals(fTrueForPublicFalseForPrivateNullForBoth))
			return MongoTemplateManager.getPublicDatabases();
		return CollectionUtils.disjunction(MongoTemplateManager.getAvailableModules(), MongoTemplateManager.getPublicDatabases());
	}

	@Override
	public Map<String, Map<Comparable, String>> getEntitiesByModule(String entityType)
	{
		return getEntitiesByModule(entityType, null);
	}
	
	public Map<String, Map<Comparable, String>> getEntitiesByModule(String entityType, Boolean fTrueIfPublicFalseIfPrivateNullIfBoth)
	{
		Map<String, Map<Comparable, String>> entitiesByModule = new LinkedHashMap<String, Map<Comparable, String>>();
		if ("project".equals(entityType))
			for (String sModule : MongoTemplateManager.getAvailableModules())
				if (fTrueIfPublicFalseIfPrivateNullIfBoth == null || (MongoTemplateManager.isModulePublic(sModule) == fTrueIfPublicFalseIfPrivateNullIfBoth))
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
	public boolean updateDataSource(String sModule, boolean fPublic, boolean fHidden) throws Exception {
		return MongoTemplateManager.saveOrUpdateDataSource(MongoTemplateManager.ModuleAction.UPDATE_STATUS, sModule, fPublic, fHidden, null, null);
	}

	@Override
	public boolean createDataSource(String sModule, String sHost, Long expiryDate) throws Exception {
		return MongoTemplateManager.saveOrUpdateDataSource(MongoTemplateManager.ModuleAction.UPDATE_STATUS, sModule, false, false, sHost, expiryDate);
	}
	
	@Override
	public Collection<String> getHosts() {
		return MongoTemplateManager.getHostNames();
	}
}
