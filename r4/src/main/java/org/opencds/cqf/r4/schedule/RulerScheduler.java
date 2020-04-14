package org.opencds.cqf.r4.schedule;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;



public class RulerScheduler {

    private static final Logger logger = LoggerFactory.getLogger(RulerScheduler.class);

    private Scheduler scheduler;

  //  private ScheduleType type;

    private JobDetail jobDetail;

    private Trigger trigger;

   // private String scheduleParam;

    private BaseTaskJob job;

    private String group;

    private ScheduleExpression scheduleExpression;

    public RulerScheduler(BaseTaskJob job, String group, ScheduleExpression scheduleExpression) throws SchedulerException{
        this.job = job;
        this.group = group;
        this.scheduleExpression = scheduleExpression;
        init();
    }

    private  void init() throws SchedulerException{
        initScheduler();
        initJobDetail();
        initTrigger();
    }

    private void initScheduler() throws SchedulerException {
        SchedulerFactory schedulerFactory = new StdSchedulerFactory();
        scheduler = schedulerFactory.getScheduler();
    }

    private void initJobDetail(){
        jobDetail = JobBuilder.newJob(this.job.getClass())
                .withIdentity(this.job.getId(), group)
                .build();
    }

    private void initTrigger(){
        if(scheduleExpression.getScheduleType().equals(ScheduleType.ONCE)){
            trigger = (SimpleTrigger) TriggerBuilder.newTrigger()
                        .withIdentity("onceTrigger", group)
                        .startAt(scheduleExpression.getStartDate())
                        .forJob(job.getId(), group)
                        .build();


        } else if(scheduleExpression.getScheduleType().equals(ScheduleType.SIMPLE)){
            trigger = TriggerBuilder.newTrigger()
                    .withIdentity("simpleTrigger", group)
                    .startNow()
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                            .withIntervalInSeconds(scheduleExpression.getInterValInSeconds())
                            .repeatForever())
                    .build();

        } else if(scheduleExpression.getScheduleType().equals(ScheduleType.CRON)){   //scheduleParam ex: "0 0/2 8-17 * * ?"
            trigger = TriggerBuilder.newTrigger()
                    .withIdentity("cronTrigger", group)
                    .withSchedule(CronScheduleBuilder.cronSchedule(scheduleExpression.getScheduleExpression()))
                    .forJob(job.getId(), group)
                    .build();
        }
    }

    public void start(){
        try {
            scheduler.start();
            scheduler.scheduleJob(jobDetail, trigger);
        }catch (SchedulerException e) {
            logger.info("Job failed due to exception:"+job.getId()+"|"+group+"|"+e.toString());
        }
    }


    public static enum ScheduleType {
        ONCE,
        SIMPLE,
        CRON
    }
}
