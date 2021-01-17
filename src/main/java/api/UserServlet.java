package api;

import dto.UserDTO;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.dbcp2.BasicDataSource;
import util.AppUtil;

import javax.crypto.SecretKey;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbException;
import javax.json.stream.JsonParsingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author : Dhanusha Perera
 * @since : 11/01/2021
 **/

/*
 * DELETE /api/v1/users/{username}
 * POST /api/v1/users
 * PUT /api/v1/users/{username}
 *
 *
 * urlPatterns = /api/v1/users/*
 * */

@WebServlet(name = "UserServlet", urlPatterns = {"/api/v1/users/*", "/api/v1/auth"})
public class UserServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String username = request.getParameter("username");
        BasicDataSource basicDataSource = (BasicDataSource) getServletContext().getAttribute("cp");
        /* Set ContentType*/
        response.setContentType("application/json");

        UserDTO user;

        try {
            PrintWriter out = response.getWriter();

            try (Connection connection = basicDataSource.getConnection()) {


                PreparedStatement pstm;
                if (request.getServletPath().equals("/api/v1/users") && request.getQueryString().startsWith("q=")) {
                    /* get parameter from the header */
                    String usernameInputByUser = request.getParameter("q");

                    /* check the username in the database and get the username */
                    pstm = connection.prepareStatement("SELECT * FROM `user` WHERE `username`=" + usernameInputByUser + "");

                } else {
                    pstm = connection.prepareStatement("SELECT * FROM `user`" +
                            ((username != null) ? " WHERE `username`=?" : ""));
                    if (username != null) {
                        pstm.setObject(1, username);
                    }
                    ResultSet rst = pstm.executeQuery();

                    List<UserDTO> userDTOList = new ArrayList<>();

                    while (rst.next()) {
                        userDTOList.add(
                                new UserDTO(
                                        rst.getString(1),
                                        rst.getString(2))
                        );
                    }

                    Jsonb jsonb1 = JsonbBuilder.create();
                    out.println(jsonb1.toJson(userDTOList));
                }


            } catch (JsonParsingException exception) {
                exception.printStackTrace();
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            } catch (SQLException exception) {
                exception.printStackTrace();
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    @Override /* WORKING FINE */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Jsonb jsonb = JsonbBuilder.create();

        try {
            /*  Create UserDTO java object by getting JSON user data */
            UserDTO userDTO = jsonb.fromJson(request.getReader(), UserDTO.class);
            /* validation part */
            if (userDTO.getUsername() == null ||
                    userDTO.getPassword() == null ||
                    userDTO.getUsername().trim().isEmpty() ||
                    userDTO.getPassword().trim().isEmpty()) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            /* database connection pool */
            BasicDataSource basicDataSource = (BasicDataSource) getServletContext().getAttribute("cp");

            /* get database connection */
            try (Connection connection = basicDataSource.getConnection()) {
//                PrintWriter out = response.getWriter();
                /* AUTH
                 * POST http://localhost:8080/todolist/api/v1/auth   */
                /* Here, we get the user-given username,
                 * and we try to find a username in the database using that user-given username,
                 * if we find a matching username, then we get the password(encrypted).
                 * then, we verify the user-given password and password(encrypted).
                 *
                 * IF, the verification process successful, then we generate a token for the user.
                 * otherwise, we send "invalid user" or "unauthorized user" */
                if (request.getServletPath().equals("/api/v1/auth")) {
                    // token should be generated here
                    try {
                        PreparedStatement pstm = connection.
                                prepareStatement("SELECT * FROM `user` WHERE username=?");
                        pstm.setObject(1, userDTO.getUsername());
                        ResultSet rst = pstm.executeQuery();
                        if (rst.next()) {
                            String sha256Hex = DigestUtils.sha256Hex(userDTO.getPassword());
                            if (sha256Hex.equals(rst.getString("password"))) {

                                /* Token generated here.
                                 * We need a secret key, so we store the secret key in the application.properties,
                                 * So that we can access that key whenever we need it */
                                SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(AppUtil.getAppSecretKey()));
                                String jws = Jwts.builder()
                                        .setIssuer("ijse")
                                        .setExpiration(new Date(System.currentTimeMillis() + (1000 * 60 * 60 * 24)))
                                        .setIssuedAt(new Date())
                                        .claim("name", userDTO.getUsername())
                                        .signWith(key)
                                        .compact();

                                /* If token is generated successfully,
                                 * We send the JWT(JWS) */
                                response.setContentType("text/plain");
                                response.getWriter().println(jws);

                            } else {
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                            }
                        } else {
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                        }
                    } catch (SQLException exception) {
                        exception.printStackTrace();
                        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    }

                } else {

                    /* Check whether the username is already taken or not,
                     * if username is already taken, then we do not insert a new record.
                     * otherwise, we should create a new user */
                    if (checkUserNameIsTaken(connection, userDTO.getUsername())) {
                        /* if this block runs, that means the username is already taken.
                         * then we do not insert a new record. */
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        response.getWriter().println("User already exists");
                        return;
                    }

                    /* Insert a new user happens here */

                    try {
                        /* INSERT PART */
                        PreparedStatement pstm = connection.prepareStatement("INSERT INTO `user` (`username`,`password`) VALUES (?,?);");
                        pstm.setObject(1, userDTO.getUsername());
                        /* using hash function we can encrypt the password,
                         * In order to do that we need to use commons-codec lib from apache
                         * Classname is DigestUtils */
                        String sha256HexPassword = DigestUtils.sha256Hex(userDTO.getPassword());
                        pstm.setObject(2, sha256HexPassword);

                        if (pstm.executeUpdate() > 0) {
                            response.setStatus(HttpServletResponse.SC_CREATED);
                        }
                    } catch (SQLException exception) {
                        exception.printStackTrace();
                        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        System.out.println("Error (SQLException): user insertion failed..!");
                    }

                }// end-else


            } catch (SQLException exception) {
                exception.printStackTrace();
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }


        } catch (JsonbException exp) {
            /* Token is invalid or token should be expired,
             * then throws a JsonbException */
            exp.printStackTrace();
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }


    }// doPost


    @Override /* WORKING FINE */
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

//        String usernameUrl = "";
//        if (request.getPathInfo() == null) {
//
//        } else {
//            usernameUrl = request.getPathInfo().replace("/", "");
//        }

        /* catch the header and get the username */
//        String usernameFromHeader = request.getParameter("username");

//        if (!usernameUrl.equals(username)){
//            System.out.println("Invalid input: url username != username");
//            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
//            return;
//        }

        BasicDataSource basicDataSource = (BasicDataSource) getServletContext().getAttribute("cp");
        /* Set ContentType*/
//        response.setContentType("application/json");

        UserDTO user;
        try (Connection connection = basicDataSource.getConnection()) {
            /* Printer */
            PrintWriter out = response.getWriter();

            /* Map the JSON to a UserDTO java object */
            Jsonb jsonb = JsonbBuilder.create();
            user = jsonb.fromJson(request.getReader(), UserDTO.class);

            /* Validation */
            if (user.getUsername().isEmpty() || user.getPassword().isEmpty()) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            /* TODO: password strength validation */

            PreparedStatement pstm = connection.prepareStatement(
                    "UPDATE `user` SET `password`=? WHERE username=?;"
            );

            /* using hash function we can encrypt the password,
             * In order to do that we need to use commons-codec lib from apache
             * Classname is DigestUtils */
            String sha256HexPassword = DigestUtils.sha256Hex(user.getPassword());
            pstm.setObject(1, sha256HexPassword); // password
            pstm.setObject(2, user.getUsername().trim()); // username

            if (pstm.executeUpdate() > 0) {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            } else {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }

        } catch (JsonbException exception) {
            exception.printStackTrace();
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        } catch (SQLException exception) {
            exception.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (Exception exception) {
            exception.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }// doPut

    @Override /* WORKING FINE */
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String username = null;
        if (req.getPathInfo() == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        } else {
            username = req.getPathInfo().replace("/", "");
            System.out.println("usernameUrl: " + username);

            BasicDataSource basicDataSource = (BasicDataSource) getServletContext().getAttribute("cp");

            try (Connection connection = basicDataSource.getConnection()) {
                /* Check the user-given username is in the database */

                PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM `user` WHERE `username`=?;");
                preparedStatement.setString(1, username);

                if (preparedStatement.executeUpdate() > 0) {
                    resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
                } else {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                }

            } catch (SQLIntegrityConstraintViolationException exception) {
                exception.printStackTrace();
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            } catch (SQLException exception) {
                exception.printStackTrace();
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }

    } // doDelete

    /**
     * This method will check given username is already in the database
     *
     * @return false : if given username is not taken
     * otherwise: username is already taken
     */
    public boolean checkUserNameIsTaken(Connection connection, String username) {

        try {
            PreparedStatement pstm = connection.prepareStatement("SELECT * FROM `user` WHERE `username`=?;");
            pstm.setObject(1, username);

            ResultSet resultSet = pstm.executeQuery();
            while (resultSet.next()) {
                return true;
            }

        } catch (SQLException exception) {
            exception.printStackTrace();
        }

        return false;
    }
}
