package api;

import dto.TodoItemDTO;
import dto.UserDTO;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.dbcp2.BasicDataSource;
import util.Priority;
import util.Status;

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
import java.util.List;

/**
 * @author : Dhanusha Perera
 * @since : 11/01/2021
 **/


/*
 * GET /api/v1/items/{I001}
 * POST /api/v1/items
 * PUT /api/v1/items/{id}
 * DELETE /api/v1/items/{id}
 *
 * urlPatterns = /api/v1/items/*
 * */


@WebServlet(name = "ItemServlet", urlPatterns = "/api/v1/items/*")
public class ItemServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Jsonb jsonb = JsonbBuilder.create();
        /* Get the connection pool */
        BasicDataSource basicDataSource = (BasicDataSource) getServletContext().getAttribute("cp");
        /* Set ContentType*/
        response.setContentType("application/json");

        // TODO: this line returns null, check the reason
        System.out.println("test :" + request.getAttribute("user"));

        try {
            PrintWriter out = response.getWriter();

            if (request.getPathInfo() == null || request.getPathInfo().equals("/")) {
                /* SELECT only items of a given username */

                try (Connection connection = basicDataSource.getConnection()) {
                    PreparedStatement pstm = connection.
                            prepareStatement("SELECT * FROM todo_item WHERE username = ?");
                    pstm.setObject(1, request.getAttribute("user"));

                    /* execute the query and get the result set */
                    ResultSet resultSet = pstm.executeQuery();

                    /* Create itemDTO List to store if there is/are Item(s).
                     * add those items to the arrayList */
                    List<TodoItemDTO> itemDTOList = new ArrayList<>();

                    /* Go through with the result set and
                     * add item to item to itemList(array) */
                    while (resultSet.next()) {
                        itemDTOList.add(
                                new TodoItemDTO(
                                        resultSet.getInt(1),
                                        resultSet.getString(2),
                                        Priority.valueOf(resultSet.getString(3)), // Priority(Enum)
                                        Status.valueOf(resultSet.getString(4)), // Status(Enum)
                                        resultSet.getString(5)
                                ));
                    }

                    /* make a JSON object using itemList java object
                     * with the help of Jsonb */
                    out.println(jsonb.toJson(itemDTOList));

                } catch (JsonParsingException exception) {
                    /* when we make the JSON object using Java object, some error occurred.
                     * send an error to the client - 401 BAD_REQUEST */
                    exception.printStackTrace();
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST);
//                return;
                } catch (SQLException exception) {
                    exception.printStackTrace();
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }

            } else {
                /* SELECT * FROM todo_item WHERE id=? AND username=?*/

                try (Connection connection = basicDataSource.getConnection()) {
                    int id = Integer.parseInt(request.getPathInfo().replace("/", ""));
//                request.setAttribute("user","admin");
                    PreparedStatement pstm = connection.
                            prepareStatement("SELECT * FROM todo_item WHERE id=? AND username=?");
                    pstm.setObject(1, id);
                    pstm.setObject(2, request.getAttribute("user"));
                    ResultSet rst = pstm.executeQuery();
                    if (!rst.next()) {
                        response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    } else {
                        response.setContentType("application/json");
                        TodoItemDTO item = new TodoItemDTO(rst.getInt("id"),
                                rst.getString("text"),
                                Priority.valueOf(rst.getString("priority")),
                                Status.valueOf(rst.getString("status")),
                                rst.getString("username"));
                        response.getWriter().println(jsonb.toJson(item));
                    }
                } catch (NumberFormatException numberFormatException) {
                    numberFormatException.printStackTrace();
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                } catch (SQLException exception) {
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    exception.printStackTrace();
                }
            }

        } catch (Exception exception) {
            exception.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

    }// doGet

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws
            ServletException,
            IOException {
        /* Get database connection pool */
        BasicDataSource basicDataSource = (BasicDataSource) getServletContext().getAttribute("cp");
        /* Set ContentType*/
//        response.setContentType("application/json");

        UserDTO user;
        try (Connection connection = basicDataSource.getConnection()) {
//            PrintWriter out = response.getWriter();

            Jsonb jsonb = JsonbBuilder.create();
            user = jsonb.fromJson(request.getReader(), UserDTO.class);

            /* Validation */
            if (user.getUsername().isEmpty() || user.getPassword().isEmpty()) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            /* TODO: if needed password strength validation can be done here */

            PreparedStatement pstm = connection.prepareStatement("INSERT INTO `user` (`username`,`password`) VALUES (?,?);");
            pstm.setObject(1, user.getUsername());
            /* using hash function we can encrypt the password,
             * In order to do that we need to use commons-codec lib from apache
             * Classname is DigestUtils */
            String sha256HexPassword = DigestUtils.sha3_256Hex(user.getPassword());
            pstm.setObject(1, sha256HexPassword);

        } catch (JsonbException exception) {
            exception.printStackTrace();
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
//            return;
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }// doPost


    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws
            ServletException,
            IOException {

        /* catch the header and get the username */
        String username = request.getParameter("username");

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
            pstm.setObject(1, DigestUtils.sha256Hex(user.getUsername()));
            pstm.setObject(2, user.getUsername());
            /* using hash function we can encrypt the password,
             * In order to do that we need to use commons-codec lib from apache
             * Classname is DigestUtils */
            String sha256HexPassword = DigestUtils.sha3_256Hex(user.getPassword());
            pstm.setObject(1, sha256HexPassword);

        } catch (JsonbException exception) {
            exception.printStackTrace();
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    @Override /* Works Fine */
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String itemName = null;
        int itemId = 0;
        if (req.getPathInfo() == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        } else {
            itemName = req.getPathInfo().replace("/", "");

            if (itemId != 0) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            try {
                itemId = Integer.parseInt(itemName);
            } catch (NumberFormatException exception) {
                exception.printStackTrace();
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            System.out.println("itemId: " + itemId);

            BasicDataSource basicDataSource = (BasicDataSource) getServletContext().getAttribute("cp");

            try (Connection connection = basicDataSource.getConnection()) {
                /* Check the user-given username is in the database */
                PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM `todo_item` WHERE `id`=?;");
                preparedStatement.setInt(1, itemId);

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
    }
}
