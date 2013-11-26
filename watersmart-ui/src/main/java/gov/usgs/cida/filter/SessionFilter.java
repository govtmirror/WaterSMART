package gov.usgs.cida.filter;

import gov.usgs.cida.tomcatfilters.MyHttpServletRequestWrapper;
import gov.usgs.cida.watersmart.common.ContextConstants;
import gov.usgs.cida.watersmart.common.JNDISingleton;
import gov.usgs.cida.watersmart.ldap.LDAPConnect;
import gov.usgs.cida.watersmart.ldap.LoginMessage;
import gov.usgs.cida.watersmart.ldap.User;
import java.io.IOException;
import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionFilter extends HttpServlet implements Filter {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(
            SessionFilter.class);
    public static final String REDIRECT_ON_FAIL = "redirect_on_fail";
    public static final String APP_AUTH = "X_AUTH_REAL_USER";
    public static final String LOGOUT = "LOGOUT_HOOK";
    public static final String REFRESH = "REFRESH_HOOK";
    public static final String USER_PARAM = "username";
    public static final String PASS_PARAM = "password";
    /**
     * DEVELOPMENT FLAG, TURNS OFF SSL requirement
     */
    public static boolean developmentMode;
    public static User developmentUser;
    public boolean redirectOnFail;
    public static String redirectPage;
    public static String dataSource;
    
    private static final String REDIRECT_PAGE = "redirect_page";
    
    @Override
    public void init(FilterConfig conf) throws ServletException {
        String dev = JNDISingleton.getInstance().getProperty(ContextConstants.DEVELOPMENT, "false");

        if ("true".equals(dev)) {
            //Set Development true
            developmentMode = true;
            developmentUser = User.devUser();
        }
        else {
            //Production Mode
            developmentMode = false;
        }

        redirectOnFail = "true".equalsIgnoreCase(conf.getInitParameter(REDIRECT_ON_FAIL));

        redirectPage = conf.getInitParameter(REDIRECT_PAGE);
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpreq = (HttpServletRequest) req;
        HttpServletResponse httpresp = (HttpServletResponse) resp;
        HttpSession session = httpreq.getSession();
        String redirectPath = httpreq.getContextPath() + "/" + redirectPage;
        
        User sessionUser = (developmentMode && null != developmentUser) ? developmentUser : (User) session.getAttribute(APP_AUTH);
        if (developmentMode && session.getAttribute(APP_AUTH) == null) {
            session.setAttribute(APP_AUTH, developmentUser);
        }
        
        if (null != httpreq.getParameter(LOGOUT)) {
            log.trace("logout: " + sessionUser.uid);
            session.invalidate();
            httpresp.sendRedirect(redirectPath + "?code=" + LoginMessage.LOGOUT);
            return;
        }

        if (null != req.getParameter(REFRESH)) {
            log.trace("refresh: " + sessionUser.uid);
            return;
        }

        // log.debug(httpreq.getRequestURI() + " || SA:" + serverAuth + " AA:" + appAuth + " ID:" + appAuthId + " || SECURE:" + req.isSecure() + " DEV:" + developmentMode);
        
        //HTTPS or dev mode
        if (req.isSecure() || developmentMode) {

            if (null != sessionUser && sessionUser.isAuthenticated()) {
                log.trace(sessionUser.uid + " already logged in");
            }
            else {
                // special case for the redirect page
                if (null != redirectPath && redirectPath.equals(
                        httpreq.getServletPath())) {
                    chain.doFilter(req, resp);
                    return;
                }
                // Where's your credentials? Check the request.
                String user = httpreq.getParameter(USER_PARAM);
                String pass = httpreq.getParameter(PASS_PARAM);

                User userObj = authenticateUser(user, pass);
                if (null == userObj) {
                    if (redirectOnFail) {
                        log.debug("Failed Authentication. Redirecting to login page.");
                        //redirect to nonauthhome.jsp in HTTPS
                        httpresp.sendRedirect(redirectPath + "?code=" + LoginMessage.BAD_PASS);
                    }
                    else {
                        log.debug(
                                "Failed Authentication. Returning empty response.");
                    }
                    return;
                }
                else if (userObj.isAuthenticated()){
                    log.trace("Authentication Passed. Storing user in session.");
                    //Confirm we know this person
                    session.setAttribute(APP_AUTH, userObj);
                }
                else {
                    log.error("User object exists but was not authenticated.");
                    httpresp.sendRedirect(redirectPath + "?code=" + LoginMessage.ERROR);
                    return;
                }
            }
        }
        else {
            if (redirectOnFail) {
                //HTTP
                log.debug("Non-HTTPS protocol. Redirecting to secure page.");
                //redirect to nonauthhome.jsp in HTTPS
                httpresp.sendRedirect(redirectPath + "?code=" + LoginMessage.NOT_HTTPS);
            }
            else {
                log.debug("Non-HTTPS protocol. Returning empty response.");
            }
            return;
        }

        chain.doFilter(req, resp);
    }

    private User authenticateUser(String user, String pass) {
        log.debug("Authenticating " + user);
        User userObj = null;

        if (StringUtils.isNotBlank(user) && StringUtils.isNotBlank(pass)) {
            userObj = LDAPConnect.authenticate(user, pass);
            log.trace("User " + user + " authenticated");
        }
        else {
            log.debug(
                    "User " + user + " was told to authenticate, but does not have a password");
        }

        return userObj;
    }

}
