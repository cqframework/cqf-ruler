package org.opencds.cqf.r4.schedule;

import org.opencds.cqf.r4.schedule.RulerScheduler.ScheduleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

//https://docs.oracle.com/cd/E12058_01/doc/doc.1014/e12030/cron_expressions.htm

public class ScheduleExpression {

    private static final Logger logger = LoggerFactory.getLogger(ScheduleExpression.class);

    private ScheduleType scheduleType;

    private StringBuilder expression;

    private Date startDate;

    private int interValInSeconds;

   // private

    public ScheduleExpression(ScheduleType schType){
        scheduleType = schType;
        if(schType.equals(ScheduleType.CRON)){
            expression = new StringBuilder();
        }
    }

    public String getScheduleExpression(){
        return expression.toString();
    }

    public void setExpression(String expression) {
        this.expression.append(expression);
    }

    public ScheduleType getScheduleType() {
        return scheduleType;
    }

    public void setScheduleType(ScheduleType scheduleType) {
        this.scheduleType = scheduleType;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            this.startDate = sdf.parse(startDate);
        } catch (ParseException e) {
            logger.info("Parse Exception:"+e.toString());
        }
    }

    public void setStartDateToNow(){
        startDate = new Date();
    }

    public int getInterValInSeconds() {
        return interValInSeconds;
    }

    public void setInterValInSeconds(int interValInSeconds) {
        this.interValInSeconds = interValInSeconds;
    }

}
