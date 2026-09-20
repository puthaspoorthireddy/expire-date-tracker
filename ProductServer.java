import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Scanner;
import java.nio.file.Files;
import java.nio.file.Paths;
public class ProductServer {

    // =====================================================
    // DATABASE
    // =====================================================

    static final String URL =
            "jdbc:mysql://localhost:3306/grocery_reminder";

    static final String USERNAME =
            "root";

    // IMPORTANT:
    // Put your actual MySQL password here
    static final String PASSWORD =
            "Spoorthireddy@12";


    // =====================================================
    // MAIN
    // =====================================================

    public static void main(String[] args) throws IOException {

        try {

            Connection con =
                    DriverManager.getConnection(
                            URL,
                            USERNAME,
                            PASSWORD
                    );

            System.out.println(
                    "Database connected successfully!"
            );

            con.close();

        } catch (Exception e) {

            System.out.println(
                    "Database connection failed!"
            );

            e.printStackTrace();

            return;
        }


        HttpServer server =
                HttpServer.create(
                        new InetSocketAddress(8080),
                        0
                );


        // =====================================================
        // HOME
        // =====================================================

        server.createContext(
                "/",
                exchange -> {

                    if (exchange.getRequestMethod()
                            .equalsIgnoreCase("GET")) {

                        redirect(
                                exchange,
                                "/dashboard"
                        );

                    } else {

                        sendSimpleResponse(
                                exchange,
                                405,
                                "Method not allowed."
                        );
                    }
                }
        );
         server.createContext("/login.html", exchange -> {

    if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
        sendSimpleResponse(exchange, 405, "Only GET allowed.");
        return;
    }

    try {
        byte[] data = Files.readAllBytes(Paths.get("login.html"));

        exchange.getResponseHeaders().set(
                "Content-Type",
                "text/html; charset=UTF-8"
        );

        exchange.sendResponseHeaders(200, data.length);
        exchange.getResponseBody().write(data);
        exchange.getResponseBody().close();

    } catch (Exception e) {
        e.printStackTrace();
        sendSimpleResponse(exchange, 500, "Could not load login page.");
    }
});
server.createContext("/signup.html", exchange -> {

    if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
        sendSimpleResponse(exchange, 405, "Only GET allowed.");
        return;
    }

    try {
        byte[] data = Files.readAllBytes(Paths.get("signup.html"));

        exchange.getResponseHeaders().set(
                "Content-Type",
                "text/html; charset=UTF-8"
        );

        exchange.sendResponseHeaders(200, data.length);
        exchange.getResponseBody().write(data);
        exchange.getResponseBody().close();

    } catch (Exception e) {
        e.printStackTrace();
        sendSimpleResponse(exchange, 500, "Could not load signup page.");
    }
});
        // =====================================================
        // SIGNUP BACKEND
        // =====================================================

        server.createContext(
                "/signup",
                exchange -> {

                    if (!exchange.getRequestMethod()
                            .equalsIgnoreCase("POST")) {

                        sendSimpleResponse(
                                exchange,
                                405,
                                "Only POST allowed."
                        );

                        return;
                    }


                    try {

                        // -----------------------------------------
                        // Read form data
                        // -----------------------------------------

                        String body =
                                readRequestBody(exchange);


                        String fullName =
                                getFormValue(
                                        body,
                                        "full_name"
                                );

                        String username =
                                getFormValue(
                                        body,
                                        "username"
                                );

                        String email =
                                getFormValue(
                                        body,
                                        "email"
                                );

                        String password =
                                getFormValue(
                                        body,
                                        "password"
                                );


                        // -----------------------------------------
                        // Validation
                        // -----------------------------------------

                        if (
                                fullName.isEmpty() ||
                                username.isEmpty() ||
                                email.isEmpty() ||
                                password.isEmpty()
                        ) {

                            sendSimpleResponse(
                                    exchange,
                                    400,
                                    "Please fill all fields."
                            );

                            return;
                        }


                        if (username.length() < 3) {

                            sendSimpleResponse(
                                    exchange,
                                    400,
                                    "Username must contain at least 3 characters."
                            );

                            return;
                        }


                        if (password.length() < 6) {

                            sendSimpleResponse(
                                    exchange,
                                    400,
                                    "Password must contain at least 6 characters."
                            );

                            return;
                        }


                        // -----------------------------------------
                        // Database connection
                        // -----------------------------------------

                        Connection con =
                                DriverManager.getConnection(
                                        URL,
                                        USERNAME,
                                        PASSWORD
                                );


                        // -----------------------------------------
                        // Check duplicate username/email
                        // -----------------------------------------

                        PreparedStatement check =
                                con.prepareStatement(
                                        "SELECT id FROM users " +
                                        "WHERE username=? OR email=?"
                                );


                        check.setString(
                                1,
                                username
                        );

                        check.setString(
                                2,
                                email
                        );


                        ResultSet rs =
                                check.executeQuery();


                        if (rs.next()) {

                            rs.close();
                            check.close();
                            con.close();


                            sendHtmlResponse(
                                    exchange,
                                    409,
                                    signupErrorPage(
                                            "Username or Email already exists!"
                                    )
                            );

                            return;
                        }


                        rs.close();
                        check.close();


                        // -----------------------------------------
                        // Insert user
                        // -----------------------------------------

                        PreparedStatement ps =
                                con.prepareStatement(
                                        "INSERT INTO users " +
                                        "(full_name, username, email, password) " +
                                        "VALUES (?, ?, ?, ?)"
                                );


                        ps.setString(
                                1,
                                fullName
                        );

                        ps.setString(
                                2,
                                username
                        );

                        ps.setString(
                                3,
                                email
                        );

                        ps.setString(
                                4,
                                password
                        );


                        ps.executeUpdate();


                        ps.close();
                        con.close();


                        // -----------------------------------------
                        // Success
                        // -----------------------------------------

                        sendHtmlResponse(
                                exchange,
                                200,
                                signupSuccessPage(
                                        fullName
                                )
                        );


                    } catch (Exception e) {

                        e.printStackTrace();

                        sendHtmlResponse(
                                exchange,
                                500,
                                signupErrorPage(
                                        "Database Error: " +
                                        e.getMessage()
                                )
                        );
                    }
                }
        );
// =====================================================
// LOGIN BACKEND
// =====================================================

server.createContext(
        "/login",
        exchange -> {

            if (!exchange.getRequestMethod()
                    .equalsIgnoreCase("POST")) {

                sendSimpleResponse(
                        exchange,
                        405,
                        "Only POST allowed."
                );

                return;
            }

            try {

                String body =
                        readRequestBody(exchange);

                String username =
                        getFormValue(
                                body,
                                "username"
                        );

                String password =
                        getFormValue(
                                body,
                                "password"
                        );

                if (username.isEmpty() ||
                    password.isEmpty()) {

                    sendSimpleResponse(
                            exchange,
                            400,
                            "Please enter username and password."
                    );

                    return;
                }


                Connection con =
                        DriverManager.getConnection(
                                URL,
                                USERNAME,
                                PASSWORD
                        );


                PreparedStatement ps =
                        con.prepareStatement(
                                "SELECT id FROM users " +
                                "WHERE username=? AND password=?"
                        );


                ps.setString(1, username);
                ps.setString(2, password);


                ResultSet rs =
                        ps.executeQuery();


                if (rs.next()) {

                    rs.close();
                    ps.close();
                    con.close();

                    // LOGIN SUCCESS
                    redirect(
                            exchange,
                            "/dashboard"
                    );

                } else {

                    rs.close();
                    ps.close();
                    con.close();

                    sendSimpleResponse(
                            exchange,
                            401,
                            "Invalid username or password."
                    );
                }

            } catch (Exception e) {

                e.printStackTrace();

                sendSimpleResponse(
                        exchange,
                        500,
                        "Database Error: " +
                        e.getMessage()
                );
            }
        }
);


        // =====================================================
        // EXPIRED PRODUCTS API
        // =====================================================

        server.createContext(
                "/expired-products",
                exchange -> {

                    if (!exchange.getRequestMethod()
                            .equalsIgnoreCase("GET")) {

                        sendJsonResponse(
                                exchange,
                                405,
                                "{\"error\":\"Only GET allowed\"}"
                        );

                        return;
                    }


                    try {

                        Connection con =
                                DriverManager.getConnection(
                                        URL,
                                        USERNAME,
                                        PASSWORD
                                );


                        PreparedStatement ps =
                                con.prepareStatement(
                                        "SELECT product_name, " +
                                        "expiry_date " +
                                        "FROM products " +
                                        "WHERE expiry_date < CURDATE() " +
                                        "ORDER BY expiry_date ASC"
                                );


                        ResultSet rs =
                                ps.executeQuery();


                        StringBuilder json =
                                new StringBuilder();

                        json.append("[");

                        boolean first = true;


                        while (rs.next()) {

                            if (!first) {
                                json.append(",");
                            }

                            first = false;


                            String name =
                                    rs.getString(
                                            "product_name"
                                    );

                            String date =
                                    rs.getString(
                                            "expiry_date"
                                    );


                            json.append("{");

                            json.append(
                                    "\"product\":\""
                            );

                            json.append(
                                    escapeJson(name)
                            );

                            json.append("\",");

                            json.append(
                                    "\"expiryDate\":\""
                            );

                            json.append(
                                    escapeJson(date)
                            );

                            json.append("\"");

                            json.append("}");
                        }


                        json.append("]");


                        rs.close();
                        ps.close();
                        con.close();


                        sendJsonResponse(
                                exchange,
                                200,
                                json.toString()
                        );


                    } catch (Exception e) {

                        sendJsonResponse(
                                exchange,
                                500,
                                "{\"error\":\"" +
                                escapeJson(
                                        e.getMessage()
                                ) +
                                "\"}"
                        );
                    }
                }
        );


        // =====================================================
        // EXPIRING PRODUCTS API
        // NEXT 60 DAYS
        // =====================================================

        server.createContext(
                "/expiring-products",
                exchange -> {

                    if (!exchange.getRequestMethod()
                            .equalsIgnoreCase("GET")) {

                        sendJsonResponse(
                                exchange,
                                405,
                                "{\"error\":\"Only GET allowed\"}"
                        );

                        return;
                    }


                    try {

                        Connection con =
                                DriverManager.getConnection(
                                        URL,
                                        USERNAME,
                                        PASSWORD
                                );


                        PreparedStatement ps =
                                con.prepareStatement(
                                        "SELECT product_name, " +
                                        "expiry_date, " +
                                        "DATEDIFF(expiry_date,CURDATE()) " +
                                        "AS days_left " +
                                        "FROM products " +
                                        "WHERE DATEDIFF(" +
                                        "expiry_date,CURDATE()" +
                                        ") BETWEEN 0 AND 60 " +
                                        "ORDER BY expiry_date ASC"
                                );


                        ResultSet rs =
                                ps.executeQuery();


                        StringBuilder json =
                                new StringBuilder();

                        json.append("[");

                        boolean first = true;


                        while (rs.next()) {

                            if (!first) {
                                json.append(",");
                            }

                            first = false;


                            String name =
                                    rs.getString(
                                            "product_name"
                                    );

                            String date =
                                    rs.getString(
                                            "expiry_date"
                                    );

                            int days =
                                    rs.getInt(
                                            "days_left"
                                    );


                            json.append("{");

                            json.append(
                                    "\"product\":\""
                            );

                            json.append(
                                    escapeJson(name)
                            );

                            json.append("\",");

                            json.append(
                                    "\"expiryDate\":\""
                            );

                            json.append(
                                    escapeJson(date)
                            );

                            json.append("\",");

                            json.append(
                                    "\"daysLeft\":"
                            );

                            json.append(days);

                            json.append("}");
                        }


                        json.append("]");


                        rs.close();
                        ps.close();
                        con.close();


                        sendJsonResponse(
                                exchange,
                                200,
                                json.toString()
                        );


                    } catch (Exception e) {

                        sendJsonResponse(
                                exchange,
                                500,
                                "{\"error\":\"" +
                                escapeJson(
                                        e.getMessage()
                                ) +
                                "\"}"
                        );
                    }
                }
        );


        // =====================================================
        // PRODUCTS PAGE
        // =====================================================

        server.createContext(
                "/products",
                exchange -> {

                    if (!exchange.getRequestMethod()
                            .equalsIgnoreCase("GET")) {

                        sendSimpleResponse(
                                exchange,
                                405,
                                "Only GET allowed."
                        );

                        return;
                    }


                    StringBuilder html =
                            new StringBuilder();


                    html.append(
                            "<!DOCTYPE html>" +
                            "<html lang='en'>" +
                            "<head>" +
                            "<meta charset='UTF-8'>" +
                            "<meta name='viewport' " +
                            "content='width=device-width," +
                            "initial-scale=1.0'>" +
                            "<title>View Products</title>"
                    );


                    html.append(
                            "<style>" +

                            "*{box-sizing:border-box;}" +

                            "body{" +
                            "margin:0;" +
                            "min-height:100vh;" +
                            "font-family:Arial,Helvetica,sans-serif;" +
                            "color:#263238;" +
                            "overflow-x:hidden;" +

                            "background:" +
                            "radial-gradient(circle at 8% 12%," +
                            "rgba(255,190,205,.45),transparent 24%)," +

                            "radial-gradient(circle at 92% 15%," +
                            "rgba(180,220,255,.48),transparent 25%)," +

                            "radial-gradient(circle at 8% 88%," +
                            "rgba(190,240,205,.45),transparent 26%)," +

                            "radial-gradient(circle at 94% 88%," +
                            "rgba(255,225,170,.48),transparent 26%)," +

                            "linear-gradient(135deg,#fff9fb,#f5f8ff 48%,#f8fff9);" +
                            "}" +

                            ".grocery{" +
                            "position:fixed;" +
                            "z-index:1;" +
                            "font-size:50px;" +
                            "opacity:.48;" +
                            "pointer-events:none;" +
                            "filter:drop-shadow(0 7px 10px rgba(0,0,0,.08));" +
                            "animation:float 8s ease-in-out infinite;" +
                            "}" +

                            ".g1{left:3%;top:16%;}" +
                            ".g2{right:4%;top:12%;}" +
                            ".g3{left:5%;bottom:16%;}" +
                            ".g4{right:5%;bottom:18%;}" +
                            ".g5{left:12%;top:38%;}" +
                            ".g6{right:12%;top:40%;}" +
                            ".g7{left:2%;top:64%;}" +
                            ".g8{right:2%;top:66%;}" +
                            ".g9{left:16%;bottom:6%;}" +
                            ".g10{right:16%;bottom:7%;}" +

                            "@keyframes float{" +
                            "0%,100%{transform:translateY(0);}" +
                            "50%{transform:translateY(-15px);}" +
                            "}" +

                            ".navbar{" +
                            "height:78px;" +
                            "padding:0 7%;" +
                            "display:flex;" +
                            "align-items:center;" +
                            "justify-content:space-between;" +
                            "background:rgba(255,255,255,.84);" +
                            "backdrop-filter:blur(18px);" +
                            "position:sticky;" +
                            "top:0;" +
                            "z-index:100;" +
                            "box-shadow:0 6px 25px rgba(70,80,110,.08);" +
                            "}" +

                            ".brand{" +
                            "display:flex;" +
                            "align-items:center;" +
                            "gap:11px;" +
                            "font-size:21px;" +
                            "font-weight:750;" +
                            "}" +

                            ".brand-icon{" +
                            "width:43px;" +
                            "height:43px;" +
                            "display:flex;" +
                            "align-items:center;" +
                            "justify-content:center;" +
                            "border-radius:13px;" +
                            "background:linear-gradient(135deg,#6366f1,#8b5cf6);" +
                            "color:white;" +
                            "font-size:21px;" +
                            "}" +

                            ".back{" +
                            "text-decoration:none;" +
                            "color:#374151;" +
                            "background:white;" +
                            "border:1px solid #e1e5eb;" +
                            "padding:11px 18px;" +
                            "border-radius:11px;" +
                            "font-weight:650;" +
                            "}" +

                            ".container{" +
                            "position:relative;" +
                            "z-index:2;" +
                            "width:86%;" +
                            "max-width:1150px;" +
                            "margin:auto;" +
                            "padding:48px 0 70px;" +
                            "}" +

                            ".intro{" +
                            "padding:40px 42px;" +
                            "margin-bottom:38px;" +
                            "border-radius:25px;" +
                            "background:linear-gradient(135deg,#4f46e5,#7c3aed 55%,#9333ea);" +
                            "color:white;" +
                            "box-shadow:0 18px 45px rgba(79,70,229,.23);" +
                            "}" +

                            ".intro h1{margin:0;font-size:36px;}" +

                            ".intro p{" +
                            "margin:10px 0 0;" +
                            "color:rgba(255,255,255,.78);" +
                            "}" +

                            ".table-card{" +
                            "background:rgba(255,255,255,.90);" +
                            "backdrop-filter:blur(15px);" +
                            "border-radius:19px;" +
                            "overflow:hidden;" +
                            "box-shadow:0 15px 38px rgba(70,80,110,.10);" +
                            "}" +

                            ".table-scroll{overflow-x:auto;}" +

                            "table{" +
                            "width:100%;" +
                            "min-width:900px;" +
                            "border-collapse:collapse;" +
                            "}" +

                            "th{" +
                            "padding:17px 20px;" +
                            "text-align:left;" +
                            "font-size:11px;" +
                            "color:#6366a3;" +
                            "text-transform:uppercase;" +
                            "letter-spacing:.8px;" +
                            "background:linear-gradient(90deg,#eef2ff,#f4f0ff);" +
                            "}" +

                            "td{" +
                            "padding:19px 20px;" +
                            "font-size:14px;" +
                            "border-bottom:1px solid #eef0f4;" +
                            "background:rgba(255,255,255,.65);" +
                            "}" +

                            "tr:hover td{background:#f8f9ff;}" +

                            ".serial{color:#9da3b0;font-weight:650;}" +

                            ".product-name{font-weight:700;color:#30364d;}" +

                            ".expired-date{color:#d9364f;font-weight:700;}" +

                            ".empty{" +
                            "text-align:center;" +
                            "padding:65px 20px !important;" +
                            "color:#898f9c;" +
                            "}" +

                            "footer{" +
                            "text-align:center;" +
                            "color:#a1a5b1;" +
                            "font-size:12px;" +
                            "padding-bottom:30px;" +
                            "}" +

                            "</style>"
                    );


                    html.append(
                            "</head><body>"
                    );


                    html.append(
                            "<div class='grocery g1'>&#x1F95B;</div>" +
                            "<div class='grocery g2'>&#x1F34E;</div>" +
                            "<div class='grocery g3'>&#x1F955;</div>" +
                            "<div class='grocery g4'>&#x1F966;</div>" +
                            "<div class='grocery g5'>&#x1F35E;</div>" +
                            "<div class='grocery g6'>&#x1F96B;</div>" +
                            "<div class='grocery g7'>&#x1F36A;</div>" +
                            "<div class='grocery g8'>&#x1F9C3;</div>" +
                            "<div class='grocery g9'>&#x1FAD9;</div>" +
                            "<div class='grocery g10'>&#x1F6D2;</div>"
                    );


                    html.append(
                            "<header class='navbar'>" +

                            "<div class='brand'>" +

                            "<div class='brand-icon'>" +
                            "&#x23F3;" +
                            "</div>" +

                            "Expiry Tracker" +

                            "</div>" +

                            "<a class='back' " +
                            "href='/dashboard'>" +
                            "&#x2190; Back to Dashboard" +
                            "</a>" +

                            "</header>"
                    );


                    html.append(
                            "<main class='container'>" +

                            "<section class='intro'>" +

                            "<h1>View Products</h1>" +

                            "<p>" +
                            "All products currently stored " +
                            "in your expiry tracker." +
                            "</p>" +

                            "</section>"
                    );


                    html.append(
                            "<div class='table-card'>" +
                            "<div class='table-scroll'>" +
                            "<table>" +

                            "<thead>" +
                            "<tr>" +
                            "<th>#</th>" +
                            "<th>Product</th>" +
                            "<th>Category</th>" +
                            "<th>Quantity</th>" +
                            "<th>Manufacturing Date</th>" +
                            "<th>Expiry Date</th>" +
                            "<th>Supplier</th>" +
                            "</tr>" +
                            "</thead>" +

                            "<tbody>"
                    );


                    try {

                        Connection con =
                                DriverManager.getConnection(
                                        URL,
                                        USERNAME,
                                        PASSWORD
                                );


                        PreparedStatement ps =
                                con.prepareStatement(
                                        "SELECT id, product_name, " +
                                        "category, quantity, " +
                                        "manufacturing_date, " +
                                        "expiry_date, supplier_name " +
                                        "FROM products " +
                                        "ORDER BY id ASC"
                                );


                        ResultSet rs =
                                ps.executeQuery();


                        int number = 1;


                        while (rs.next()) {

                            int id =
                                    rs.getInt("id");


                            html.append(
                                    "<tr oncontextmenu=" +
                                    "\"showMenu(event," +
                                    id +
                                    ");return false;\">"
                            );


                            html.append(
                                    "<td class='serial'>" +
                                    String.format(
                                            "%02d",
                                            number
                                    ) +
                                    "</td>"
                            );


                            html.append(
                                    "<td class='product-name'>" +
                                    escapeHtml(
                                            rs.getString(
                                                    "product_name"
                                            )
                                    ) +
                                    "</td>"
                            );


                            html.append(
                                    "<td>" +
                                    escapeHtml(
                                            rs.getString(
                                                    "category"
                                            )
                                    ) +
                                    "</td>"
                            );


                            html.append(
                                    "<td>" +
                                    rs.getInt(
                                            "quantity"
                                    ) +
                                    "</td>"
                            );


                            html.append(
                                    "<td>" +
                                    escapeHtml(
                                            rs.getString(
                                                    "manufacturing_date"
                                            )
                                    ) +
                                    "</td>"
                            );


                            html.append(
                                    "<td class='expired-date'>" +
                                    escapeHtml(
                                            rs.getString(
                                                    "expiry_date"
                                            )
                                    ) +
                                    "</td>"
                            );


                            html.append(
                                    "<td>" +
                                    escapeHtml(
                                            rs.getString(
                                                    "supplier_name"
                                            )
                                    ) +
                                    "</td>"
                            );


                            html.append(
                                    "</tr>"
                            );


                            number++;
                        }


                        if (number == 1) {

                            html.append(
                                    "<tr>" +
                                    "<td colspan='7' " +
                                    "class='empty'>" +
                                    "No products found." +
                                    "</td>" +
                                    "</tr>"
                            );
                        }


                        rs.close();
                        ps.close();
                        con.close();


                    } catch (Exception e) {

                        html.append(
                                "<tr>" +
                                "<td colspan='7' " +
                                "class='empty'>" +
                                "Database Error: " +
                                escapeHtml(
                                        e.getMessage()
                                ) +
                                "</td>" +
                                "</tr>"
                        );
                    }


                    html.append(
                            "</tbody>" +
                            "</table>" +
                            "</div>" +
                            "</div>" +
                            "</main>"
                    );


                    // Right click menu

                    html.append(
                            "<div id='menu' " +
                            "style='display:none;" +
                            "position:fixed;" +
                            "background:white;" +
                            "border:1px solid #ddd;" +
                            "border-radius:10px;" +
                            "box-shadow:0 8px 25px rgba(0,0,0,.15);" +
                            "z-index:9999;'>" +

                            "<button onclick='editProduct()' " +
                            "style='display:block;width:140px;" +
                            "padding:12px;border:0;" +
                            "background:white;text-align:left;'>" +
                            "Edit" +
                            "</button>" +

                            "<button onclick='deleteProduct()' " +
                            "style='display:block;width:140px;" +
                            "padding:12px;border:0;" +
                            "background:white;color:#d64545;" +
                            "text-align:left;'>" +
                            "Delete" +
                            "</button>" +

                            "</div>"
                    );


                    html.append(
                            "<script>" +

                            "var selectedId=null;" +

                            "function showMenu(e,id){" +
                            "selectedId=id;" +
                            "var m=document.getElementById('menu');" +
                            "m.style.display='block';" +
                            "m.style.left=e.clientX+'px';" +
                            "m.style.top=e.clientY+'px';" +
                            "}" +

                            "document.addEventListener('click'," +
                            "function(e){" +
                            "var m=document.getElementById('menu');" +
                            "if(!e.target.closest('#menu')){" +
                            "m.style.display='none';" +
                            "}" +
                            "});" +

                            "function editProduct(){" +
                            "if(selectedId==null)return;" +
                            "window.location.href=" +
                            "'/edit-product?id='+selectedId;" +
                            "}" +

                            "function deleteProduct(){" +
                            "if(selectedId==null)return;" +

                            "if(confirm(" +
                            "'Are you sure you want to delete this product?')){" +

                            "fetch('/delete-product'," +
                            "{method:'POST'," +
                            "headers:{" +
                            "'Content-Type':" +
                            "'application/x-www-form-urlencoded'" +
                            "}," +
                            "body:'id='+selectedId})" +

                            ".then(function(r){" +
                            "return r.text();" +
                            "})" +

                            ".then(function(x){" +
                            "alert(x);" +
                            "location.reload();" +
                            "});" +

                            "}" +

                            "}" +

                            "</script>"
                    );


                    html.append(
                            "<footer>" +
                            "Expiry Tracker - Smart Product Expiry Management" +
                            "</footer>"
                    );


                    html.append(
                            "</body></html>"
                    );


                    sendHtmlResponse(
                            exchange,
                            200,
                            html.toString()
                    );
                }
        );

server.createContext(
        "/add-product.html",
        exchange -> {

            if (!exchange.getRequestMethod()
                    .equalsIgnoreCase("GET")) {

                sendSimpleResponse(
                        exchange,
                        405,
                        "Only GET allowed."
                );

                return;
            }

            try {
                byte[] data = Files.readAllBytes(
                        Paths.get("add-product.html")
                );

                exchange.getResponseHeaders().set(
                        "Content-Type",
                        "text/html; charset=UTF-8"
                );

                exchange.sendResponseHeaders(
                        200,
                        data.length
                );

                exchange.getResponseBody().write(data);
                exchange.getResponseBody().close();

            } catch (Exception e) {
                e.printStackTrace();
                sendSimpleResponse(
                        exchange,
                        500,
                        "Could not load add product page."
                );
            }
        }
);
        // =====================================================
        // ADD PRODUCT
        // =====================================================

        server.createContext(
                "/add-product",
                exchange -> {

                    if (!exchange.getRequestMethod()
                            .equalsIgnoreCase("POST")) {

                        sendSimpleResponse(
                                exchange,
                                405,
                                "Only POST allowed."
                        );

                        return;
                    }


                    try {

                        String body =
                                readRequestBody(exchange);


                        String productName =
                                getFormValue(
                                        body,
                                        "product_name"
                                );

                        String category =
                                getFormValue(
                                        body,
                                        "category"
                                );

                        String quantity =
                                getFormValue(
                                        body,
                                        "quantity"
                                );

                        String manufacturingDate =
                                getFormValue(
                                        body,
                                        "manufacturing_date"
                                );

                        String expiryDate =
                                getFormValue(
                                        body,
                                        "expiry_date"
                                );

                        String supplierName =
                                getFormValue(
                                        body,
                                        "supplier_name"
                                );


                        if (
                                productName.isEmpty() ||
                                category.isEmpty() ||
                                quantity.isEmpty() ||
                                manufacturingDate.isEmpty() ||
                                expiryDate.isEmpty() ||
                                supplierName.isEmpty()
                        ) {

                            sendSimpleResponse(
                                    exchange,
                                    400,
                                    "Please fill all fields."
                            );

                            return;
                        }


                        Connection con =
                                DriverManager.getConnection(
                                        URL,
                                        USERNAME,
                                        PASSWORD
                                );


                        PreparedStatement ps =
                                con.prepareStatement(
                                        "INSERT INTO products " +
                                        "(product_name,category," +
                                        "quantity,manufacturing_date," +
                                        "expiry_date,supplier_name) " +
                                        "VALUES (?,?,?,?,?,?)"
                                );


                        ps.setString(
                                1,
                                productName
                        );

                        ps.setString(
                                2,
                                category
                        );

                        ps.setInt(
                                3,
                                Integer.parseInt(quantity)
                        );

                        ps.setString(
                                4,
                                manufacturingDate
                        );

                        ps.setString(
                                5,
                                expiryDate
                        );

                        ps.setString(
                                6,
                                supplierName
                        );


                        ps.executeUpdate();


                        ps.close();
                        con.close();


                        sendHtmlResponse(
                                exchange,
                                200,
                                successPage()
                        );


                    } catch (Exception e) {

                        sendSimpleResponse(
                                exchange,
                                500,
                                "Database Error: " +
                                e.getMessage()
                        );
                    }
                }
        );


        // =====================================================
        // DELETE PRODUCT
        // =====================================================

        server.createContext(
                "/delete-product",
                exchange -> {

                    if (!exchange.getRequestMethod()
                            .equalsIgnoreCase("POST")) {

                        sendSimpleResponse(
                                exchange,
                                405,
                                "Only POST allowed."
                        );

                        return;
                    }


                    try {

                        String body =
                                readRequestBody(exchange);


                        String id =
                                getFormValue(
                                        body,
                                        "id"
                                );


                        if (id.isEmpty()) {

                            sendSimpleResponse(
                                    exchange,
                                    400,
                                    "Product ID missing."
                            );

                            return;
                        }


                        Connection con =
                                DriverManager.getConnection(
                                        URL,
                                        USERNAME,
                                        PASSWORD
                                );


                        PreparedStatement ps =
                                con.prepareStatement(
                                        "DELETE FROM products " +
                                        "WHERE id=?"
                                );


                        ps.setInt(
                                1,
                                Integer.parseInt(id)
                        );


                        int rows =
                                ps.executeUpdate();


                        ps.close();
                        con.close();


                        if (rows > 0) {

                            sendSimpleResponse(
                                    exchange,
                                    200,
                                    "Product deleted successfully!"
                            );

                        } else {

                            sendSimpleResponse(
                                    exchange,
                                    404,
                                    "Product not found!"
                            );
                        }


                    } catch (Exception e) {

                        sendSimpleResponse(
                                exchange,
                                500,
                                "Database Error: " +
                                e.getMessage()
                        );
                    }
                }
        );


        // =====================================================
        // EDIT PRODUCT
        // =====================================================

        server.createContext(
                "/edit-product",
                exchange -> {

                    String query =
                            exchange.getRequestURI()
                                    .getQuery();


                    if (
                            query == null ||
                            !query.startsWith("id=")
                    ) {

                        sendSimpleResponse(
                                exchange,
                                400,
                                "Product ID missing."
                        );

                        return;
                    }


                    int id;


                    try {

                        id =
                                Integer.parseInt(
                                        query.substring(3)
                                );

                    } catch (Exception e) {

                        sendSimpleResponse(
                                exchange,
                                400,
                                "Invalid product ID."
                        );

                        return;
                    }


                    String name = "";
                    String category = "";
                    String quantity = "";
                    String manufacturing = "";
                    String expiry = "";
                    String supplier = "";


                    try {

                        Connection con =
                                DriverManager.getConnection(
                                        URL,
                                        USERNAME,
                                        PASSWORD
                                );


                        PreparedStatement ps =
                                con.prepareStatement(
                                        "SELECT * FROM products " +
                                        "WHERE id=?"
                                );


                        ps.setInt(
                                1,
                                id
                        );


                        ResultSet rs =
                                ps.executeQuery();


                        if (!rs.next()) {

                            rs.close();
                            ps.close();
                            con.close();


                            sendSimpleResponse(
                                    exchange,
                                    404,
                                    "Product not found."
                            );

                            return;
                        }


                        name =
                                rs.getString(
                                        "product_name"
                                );

                        category =
                                rs.getString(
                                        "category"
                                );

                        quantity =
                                String.valueOf(
                                        rs.getInt(
                                                "quantity"
                                        )
                                );

                        manufacturing =
                                rs.getString(
                                        "manufacturing_date"
                                );

                        expiry =
                                rs.getString(
                                        "expiry_date"
                                );

                        supplier =
                                rs.getString(
                                        "supplier_name"
                                );


                        rs.close();
                        ps.close();
                        con.close();


                    } catch (Exception e) {

                        sendSimpleResponse(
                                exchange,
                                500,
                                "Database Error: " +
                                e.getMessage()
                        );

                        return;
                    }


                    sendHtmlResponse(
                            exchange,
                            200,
                            editPage(
                                    id,
                                    name,
                                    category,
                                    quantity,
                                    manufacturing,
                                    expiry,
                                    supplier
                            )
                    );
                }
        );


        // =====================================================
        // UPDATE PRODUCT
        // =====================================================

        server.createContext(
                "/update-product",
                exchange -> {

                    if (!exchange.getRequestMethod()
                            .equalsIgnoreCase("POST")) {

                        sendSimpleResponse(
                                exchange,
                                405,
                                "Only POST allowed."
                        );

                        return;
                    }


                    try {

                        String body =
                                readRequestBody(exchange);


                        String id =
                                getFormValue(
                                        body,
                                        "id"
                                );

                        String name =
                                getFormValue(
                                        body,
                                        "product_name"
                                );

                        String category =
                                getFormValue(
                                        body,
                                        "category"
                                );

                        String quantity =
                                getFormValue(
                                        body,
                                        "quantity"
                                );

                        String manufacturing =
                                getFormValue(
                                        body,
                                        "manufacturing_date"
                                );

                        String expiry =
                                getFormValue(
                                        body,
                                        "expiry_date"
                                );

                        String supplier =
                                getFormValue(
                                        body,
                                        "supplier_name"
                                );


                        Connection con =
                                DriverManager.getConnection(
                                        URL,
                                        USERNAME,
                                        PASSWORD
                                );


                        PreparedStatement ps =
                                con.prepareStatement(
                                        "UPDATE products SET " +
                                        "product_name=?," +
                                        "category=?," +
                                        "quantity=?," +
                                        "manufacturing_date=?," +
                                        "expiry_date=?," +
                                        "supplier_name=? " +
                                        "WHERE id=?"
                                );


                        ps.setString(
                                1,
                                name
                        );

                        ps.setString(
                                2,
                                category
                        );

                        ps.setInt(
                                3,
                                Integer.parseInt(quantity)
                        );

                        ps.setString(
                                4,
                                manufacturing
                        );

                        ps.setString(
                                5,
                                expiry
                        );

                        ps.setString(
                                6,
                                supplier
                        );

                        ps.setInt(
                                7,
                                Integer.parseInt(id)
                        );


                        int rows =
                                ps.executeUpdate();


                        ps.close();
                        con.close();


                        if (rows > 0) {

                            sendHtmlResponse(
                                    exchange,
                                    200,
                                    updateSuccessPage()
                            );

                        } else {

                            sendSimpleResponse(
                                    exchange,
                                    404,
                                    "Product not found."
                            );
                        }


                    } catch (Exception e) {

                        sendSimpleResponse(
                                exchange,
                                500,
                                "Database Error: " +
                                e.getMessage()
                        );
                    }
                }
        );


        // =====================================================
        // DASHBOARD
        // =====================================================

        server.createContext(
                "/dashboard",
                exchange -> {

                    sendHtmlResponse(
                            exchange,
                            200,
                            dashboardPage()
                    );
                }
        );


        // =====================================================
        // START SERVER
        // =====================================================

        server.start();


        System.out.println(
                "======================================"
        );

        System.out.println(
                "Expiry Tracker Server Started!"
        );

        System.out.println(
                "Dashboard:"
        );

        System.out.println(
                "http://localhost:8080/dashboard"
        );

        System.out.println(
                "Products:"
        );

        System.out.println(
                "http://localhost:8080/products"
        );

        System.out.println(
                "Signup Backend:"
        );

        System.out.println(
                "POST http://localhost:8080/signup"
        );

        System.out.println(
                "======================================"
        );
    }


    // =====================================================
    // SIGNUP SUCCESS PAGE
    // =====================================================

    static String signupSuccessPage(
            String fullName
    ) {

        return

                "<!DOCTYPE html>" +

                "<html>" +

                "<head>" +

                "<meta charset='UTF-8'>" +

                "<meta name='viewport' " +
                "content='width=device-width," +
                "initial-scale=1.0'>" +

                "<title>Account Created</title>" +

                "<style>" +

                "body{" +
                "margin:0;" +
                "min-height:100vh;" +
                "display:flex;" +
                "align-items:center;" +
                "justify-content:center;" +
                "font-family:Arial;" +
                "background:" +
                "linear-gradient(135deg," +
                "#fff9fb,#f5f8ff,#f8fff9);" +
                "}" +

                ".box{" +
                "width:90%;" +
                "max-width:520px;" +
                "background:white;" +
                "padding:45px;" +
                "border-radius:25px;" +
                "text-align:center;" +
                "box-shadow:" +
                "0 20px 50px " +
                "rgba(70,80,110,.15);" +
                "}" +

                ".icon{" +
                "width:75px;" +
                "height:75px;" +
                "margin:auto;" +
                "display:flex;" +
                "align-items:center;" +
                "justify-content:center;" +
                "border-radius:22px;" +
                "font-size:35px;" +
                "background:" +
                "linear-gradient(135deg," +
                "#6366f1,#8b5cf6);" +
                "color:white;" +
                "}" +

                "h1{" +
                "color:#30364d;" +
                "}" +

                "p{" +
                "color:#858b9a;" +
                "line-height:1.6;" +
                "}" +

                "a{" +
                "display:inline-block;" +
                "margin-top:20px;" +
                "padding:13px 22px;" +
                "border-radius:11px;" +
                "text-decoration:none;" +
                "font-weight:bold;" +
                "color:white;" +
                "background:" +
                "linear-gradient(135deg," +
                "#6366f1,#7c3aed);" +
                "}" +

                "</style>" +

                "</head>" +

                "<body>" +

                "<div class='box'>" +

                "<div class='icon'>" +
                "&#x2705;" +
                "</div>" +

                "<h1>" +
                "Account Created Successfully!" +
                "</h1>" +

                "<p>" +
                "Welcome, " +
                escapeHtml(fullName) +
                "!<br>" +
                "Your account has been saved successfully." +
                "</p>" +

                "<a href='/login.html'>" +
                "Go to Login" +
                "</a>" +

                "</div>" +

                "</body>" +

                "</html>";
    }


    // =====================================================
    // SIGNUP ERROR PAGE
    // =====================================================

    static String signupErrorPage(
            String message
    ) {

        return

                "<!DOCTYPE html>" +

                "<html>" +

                "<head>" +

                "<meta charset='UTF-8'>" +

                "<meta name='viewport' " +
                "content='width=device-width," +
                "initial-scale=1.0'>" +

                "<title>Signup Error</title>" +

                "<style>" +

                "body{" +
                "margin:0;" +
                "min-height:100vh;" +
                "display:flex;" +
                "align-items:center;" +
                "justify-content:center;" +
                "font-family:Arial;" +
                "background:" +
                "linear-gradient(135deg," +
                "#fff9fb,#f5f8ff,#f8fff9);" +
                "}" +

                ".box{" +
                "width:90%;" +
                "max-width:520px;" +
                "background:white;" +
                "padding:45px;" +
                "border-radius:25px;" +
                "text-align:center;" +
                "box-shadow:" +
                "0 20px 50px " +
                "rgba(70,80,110,.15);" +
                "}" +

                ".icon{" +
                "width:75px;" +
                "height:75px;" +
                "margin:auto;" +
                "display:flex;" +
                "align-items:center;" +
                "justify-content:center;" +
                "border-radius:22px;" +
                "font-size:35px;" +
                "background:#ffe4e8;" +
                "color:#d9364f;" +
                "}" +

                "h1{" +
                "color:#30364d;" +
                "}" +

                "p{" +
                "color:#d9364f;" +
                "line-height:1.6;" +
                "}" +

                "a{" +
                "display:inline-block;" +
                "margin-top:20px;" +
                "padding:13px 22px;" +
                "border-radius:11px;" +
                "text-decoration:none;" +
                "font-weight:bold;" +
                "color:white;" +
                "background:" +
                "linear-gradient(135deg," +
                "#6366f1,#7c3aed);" +
                "}" +

                "</style>" +

                "</head>" +

                "<body>" +

                "<div class='box'>" +

                "<div class='icon'>" +
                "&#x26A0;" +
                "</div>" +

                "<h1>" +
                "Signup Failed" +
                "</h1>" +

                "<p>" +
                escapeHtml(message) +
                "</p>" +

                "<a href='javascript:history.back()'>" +
                "&#x2190; Go Back" +
                "</a>" +

                "</div>" +

                "</body>" +

                "</html>";
    }


    // =====================================================
    // PRODUCT SUCCESS PAGE
    // =====================================================

    static String successPage() {

        return

                "<!DOCTYPE html>" +

                "<html>" +

                "<head>" +

                "<meta charset='UTF-8'>" +

                "<meta name='viewport' " +
                "content='width=device-width," +
                "initial-scale=1.0'>" +

                "<title>Product Added</title>" +

                "<style>" +

                "body{" +
                "margin:0;" +
                "min-height:100vh;" +
                "font-family:Arial;" +
                "display:flex;" +
                "align-items:center;" +
                "justify-content:center;" +
                "background:" +
                "linear-gradient(135deg," +
                "#fff9fb,#f5f8ff,#f8fff9);" +
                "}" +

                ".box{" +
                "width:90%;" +
                "max-width:550px;" +
                "background:white;" +
                "padding:45px;" +
                "border-radius:25px;" +
                "text-align:center;" +
                "box-shadow:" +
                "0 20px 50px " +
                "rgba(70,80,110,.15);" +
                "}" +

                ".icon{" +
                "width:75px;" +
                "height:75px;" +
                "margin:auto;" +
                "border-radius:22px;" +
                "display:flex;" +
                "align-items:center;" +
                "justify-content:center;" +
                "font-size:35px;" +
                "background:" +
                "linear-gradient(135deg," +
                "#6366f1,#8b5cf6);" +
                "color:white;" +
                "}" +

                "h1{color:#30364d;}" +

                "p{color:#858b9a;}" +

                ".buttons{" +
                "display:flex;" +
                "gap:12px;" +
                "justify-content:center;" +
                "flex-wrap:wrap;" +
                "margin-top:25px;" +
                "}" +

                "a{" +
                "text-decoration:none;" +
                "padding:13px 20px;" +
                "border-radius:11px;" +
                "font-weight:bold;" +
                "}" +

                ".dashboard{" +
                "background:linear-gradient(135deg,#6366f1,#7c3aed);" +
                "color:white;" +
                "}" +

                ".products{" +
                "background:#f1f3f8;" +
                "color:#374151;" +
                "}" +

                "</style>" +

                "</head>" +

                "<body>" +

                "<div class='box'>" +

                "<div class='icon'>" +
                "&#x2705;" +
                "</div>" +

                "<h1>" +
                "Product Added Successfully!" +
                "</h1>" +

                "<p>" +
                "Your grocery product has been " +
                "added to the expiry tracker." +
                "</p>" +

                "<div class='buttons'>" +

                "<a class='dashboard' " +
                "href='/dashboard'>" +
                "&#x2190; Back to Dashboard" +
                "</a>" +

                "<a class='products' " +
                "href='/products'>" +
                "View Products &#x2192;" +
                "</a>" +

                "</div>" +

                "</div>" +

                "</body>" +

                "</html>";
    }


    // =====================================================
    // UPDATE SUCCESS
    // =====================================================

    static String updateSuccessPage() {

        return

                "<!DOCTYPE html>" +
                "<html>" +

                "<head>" +

                "<meta charset='UTF-8'>" +

                "<title>Updated</title>" +

                "<style>" +

                "body{" +
                "margin:0;" +
                "min-height:100vh;" +
                "display:flex;" +
                "align-items:center;" +
                "justify-content:center;" +
                "font-family:Arial;" +
                "background:" +
                "linear-gradient(135deg," +
                "#fff9fb,#f5f8ff,#f8fff9);" +
                "}" +

                ".box{" +
                "background:white;" +
                "padding:45px;" +
                "border-radius:25px;" +
                "text-align:center;" +
                "box-shadow:0 20px 50px rgba(0,0,0,.12);" +
                "}" +

                "h1{color:#30364d;}" +

                "a{" +
                "display:inline-block;" +
                "margin-top:20px;" +
                "padding:13px 20px;" +
                "border-radius:11px;" +
                "text-decoration:none;" +
                "color:white;" +
                "background:linear-gradient(135deg,#6366f1,#7c3aed);" +
                "}" +

                "</style>" +

                "</head>" +

                "<body>" +

                "<div class='box'>" +

                "<h1>" +
                "&#x2705; Product Updated Successfully!" +
                "</h1>" +

                "<a href='/products'>" +
                "&#x2190; Back to Products" +
                "</a>" +

                "</div>" +

                "</body>" +

                "</html>";
    }


    // =====================================================
    // EDIT PAGE
    // =====================================================

    static String editPage(
            int id,
            String name,
            String category,
            String quantity,
            String manufacturing,
            String expiry,
            String supplier
    ) {

        return

                "<!DOCTYPE html>" +

                "<html>" +

                "<head>" +

                "<meta charset='UTF-8'>" +

                "<meta name='viewport' " +
                "content='width=device-width," +
                "initial-scale=1.0'>" +

                "<title>Edit Product</title>" +

                "<style>" +

                "body{" +
                "margin:0;" +
                "min-height:100vh;" +
                "font-family:Arial;" +
                "padding:40px 20px;" +
                "background:" +
                "linear-gradient(135deg," +
                "#fff9fb,#f5f8ff,#f8fff9);" +
                "}" +

                ".box{" +
                "max-width:620px;" +
                "margin:auto;" +
                "background:white;" +
                "padding:35px;" +
                "border-radius:25px;" +
                "box-shadow:0 20px 50px rgba(70,80,110,.13);" +
                "}" +

                "h1{color:#30364d;}" +

                "label{" +
                "display:block;" +
                "margin-top:17px;" +
                "font-weight:bold;" +
                "font-size:13px;" +
                "}" +

                "input{" +
                "width:100%;" +
                "padding:13px;" +
                "margin-top:7px;" +
                "border:1px solid #ddd;" +
                "border-radius:10px;" +
                "font-size:14px;" +
                "}" +

                "button{" +
                "width:100%;" +
                "margin-top:25px;" +
                "padding:14px;" +
                "border:0;" +
                "border-radius:11px;" +
                "background:linear-gradient(135deg,#6366f1,#7c3aed);" +
                "color:white;" +
                "font-weight:bold;" +
                "cursor:pointer;" +
                "}" +

                ".back{" +
                "text-decoration:none;" +
                "color:#6366f1;" +
                "font-weight:bold;" +
                "}" +

                "</style>" +

                "</head>" +

                "<body>" +

                "<div class='box'>" +

                "<a class='back' href='/products'>" +
                "&#x2190; Back to Products" +
                "</a>" +

                "<h1>Edit Product</h1>" +

                "<form method='POST' " +
                "action='/update-product'>" +

                "<input type='hidden' " +
                "name='id' value='" +
                id +
                "'>" +

                "<label>Product Name</label>" +

                "<input type='text' " +
                "name='product_name' " +
                "value='" +
                escapeHtml(name) +
                "' required>" +

                "<label>Category</label>" +

                "<input type='text' " +
                "name='category' " +
                "value='" +
                escapeHtml(category) +
                "' required>" +

                "<label>Quantity</label>" +

                "<input type='number' " +
                "name='quantity' " +
                "value='" +
                escapeHtml(quantity) +
                "' required>" +

                "<label>Manufacturing Date</label>" +

                "<input type='date' " +
                "name='manufacturing_date' " +
                "value='" +
                escapeHtml(manufacturing) +
                "' required>" +

                "<label>Expiry Date</label>" +

                "<input type='date' " +
                "name='expiry_date' " +
                "value='" +
                escapeHtml(expiry) +
                "' required>" +

                "<label>Supplier Name</label>" +

                "<input type='text' " +
                "name='supplier_name' " +
                "value='" +
                escapeHtml(supplier) +
                "' required>" +

                "<button type='submit'>" +
                "Save Changes" +
                "</button>" +

                "</form>" +

                "</div>" +

                "</body>" +

                "</html>";
    }


    // =====================================================
    // DASHBOARD
    // =====================================================

    static String dashboardPage() {

        StringBuilder h =
                new StringBuilder();


        h.append(
                "<!DOCTYPE html>" +
                "<html lang='en'>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<meta name='viewport' " +
                "content='width=device-width,initial-scale=1.0'>" +
                "<title>Expiry Tracker</title>"
        );


        h.append(
                "<style>" +

                "*{box-sizing:border-box;}" +

                "body{" +
                "margin:0;" +
                "min-height:100vh;" +
                "font-family:Arial,Helvetica,sans-serif;" +
                "color:#263238;" +
                "overflow-x:hidden;" +

                "background:" +
                "radial-gradient(circle at 8% 12%,rgba(255,190,205,.45),transparent 24%)," +
                "radial-gradient(circle at 92% 15%,rgba(180,220,255,.48),transparent 25%)," +
                "radial-gradient(circle at 8% 88%,rgba(190,240,205,.45),transparent 26%)," +
                "radial-gradient(circle at 94% 88%,rgba(255,225,170,.48),transparent 26%)," +
                "linear-gradient(135deg,#fff9fb,#f5f8ff 48%,#f8fff9);" +
                "}" +

                ".grocery{" +
                "position:fixed;" +
                "z-index:1;" +
                "font-size:52px;" +
                "opacity:.48;" +
                "pointer-events:none;" +
                "filter:drop-shadow(0 7px 10px rgba(0,0,0,.08));" +
                "animation:float 8s ease-in-out infinite;" +
                "}" +

                ".g1{left:3%;top:16%;}" +
                ".g2{right:4%;top:12%;}" +
                ".g3{left:5%;bottom:16%;}" +
                ".g4{right:5%;bottom:18%;}" +
                ".g5{left:12%;top:38%;}" +
                ".g6{right:12%;top:40%;}" +
                ".g7{left:2%;top:64%;}" +
                ".g8{right:2%;top:66%;}" +
                ".g9{left:16%;bottom:6%;}" +
                ".g10{right:16%;bottom:7%;}" +

                "@keyframes float{" +
                "0%,100%{transform:translateY(0);}" +
                "50%{transform:translateY(-15px);}" +
                "}" +

                ".navbar{" +
                "height:78px;" +
                "padding:0 7%;" +
                "display:flex;" +
                "align-items:center;" +
                "justify-content:space-between;" +
                "background:rgba(255,255,255,.84);" +
                "backdrop-filter:blur(18px);" +
                "position:sticky;" +
                "top:0;" +
                "z-index:100;" +
                "box-shadow:0 6px 25px rgba(70,80,110,.08);" +
                "}" +

                ".brand{" +
                "display:flex;" +
                "align-items:center;" +
                "gap:11px;" +
                "font-size:21px;" +
                "font-weight:750;" +
                "}" +

                ".brand-icon{" +
                "width:43px;" +
                "height:43px;" +
                "display:flex;" +
                "align-items:center;" +
                "justify-content:center;" +
                "border-radius:13px;" +
                "background:linear-gradient(135deg,#6366f1,#8b5cf6);" +
                "color:white;" +
                "font-size:21px;" +
                "}" +

                ".nav-buttons{" +
                "display:flex;" +
                "gap:10px;" +
                "}" +

                ".btn{" +
                "text-decoration:none;" +
                "padding:11px 18px;" +
                "border-radius:11px;" +
                "font-size:13px;" +
                "font-weight:650;" +
                "}" +

                ".add{" +
                "color:white;" +
                "background:linear-gradient(135deg,#6366f1,#7c3aed);" +
                "}" +

                ".view{" +
                "color:#374151;" +
                "background:white;" +
                "border:1px solid #e1e5eb;" +
                "}" +

                ".container{" +
                "position:relative;" +
                "z-index:2;" +
                "width:86%;" +
                "max-width:1150px;" +
                "margin:auto;" +
                "padding:48px 0 70px;" +
                "}" +

                ".intro{" +
                "padding:40px 42px;" +
                "margin-bottom:38px;" +
                "border-radius:25px;" +
                "background:linear-gradient(135deg,#4f46e5,#7c3aed 55%,#9333ea);" +
                "color:white;" +
                "box-shadow:0 18px 45px rgba(79,70,229,.23);" +
                "}" +

                ".label{" +
                "display:inline-block;" +
                "padding:6px 12px;" +
                "border-radius:20px;" +
                "background:rgba(255,255,255,.15);" +
                "font-size:11px;" +
                "font-weight:bold;" +
                "text-transform:uppercase;" +
                "letter-spacing:1px;" +
                "}" +

                ".intro h1{" +
                "margin:12px 0 0;" +
                "font-size:36px;" +
                "}" +

                ".intro p{" +
                "margin:9px 0 0;" +
                "color:rgba(255,255,255,.78);" +
                "}" +

                ".section{" +
                "margin-top:30px;" +
                "}" +

                ".section-header{" +
                "display:flex;" +
                "align-items:center;" +
                "justify-content:space-between;" +
                "margin-bottom:17px;" +
                "gap:15px;" +
                "}" +

                ".section-left{" +
                "display:flex;" +
                "align-items:center;" +
                "gap:12px;" +
                "}" +

                ".section-icon{" +
                "width:45px;" +
                "height:45px;" +
                "display:flex;" +
                "align-items:center;" +
                "justify-content:center;" +
                "border-radius:13px;" +
                "font-size:21px;" +
                "background:linear-gradient(135deg,#fff1c9,#ffe1a3);" +
                "}" +

                ".expired-icon{" +
                "background:linear-gradient(135deg,#ffe1e5,#ffc9d0);" +
                "}" +

                ".section-title{" +
                "margin:0;" +
                "font-size:21px;" +
                "}" +

                ".note{" +
                "padding:7px 13px;" +
                "border-radius:20px;" +
                "background:rgba(255,255,255,.78);" +
                "border:1px solid #e8e9ee;" +
                "font-size:12px;" +
                "color:#777d89;" +
                "}" +

                ".card{" +
                "background:rgba(255,255,255,.90);" +
                "backdrop-filter:blur(15px);" +
                "border-radius:19px;" +
                "overflow:hidden;" +
                "box-shadow:0 15px 38px rgba(70,80,110,.10);" +
                "}" +

                "table{" +
                "width:100%;" +
                "border-collapse:collapse;" +
                "}" +

                "th{" +
                "padding:17px 20px;" +
                "text-align:left;" +
                "font-size:11px;" +
                "color:#6366a3;" +
                "text-transform:uppercase;" +
                "background:linear-gradient(90deg,#eef2ff,#f4f0ff);" +
                "}" +

                "td{" +
                "padding:19px 20px;" +
                "font-size:14px;" +
                "border-bottom:1px solid #eef0f4;" +
                "}" +

                ".serial{" +
                "color:#9da3b0;" +
                "font-weight:bold;" +
                "}" +

                ".product-name{" +
                "font-weight:bold;" +
                "color:#30364d;" +
                "}" +

                ".urgent{" +
                "display:inline-flex;" +
                "padding:7px 13px;" +
                "border-radius:20px;" +
                "background:#ffe4e8;" +
                "color:#d9364f;" +
                "font-weight:bold;" +
                "font-size:12px;" +
                "}" +

                ".warning{" +
                "display:inline-flex;" +
                "padding:7px 13px;" +
                "border-radius:20px;" +
                "background:#fff1d4;" +
                "color:#bd7200;" +
                "font-weight:bold;" +
                "font-size:12px;" +
                "}" +

                ".safe{" +
                "display:inline-flex;" +
                "padding:7px 13px;" +
                "border-radius:20px;" +
                "background:#def8e6;" +
                "color:#287a45;" +
                "font-weight:bold;" +
                "font-size:12px;" +
                "}" +

                ".empty{" +
                "text-align:center;" +
                "padding:55px 20px !important;" +
                "color:#898f9c;" +
                "}" +

                "footer{" +
                "text-align:center;" +
                "color:#a1a5b1;" +
                "font-size:12px;" +
                "padding-bottom:30px;" +
                "}" +

                "</style>"
        );


        h.append(
                "</head><body>"
        );


        h.append(
                "<div class='grocery g1'>&#x1F95B;</div>" +
                "<div class='grocery g2'>&#x1F34E;</div>" +
                "<div class='grocery g3'>&#x1F955;</div>" +
                "<div class='grocery g4'>&#x1F966;</div>" +
                "<div class='grocery g5'>&#x1F35E;</div>" +
                "<div class='grocery g6'>&#x1F96B;</div>" +
                "<div class='grocery g7'>&#x1F36A;</div>" +
                "<div class='grocery g8'>&#x1F9C3;</div>" +
                "<div class='grocery g9'>&#x1FAD9;</div>" +
                "<div class='grocery g10'>&#x1F6D2;</div>"
        );


        h.append(
                "<header class='navbar'>" +

                "<div class='brand'>" +

                "<div class='brand-icon'>" +
                "&#x23F3;" +
                "</div>" +

                "Expiry Tracker" +

                "</div>" +

                "<div class='nav-buttons'>" +

                "<a class='btn add' " +
                "href='/add-product.html'>" +
                "+ Add Product" +
                "</a>" +

                "<a class='btn view' " +
                "href='/products'>" +
                "View Products &#x2192;" +
                "</a>" +

                "</div>" +

                "</header>"
        );


        h.append(
                "<main class='container'>" +

                "<section class='intro'>" +

                "<span class='label'>" +
                "Grocery Inventory" +
                "</span>" +

                "<h1>Expiry Tracker</h1>" +

                "<p>" +
                "Keep your groceries fresh and " +
                "never miss an expiry date." +
                "</p>" +

                "</section>"
        );


        // =====================================================
        // EXPIRED
        // =====================================================

        h.append(
                "<section class='section'>" +

                "<div class='section-header'>" +

                "<div class='section-left'>" +

                "<div class='section-icon expired-icon'>" +
                "&#x1F534;" +
                "</div>" +

                "<h2 class='section-title'>" +
                "Already Expired" +
                "</h2>" +

                "</div>" +

                "<span class='note' " +
                "style='color:#d9364f;'>" +
                "Products past expiry date" +
                "</span>" +

                "</div>" +

                "<div class='card'>" +

                "<table>" +

                "<thead>" +

                "<tr>" +
                "<th>#</th>" +
                "<th>Product Name</th>" +
                "<th>Status</th>" +
                "</tr>" +

                "</thead>" +

                "<tbody>"
        );


        try {

            Connection con =
                    DriverManager.getConnection(
                            URL,
                            USERNAME,
                            PASSWORD
                    );


            PreparedStatement ps =
                    con.prepareStatement(
                            "SELECT product_name " +
                            "FROM products " +
                            "WHERE expiry_date < CURDATE() " +
                            "ORDER BY expiry_date ASC"
                    );


            ResultSet rs =
                    ps.executeQuery();


            int count = 1;


            while (rs.next()) {

                h.append(
                        "<tr>" +

                        "<td class='serial'>" +
                        String.format(
                                "%02d",
                                count
                        ) +
                        "</td>" +

                        "<td class='product-name'>" +
                        escapeHtml(
                                rs.getString(
                                        "product_name"
                                )
                        ) +
                        "</td>" +

                        "<td>" +

                        "<span class='urgent'>" +
                        "Expired" +
                        "</span>" +

                        "</td>" +

                        "</tr>"
                );


                count++;
            }


            if (count == 1) {

                h.append(
                        "<tr>" +

                        "<td colspan='3' " +
                        "class='empty'>" +

                        "No expired products." +

                        "</td>" +

                        "</tr>"
                );
            }


            rs.close();
            ps.close();
            con.close();


        } catch (Exception e) {

            h.append(
                    "<tr>" +

                    "<td colspan='3' " +
                    "class='empty'>" +

                    "Unable to load expired products." +

                    "</td>" +

                    "</tr>"
            );
        }


        h.append(
                "</tbody>" +
                "</table>" +
                "</div>" +
                "</section>"
        );


        // =====================================================
        // EXPIRING SOON
        // =====================================================

        h.append(
                "<section class='section'>" +

                "<div class='section-header'>" +

                "<div class='section-left'>" +

                "<div class='section-icon'>" +
                "&#x23F0;" +

                "</div>" +

                "<h2 class='section-title'>" +
                "Expiring Soon" +
                "</h2>" +

                "</div>" +

                "<span class='note'>" +
                "Within the next 60 days" +
                "</span>" +

                "</div>" +

                "<div class='card'>" +

                "<table>" +

                "<thead>" +

                "<tr>" +
                "<th>#</th>" +
                "<th>Product Name</th>" +
                "<th>Days Left</th>" +
                "</tr>" +

                "</thead>" +

                "<tbody>"
        );


        try {

            Connection con =
                    DriverManager.getConnection(
                            URL,
                            USERNAME,
                            PASSWORD
                    );


            PreparedStatement ps =
                    con.prepareStatement(
                            "SELECT product_name, " +
                            "DATEDIFF(expiry_date,CURDATE()) " +
                            "AS days_left " +
                            "FROM products " +
                            "WHERE DATEDIFF(" +
                            "expiry_date,CURDATE()" +
                            ") BETWEEN 0 AND 60 " +
                            "ORDER BY expiry_date ASC"
                    );


            ResultSet rs =
                    ps.executeQuery();


            int count = 1;


            while (rs.next()) {

                int days =
                        rs.getInt(
                                "days_left"
                        );


                String badge =
                        "safe";


                if (days <= 7) {

                    badge =
                            "urgent";

                } else if (days <= 30) {

                    badge =
                            "warning";
                }


                String text =
                        days == 1
                        ? "1 day"
                        : days + " days";


                h.append(
                        "<tr>" +

                        "<td class='serial'>" +
                        String.format(
                                "%02d",
                                count
                        ) +
                        "</td>" +

                        "<td class='product-name'>" +
                        escapeHtml(
                                rs.getString(
                                        "product_name"
                                )
                        ) +
                        "</td>" +

                        "<td>" +

                        "<span class='" +
                        badge +
                        "'>" +

                        text +

                        "</span>" +

                        "</td>" +

                        "</tr>"
                );


                count++;
            }


            if (count == 1) {

                h.append(
                        "<tr>" +

                        "<td colspan='3' " +
                        "class='empty'>" +

                        "No products are expiring " +
                        "within the next 60 days." +

                        "</td>" +

                        "</tr>"
                );
            }


            rs.close();
            ps.close();
            con.close();


        } catch (Exception e) {

            h.append(
                    "<tr>" +

                    "<td colspan='3' " +
                    "class='empty'>" +

                    "Unable to load expiry data." +

                    "</td>" +

                    "</tr>"
            );
        }


        h.append(
                "</tbody>" +
                "</table>" +
                "</div>" +
                "</section>" +

                "</main>" +

                "<footer>" +

                "Expiry Tracker - " +
                "Smart Product Expiry Management" +

                "</footer>" +

                "</body>" +

                "</html>"
        );


        return h.toString();
    }


    // =====================================================
    // READ REQUEST BODY
    // =====================================================

    static String readRequestBody(
            HttpExchange exchange
    ) throws IOException {

        InputStream input =
                exchange.getRequestBody();


        Scanner scanner =
                new Scanner(
                        input,
                        StandardCharsets.UTF_8.name()
                ).useDelimiter("\\A");


        String body =
                scanner.hasNext()
                ? scanner.next()
                : "";


        scanner.close();


        return body;
    }


    // =====================================================
    // FORM VALUE
    // =====================================================

    static String getFormValue(
            String body,
            String wantedKey
    ) throws Exception {

        String[] fields =
                body.split("&");


        for (String field : fields) {

            String[] pair =
                    field.split(
                            "=",
                            2
                    );


            if (pair.length < 2) {
                continue;
            }


            String key =
                    URLDecoder.decode(
                            pair[0],
                            "UTF-8"
                    );


            String value =
                    URLDecoder.decode(
                            pair[1],
                            "UTF-8"
                    );


            if (key.equals(wantedKey)) {

                return value;
            }
        }


        return "";
    }


    // =====================================================
    // REDIRECT
    // =====================================================

    static void redirect(
            HttpExchange exchange,
            String location
    ) throws IOException {

        exchange.getResponseHeaders()
                .set(
                        "Location",
                        location
                );


        exchange.sendResponseHeaders(
                302,
                -1
        );


        exchange.close();
    }


    // =====================================================
    // HTML RESPONSE
    // =====================================================

    static void sendHtmlResponse(
            HttpExchange exchange,
            int status,
            String text
    ) throws IOException {

        byte[] data =
                text.getBytes(
                        StandardCharsets.UTF_8
                );


        exchange.getResponseHeaders()
                .set(
                        "Content-Type",
                        "text/html; charset=UTF-8"
                );


        exchange.sendResponseHeaders(
                status,
                data.length
        );


        exchange.getResponseBody()
                .write(data);


        exchange.getResponseBody()
                .close();
    }


    // =====================================================
    // JSON RESPONSE
    // =====================================================

    static void sendJsonResponse(
            HttpExchange exchange,
            int status,
            String text
    ) throws IOException {

        byte[] data =
                text.getBytes(
                        StandardCharsets.UTF_8
                );


        exchange.getResponseHeaders()
                .set(
                        "Access-Control-Allow-Origin",
                        "*"
                );


        exchange.getResponseHeaders()
                .set(
                        "Content-Type",
                        "application/json; charset=UTF-8"
                );


        exchange.sendResponseHeaders(
                status,
                data.length
        );


        exchange.getResponseBody()
                .write(data);


        exchange.getResponseBody()
                .close();
    }


    // =====================================================
    // SIMPLE RESPONSE
    // =====================================================

    static void sendSimpleResponse(
            HttpExchange exchange,
            int status,
            String text
    ) throws IOException {

        byte[] data =
                text.getBytes(
                        StandardCharsets.UTF_8
                );


        exchange.getResponseHeaders()
                .set(
                        "Content-Type",
                        "text/plain; charset=UTF-8"
                );


        exchange.sendResponseHeaders(
                status,
                data.length
        );


        exchange.getResponseBody()
                .write(data);


        exchange.getResponseBody()
                .close();
    }


    // =====================================================
    // ESCAPE HTML
    // =====================================================

    static String escapeHtml(
            String text
    ) {

        if (text == null) {
            return "";
        }


        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }


    // =====================================================
    // ESCAPE JSON
    // =====================================================

    static String escapeJson(
            String text
    ) {

        if (text == null) {
            return "";
        }


        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}