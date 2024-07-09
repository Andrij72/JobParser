package com.akul.jobparser;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;

public class ParserStartTest {
    private static final Logger logger = Logger.getLogger(ParserStart.class);

    public static void main(String[] args) {
        logger.info("Test Parser started");

        try (InputStream in = ParserStartTest.class.getClassLoader()
                .getResourceAsStream("app.properties")) {
            Properties config = new Properties();
            config.load(in);

            String dbUrl = config.getProperty("jdbc.url");
            String dbUsername = config.getProperty("jdbc.username");
            String dbPassword = config.getProperty("jdbc.password");

            JobParser parser = new JobParser();
            parser.setDbUrl(dbUrl);
            parser.setDbUsername(dbUsername);
            parser.setDbPassword(dbPassword);
            parser.setExecutorService(Executors.newFixedThreadPool(10));


           //parser.parseAndSaveJobsFromXing("https://www.xing.com/jobs/java-developer");
           parser.parseAndSaveJobsFromWorkUA("https://www.work.ua/jobs-java/");

           parser.getExecutorService().shutdown();
           parser.getExecutorService().awaitTermination(1, TimeUnit.MINUTES);
        } catch (IOException|InterruptedException e) {
            logger.error("Error during test parser execution", e);
        }

        logger.info("Test Parser finished");
    }
}
