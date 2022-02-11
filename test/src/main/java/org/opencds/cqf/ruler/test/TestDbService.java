package org.opencds.cqf.ruler.test;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.Table;
import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TestDbService {

	@Autowired
    private EntityManager entityManager;
    private List<String> tableNames;

    @PostConstruct
    void afterPropertiesSet() {
        tableNames = entityManager.getMetamodel().getEntities().stream()
            .filter(entityType -> entityType.getJavaType().getAnnotation(Table.class) != null)
            .map(entityType -> entityType.getJavaType().getAnnotation(Table.class))
            .map(this::convertToTableName) // TODO
            .collect(Collectors.toList());
    }

	 private String convertToTableName(Table table) {
		String schema = table.schema();
		String tableName = table.name();

		String convertedSchema = StringUtils.hasText(schema) ? schema.toLowerCase() + "." : "";
		String convertedTableName = tableName.replaceAll("([a-z])([A-Z])", "$1_$2");

		return convertedSchema + convertedTableName;
  }

  @Transactional
  public void resetDatabase() {
		entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();

		for (String tableName : tableNames) {
			 entityManager.createNativeQuery("TRUNCATE TABLE " + tableName).executeUpdate();
		}

		entityManager.createNativeQuery("DROP ALL OBJECTS").executeUpdate();

		entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();
  }
}
