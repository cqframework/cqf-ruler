package org.opencds.cqf.ruler.provider;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.internal.StandardDialectResolver;
import org.hibernate.engine.jdbc.dialect.spi.DatabaseMetaDataDialectResolutionInfoAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.jpa.config.HibernatePropertiesProvider;
import ca.uhn.fhir.util.ReflectionUtil;

public class PatchedJpaHibernatePropertiesProvider extends HibernatePropertiesProvider {

	private LocalContainerEntityManagerFactoryBean entityManagerFactory;

	private Dialect dialect;

	public PatchedJpaHibernatePropertiesProvider(LocalContainerEntityManagerFactoryBean entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;
	}

	@Override
	public Dialect getDialect() {
		if (dialect == null) {
			String dialectClass = (String) entityManagerFactory.getJpaPropertyMap().get("hibernate.dialect");
			dialect = ReflectionUtil.newInstanceOrReturnNull(dialectClass, Dialect.class);
		}

		if (dialect == null) {
			DataSource connection = entityManagerFactory.getDataSource();
			try (Connection c = connection.getConnection()) {
				dialect = new StandardDialectResolver()
						.resolveDialect(
								new DatabaseMetaDataDialectResolutionInfoAdapter(c.getMetaData()));
			} catch (SQLException sqlException) {
				throw new ConfigurationException(sqlException.getMessage(), sqlException);
			}
		}

		return dialect;
	}
}
