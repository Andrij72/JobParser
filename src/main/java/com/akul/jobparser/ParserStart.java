package com.akul.jobparser;


import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.apache.log4j.Logger;

import java.io.InputStream;
import java.util.Properties;

public class ParserStart {
    private static final Logger logger = Logger.getLogger(ParserStart.class);

    public static void main(String[] args) {
        try (InputStream in = JobParser.class.getClassLoader()
                .getResourceAsStream("app.properties")) {
            Properties config = new Properties();
            config.load(in);

            Scheduler scheduler = new StdSchedulerFactory().getScheduler();

            JobDetail job = JobBuilder.newJob(JobParser.class)
                    .withIdentity("jobParser", "group1")
                    .build();

            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("trigger1", "group1")
                    .withSchedule(CronScheduleBuilder.cronSchedule(config.getProperty("cron.time")))
                    .build();

            scheduler.scheduleJob(job, trigger);
            scheduler.start();
            logger.info("Scheduler started with cron expression: " + config.getProperty("cron.time"));
        } catch (Exception e) {
            logger.error("Error starting the scheduler", e);
        }
    }
}
