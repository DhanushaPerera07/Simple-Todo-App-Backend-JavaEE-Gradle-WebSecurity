package filter;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author : Dhanusha Perera
 * @since : 12/01/2021
 **/
@WebFilter(filterName = "CorsFilter")
public class CorsFilter extends HttpFilter {
    /* Since, we extend the CorsFilter class by HttpFilter class
     * We do not need to override all the methods.
     * We can override the method we want like this. */

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse resp, FilterChain chain) throws IOException, ServletException {
        /* Since, we have HttpServletRequest req and HttpServletResponse resp
        We do not have to cast the resp object */
        resp.setHeader("Access-Control-Allow-Origin","http://localhost:4200");
        resp.setHeader("Access-Control-Allow-Methods","GET, PUT, POST, DELETE, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers","Content-Type, Authorization");
        chain.doFilter(req, resp); // in this line; go to the next filter in the filter chain
    }

    /* If we implement CorsFilter class by the Filter Interface,
     * We have to override all the methods like this. */

/*    public void init(FilterConfig config) throws ServletException {

    }


    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws ServletException, IOException {
        HttpServletResponse response = (HttpServletResponse) resp;
        response.setHeader("Access-Control-Allow-Origin", CommonConstants.FRONTEND_URL);
        response.setHeader("Access-Control-Allow-Headers", "Content-Type");
        response.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE");
        chain.doFilter(req, resp); // this is where it runs the next filter/ servlet
        System.out.println("Response");
    }

    public void destroy() {
    }*/
}
