package org.opencds.cqf.r4.schedule;

import ca.uhn.fhir.jpa.model.sched.HapiJob;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.hl7.fhir.r4.model.Task.TaskPriority;
import org.hl7.fhir.r4.model.Task.TaskIntent;
import org.hl7.fhir.r4.model.Task;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class BaseTaskJob implements HapiJob{

    private static final Logger logger = LoggerFactory.getLogger(BaseTaskJob.class);

    private Task task;

    private String id = "";

    public BaseTaskJob(Task task) {
        this.task = task;
        this.id = task.getId();
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }


    //https://www.hl7.org/fhir/valueset-task-status.html
    public void setStatus(String statusString) {
        this.task.setStatus(TaskStatus.fromCode(statusString));
    }

    public void setStatus(TaskStatus status) {
        this.task.setStatus(status);
    }


    public TaskStatus getStatus() {
        return this.task.getStatus();
    }

    // cap_small :routine, urgent, asap, stat,
    public void setPriority(String priority) {
        this.task.setPriority(TaskPriority.fromCode(priority));
    }

    public void setPriority(TaskPriority priority) {
        this.task.setPriority(priority);
    }

    public TaskPriority getPriority() {
        return task.getPriority();
    }

    // unknown,  proposal, plan, order, originalorder, reflexorder, fillerorder, instanceorder, option,
    public void setIntent(String intent) {
        this.task.setIntent(TaskIntent.fromCode(intent));
    }

    public void setIntent(TaskIntent intent) {
        this.task.setIntent(intent);
    }

    public TaskIntent getIntent() {
        return task.getIntent();
    }

    @Override
    public abstract void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException;
}

