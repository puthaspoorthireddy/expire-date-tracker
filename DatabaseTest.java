import java.sql.Connection;
import java.sql.DriverManager;

public class DatabaseTest {

    public static void main(String[] args) {

        String url = "jdbc:mysql://localhost:3306/grocery_reminder";
        String username = "root";
        String password = "Spoorthireddy@12";

        try {
            Connection con = DriverManager.getConnection(
                url, username, password
            );

            System.out.println("Database Connected Successfully!");

            con.close();

        } catch (Exception e) {
            System.out.println("Database Connection Failed!");
            e.printStackTrace();
        }
    }
}