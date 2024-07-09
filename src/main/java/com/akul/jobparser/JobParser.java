package com.akul.jobparser;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.apache.log4j.Logger;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class JobParser implements Job {
    private static final Logger logger = Logger.getLogger(JobParser.class);

    private String dbUrl;
    private String dbUsername;
    private String dbPassword;
    private ExecutorService executorService;
    private Cache jobCache;

    private List<Vacation> vacationList = new ArrayList<>();

    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public void setDbUrl(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    public void setDbUsername(String dbUsername) {
        this.dbUsername = dbUsername;
    }

    public void setDbPassword(String dbPassword) {
        this.dbPassword = dbPassword;
    }

    public String getDbUrl() {
        return dbUrl;
    }

    public String getDbUsername() {
        return dbUsername;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public JobParser() {
        // Initialize CacheManager and Cache
        CacheManager cacheManager = CacheManager.getInstance();
        jobCache = cacheManager.getCache("jobCache");

        if (jobCache == null) {
            cacheManager.addCache("jobCache");
            jobCache = cacheManager.getCache("jobCache");
        }
    }

    @Override
    public void execute(JobExecutionContext context) {
        logger.info("Parser started");

        try (InputStream in = JobParser.class.getClassLoader()
                .getResourceAsStream("app.properties")) {
            Properties config = new Properties();
            config.load(in);

            dbUrl = config.getProperty("jdbc.url");
            dbUsername = config.getProperty("jdbc.username");
            dbPassword = config.getProperty("jdbc.password");

            executorService = Executors.newFixedThreadPool(10);

            //  parseAndSaveJobsFromXing("https://www.xing.com/jobs/java-developer");
            parseAndSaveJobsFromWorkUA("https://www.work.ua/jobs-java/");

            executorService.shutdown();
            executorService.awaitTermination(1, TimeUnit.MINUTES);
        } catch (IOException | InterruptedException e) {
            logger.error("Error during job execution", e);
        }

        logger.info("Job finished");
    }

    private Map<String, String> loginAndGetCookies(String loginUrl, String username, String password) throws IOException {
        Connection.Response loginForm = Jsoup.connect(loginUrl)
                .method(Connection.Method.GET)
                .execute();

        // Get login form data
        Map<String, String> loginFormData = loginForm.parse().select("form").forms().get(0).formData().stream()
                .collect(Collectors.toMap(e -> e.key(), e -> e.value()));

        loginFormData.put("username", username);
        loginFormData.put("password", password);

        // Post login form
        Connection.Response response = Jsoup.connect(loginUrl)
                .data(loginFormData)
                .cookies(loginForm.cookies())
                .method(Connection.Method.POST)
                .execute();

        return response.cookies();
    }

    void parseAndSaveJobsFromXing(String url) {
        executorService.submit(() -> {
            try (InputStream in = JobParser.class.getClassLoader()
                    .getResourceAsStream("app.properties")) {

                net.sf.ehcache.Element cachedDoc = jobCache.get(url);
                Document doc;
                if (cachedDoc != null) {
                    doc = (Document) cachedDoc.getObjectValue();
                } else {
                    Properties config = new Properties();
                    config.load(in);
                    config.load(new FileInputStream("app.properties"));

                    String username = config.getProperty("xing.username");
                    String password = config.getProperty("xing.password");

                    Map<String, String> cookies = loginAndGetCookies("https://www.xing.com/login", username, password);

                    doc = Jsoup.connect(url)
                            .cookies(cookies)
                            .timeout(10 * 1000)
                            .get();
                    jobCache.put(new net.sf.ehcache.Element(url, doc));
                }

                Elements jobElements = doc.getElementsByTag("li");


                for (Element jobElement : jobElements) {
                    // Получение информации о вакансии
                    String title = jobElement.select("h2 a").text();
                    String salary = jobElement.select(".strong-600").text();
                    String company = jobElement.select(".add-right-xs").text();
                    String jobUrl = "https://www.work.ua" + jobElement.select("h2 a").attr("href");


                    System.out.println("Название: " + title);
                    System.out.println(salary);
                    System.out.println("Компания: " + company);
                    System.out.println("URL: " + jobUrl);
                    System.out.println("----------------------");
                }
               /* if (isValidJavaJob(name, text)) {
                    saveJob(name, text, link);
                }*/
            } catch (IOException e) {
                logger.error("Error while parsing jobs from Xing " + url, e);
            }
        });
    }

    public void parseAndSaveJobsFromWorkUA(String url) {
        executorService.submit(() -> {
            try {
                net.sf.ehcache.Element cachedDoc = jobCache.get(url);
                Document doc;
                if (cachedDoc != null) {
                    doc = (Document) cachedDoc.getObjectValue();
                } else {
                    doc = Jsoup.connect(url).timeout(10 * 1000).get();
                    jobCache.put(new net.sf.ehcache.Element(url, doc));
                }

                Elements jobElements = doc.select(".job-link");

                for (Element jobElement : jobElements) {
                    String title = cleanText(jobElement.select("h2 a").text());
                    String salaryText = cleanText(jobElement.select(".strong-600").text());
                    String company = cleanText(jobElement.select(".add-right-xs").text());
                    String link = "https://www.work.ua" + jobElement.select("h2 a").attr("href");

                    String salary = "Зарплата: не указана";
                    Pattern pattern = Pattern.compile(".*?грн");
                    Matcher matcher = pattern.matcher(salaryText);

                    if (matcher.find()) {
                        salary = "Зарплата: " + matcher.group();
                    }
                    String text = company + " | " + salaryText;
                    System.out.println("Название: " + title);
                    System.out.println(salary);
                    System.out.println("Компания: " + company);
                    System.out.println("URL: " + link);
                    System.out.println("----------------------");

                    if (isValidJavaJob(title, text)) {
                        vacationList.add(new Vacation(title, text, link));
                        logger.info("Found job: " + title + ", Link: " + link);
                    }
                }
                saveJobToDatabase();
            } catch (IOException | SQLException e) {
                logger.error("Error while parsing jobs from WorkUA " + url, e);
            }
        });
    }

    private boolean isValidJavaJob(String name, String text) {
        String lowerCaseName = name.toLowerCase();
        return lowerCaseName.contains("java") && !lowerCaseName.contains("javascript") && !lowerCaseName.contains("java script");
    }

    private void saveJobToDatabase() throws SQLException {
        try (java.sql.Connection conn = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO vacancy (name, text, link) VALUES (?, ?, ?)")) {
            for (Vacation job : vacationList) {
                stmt.setString(1, job.getName());
                stmt.setString(2, job.getText());
                stmt.setString(3, job.getLink());
                stmt.addBatch();
            }
            stmt.executeBatch();
            vacationList.clear();
        }
    }

    private String cleanText(String rawText) {
        if (rawText == null) return "";
        return rawText
                .replace('\u00A0', ' ') // Заменяем неразрывные пробелы обычными
                .replaceAll("&nbsp;", " ") // Заменяем HTML-кодированные неразрывные пробелы
                .replaceAll("[\\p{C}\\p{Z}]", " ") // Убираем другие не видимые и управляющие символы
                .replaceAll("NNBSP|THSP", "") // Убираем известные нежелательные символы
                .trim(); // Убираем пробелы в начале и конце строки
    }

}
