package ru.javaops.masterjava.xml;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.google.common.base.Splitter;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.lang3.StringUtils;
import ru.javaops.masterjava.xml.schema.User;
import ru.javaops.masterjava.xml.util.StaxStreamProcessor;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;

/**
 * Created by alximik on 12/10/16.
 */
@WebServlet("/UserExporter")
public class UserExporter extends HttpServlet {

    private boolean isMultipart;
    private String filePath;
    private int maxFileSize = 100 * 1024 * 1024;
    private int maxMemSize = 1024 * 1024;
    private File file;


    @Override
    public void init() throws ServletException {
        // Get the file location where it would be stored.
        filePath = getServletContext().getInitParameter("file-upload");
    }


    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, java.io.IOException {

        // Check that we have a file upload request
        isMultipart = ServletFileUpload.isMultipartContent(request);
        response.setContentType("text/html");
        java.io.PrintWriter out = response.getWriter();
        if (!isMultipart) {
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Servlet upload</title>");
            out.println("</head>");
            out.println("<body>");
            out.println("<p>No file uploaded</p>");
            out.println("</body>");
            out.println("</html>");
            return;
        }

        // Create a factory for disk-based file items
        DiskFileItemFactory factory = new DiskFileItemFactory();
        // maximum size that will be stored in memory
        factory.setSizeThreshold(maxMemSize);
        // Location to save data that is larger than maxMemSize.
        factory.setRepository(new File("/Users/alximik/Desktop/data/"));
        // Create a new file upload handler
        ServletFileUpload upload = new ServletFileUpload(factory);
        // maximum file size to be uploaded.
        upload.setSizeMax(maxFileSize);

        try {
            List<FileItem> fileItems = upload.parseRequest(request);
            fileItems.stream().filter(fileItem -> !fileItem.isFormField()).forEach(fileItem -> {
                insertUsersToDb(fileItem);
            });

        } catch (Exception ex) {
            System.out.println(ex);
        }
    }

    private void insertUsersToDb(FileItem fileItem) {
        try {
//            Connection connection = getDbConnection();
            Connection connection = createConnectionPool();
            StaxStreamProcessor parser;
            try {
                parser = new StaxStreamProcessor(fileItem.getInputStream());
                while (parser.doUntil(XMLEvent.START_ELEMENT, "User")) {
                    User user = new User();
                    user.setValue(parser.getReader().getElementText());
                    user.setEmail(parser.getAttribute("flag"));
                    user.setEmail(parser.getAttribute("email"));
                    user.setEmail(parser.getAttribute("city"));
                    insertUserToDb(connection, user);
                }

            } catch (XMLStreamException | IOException e) {
                System.err.println("Problem with file");
                e.printStackTrace();
            }

        } catch (SQLException e) {
            System.err.println("Problem with DB");
            e.printStackTrace();
        }


    }

    private Connection createConnectionPool() throws SQLException {
        PoolProperties p = new PoolProperties();
        p.setUrl("jdbc:postgresql://localhost:5432/masterjava");
        p.setDriverClassName("org.postgresql.Driver");
        p.setUsername("postgres");
        p.setPassword("postgres");
        p.setJmxEnabled(true);
        p.setTestWhileIdle(false);
        p.setTestOnBorrow(true);
        p.setValidationQuery("SELECT 1");
        p.setTestOnReturn(false);
        p.setValidationInterval(30000);
        p.setTimeBetweenEvictionRunsMillis(30000);
        p.setMaxActive(100);
        p.setInitialSize(10);
        p.setMaxWait(10000);
        p.setRemoveAbandonedTimeout(60);
        p.setMinEvictableIdleTimeMillis(30000);
        p.setMinIdle(10);
        p.setLogAbandoned(true);
        p.setRemoveAbandoned(true);
        p.setJdbcInterceptors(
                "org.apache.tomcat.jdbc.pool.interceptor.ConnectionState;"+
                        "org.apache.tomcat.jdbc.pool.interceptor.StatementFinalizer");
        DataSource datasource = new DataSource();
        datasource.setPoolProperties(p);
        return datasource.getConnection();
    }

    private Connection getDbConnection() throws SQLException {
        Connection connection = null;
//        connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/masterjava", "postgres", "postgres");


        return connection;
    }

    private void insertUserToDb(Connection connection, User user) {
        String insertTableSQL = "INSERT INTO users(FULL_NAME, FLAG, CITY, EMAIL) VALUES(?,?,?,?,?)";
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(insertTableSQL);
            preparedStatement.setString(1,user.getValue());
            preparedStatement.setString(2, String.valueOf(user.getFlag()));
            preparedStatement.setString(3, String.valueOf(user.getCity()));
            preparedStatement.setString(4, user.getEmail());
            preparedStatement.executeLargeUpdate();
        } catch (SQLException e) {
            System.err.println("Problem with insert user");
            e.printStackTrace();
        }


    }

}
