package listener; /**
 * @author : Dhanusha Perera
 * @since : 11/01/2021
 **/

import org.apache.commons.dbcp2.BasicDataSource;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;

@WebListener()
public class ContextListener implements ServletContextListener {

    // Public constructor is required by servlet spec
    public ContextListener() {
    }

    // -------------------------------------------------------
    // ServletContextListener implementation
    // -------------------------------------------------------
    public void contextInitialized(ServletContextEvent sce) {
      /* This method is called when the servlet context is
         initialized(when the Web application is deployed). 
         You can initialize servlet context related data here.
      */
        System.out.println("contextInitialized");

        Properties properties = new Properties();
        try {
            properties.load(this.getClass().getResourceAsStream("/application.properties"));

            BasicDataSource basicDataSource = new BasicDataSource();
            basicDataSource.setUsername(properties.getProperty("mysql.username"));
            basicDataSource.setPassword(properties.getProperty("mysql.password"));
            basicDataSource.setUrl(properties.getProperty("mysql.url"));
            basicDataSource.setDriverClassName(properties.getProperty("mysql.driver_class"));
            basicDataSource.setInitialSize(5);
            basicDataSource.setMaxTotal(5);
            ServletContext ctx = sce.getServletContext();
            ctx.setAttribute("cp", basicDataSource);

        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public void contextDestroyed(ServletContextEvent sce) {
      /* This method is invoked when the Servlet Context 
         (the Web application) is undeployed or 
         Application Server shuts down.
      */
        System.out.println("contextDestroyed");
        BasicDataSource basicDataSource = (BasicDataSource) sce.getServletContext().getAttribute("cp");

        try {
            basicDataSource.close();
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

}
