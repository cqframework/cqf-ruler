package org.opencds.cqf.ruler.cdshooks;

import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.cache.IResourceChangeEvent;
import ca.uhn.fhir.jpa.cache.IResourceChangeListener;
import ca.uhn.fhir.jpa.cache.ResourceChangeEvent;
import org.opencds.cqf.cql.engine.runtime.Code;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.ValueSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ValueSetCache implements IResourceChangeListener {
	private static final Logger logger = LoggerFactory.getLogger(ValueSetCache.class);
	private IFhirResourceDao<?> valueSetDao;
	private Map<VersionedIdentifier, List<Code>> valueSetCache;

	public ValueSetCache(DaoRegistry daoRegistry) {
		this.valueSetDao = daoRegistry.getResourceDao("ValueSet");
		this.valueSetCache = new HashMap<>();
	}

	public Map<VersionedIdentifier, List<Code>> getValueSetCache() {
		return valueSetCache;
	}

	@Override
	public void handleInit(Collection<IIdType> collection) {
		handleChange(ResourceChangeEvent.fromCreatedUpdatedDeletedResourceIds(new ArrayList<>(collection),
			Collections.emptyList(), Collections.emptyList()));
	}

	@Override
	public void handleChange(IResourceChangeEvent iResourceChangeEvent) {
		if (iResourceChangeEvent == null)
			return;
		if (iResourceChangeEvent.getCreatedResourceIds() != null
			&& !iResourceChangeEvent.getCreatedResourceIds().isEmpty()) {
			insert(iResourceChangeEvent.getCreatedResourceIds());
		}
		if (iResourceChangeEvent.getUpdatedResourceIds() != null
			&& !iResourceChangeEvent.getUpdatedResourceIds().isEmpty()) {
			update(iResourceChangeEvent.getUpdatedResourceIds());
		}
		if (iResourceChangeEvent.getDeletedResourceIds() != null
			&& !iResourceChangeEvent.getDeletedResourceIds().isEmpty()) {
			delete(iResourceChangeEvent.getDeletedResourceIds());
		}
	}

	private void insert(List<IIdType> createdIds) {
		for (IIdType id : createdIds) {
			try {
				VersionedIdentifier identifier = new VersionedIdentifier().withId(id.getIdPart());
				if (!valueSetCache.containsKey(identifier)) {
					IBaseResource valueSet = valueSetDao.read(id);
					if (valueSet instanceof ValueSet && ((ValueSet) valueSet).hasExpansion()) {
						valueSetCache.put(identifier, ((ValueSet) valueSet).getExpansion().getContains().stream().map(
							e -> new Code().withCode(e.getCode()).withSystem(e.getSystem())
						).collect(Collectors.toList()));
					}
				}
			} catch (Exception e) {
				logger.info(String.format("Failed to create service for %s", id.getIdPart()));
			}
		}
	}

	private void update(List<IIdType> updatedIds) {
		try {
			delete(updatedIds);
			insert(updatedIds);
		} catch (Exception e) {
			logger.info(String.format("Failed to update service(s) for %s", updatedIds));
		}
	}

	private void delete(List<IIdType> deletedIds) {
		for (IIdType id : deletedIds) {
			valueSetCache.remove(new VersionedIdentifier().withId(id.getIdPart()));
		}
	}
}
