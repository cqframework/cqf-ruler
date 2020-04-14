package org.opencds.cqf.r4.schedule;

import org.hl7.fhir.r4.model.Task;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpecificJob extends BaseTaskJob {

    private static final Logger logger = LoggerFactory.getLogger(SpecificJob.class);

    public SpecificJob(Task task) {
        super(task);
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        System.out.println("Specific job started");

        //write implementation details here

        System.out.println("Specific job finished");

    }


}
