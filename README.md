### Vacancy Parser
  #### General Description

The parser application should visit the website www.work.ua in the job section and collect Java vacancies.

_Task_:

1. Implement a data collection and analysis module from www.work.ua .

2. The system should use Jsoup for page parsing.

3. The system should run once a day.

To do this, use the Quartz library.

http://www.quartz-scheduler.org/documentation/quartz-2.x/tutorials/crontrigger.html

4. The system should collect data only about Java vacancies. It should be noted that JavaScript does not fit, nor does Java Script.

5. The data should be stored in a database.

The database should have a table named vacancy (id, name, text, link).

id - primary key
name - job name
text - job description
link - text, link to the vacancy
6. Consider duplicates. Vacancies with the same name are considered duplicates.

7. Consider the last run time. If this is the first run, then all announcements from the beginning of the year should be collected.

8. There should be no output or input of information in the system. All settings should be in the file app.properties.

https://docs.oracle.com/javase/tutorial/essential/environment/properties.html

In the app.properties file, specify the database settings and the frequency of application runs.

###### jdbc.driver =.. 
######  jdbc.url=...
###### jdbc.username=...
###### jdbc.password=...
###### cron.time=...
9. Use the log4j logger to output the necessary information.

10. Example of starting the application.

java -jar SqlRuParser app.properties