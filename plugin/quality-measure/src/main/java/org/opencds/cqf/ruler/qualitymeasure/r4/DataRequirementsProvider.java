package org.opencds.cqf.ruler.qualitymeasure.r4;

import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.rp.r4.MeasureResourceProvider;
import org.opencds.cqf.ruler.api.OperationProvider;
import org.opencds.cqf.ruler.utility.ClientUtilities;
import org.opencds.cqf.ruler.utility.OperatorUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;


public class DataRequirementsProvider implements OperationProvider, ClientUtilities, OperatorUtilities {

	private static final Logger logger = LoggerFactory.getLogger(DataRequirementsProvider.class);

	@Autowired
	private DaoRegistry myDaoRegistry;

	@Autowired
	private MeasureResourceProvider measureResourceProvider;

}
