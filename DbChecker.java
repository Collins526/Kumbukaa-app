import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class DbChecker {
    public static void main(String[] args) throws Exception {
        String url = System.getenv("SPRING_DATASOURCE_URL");
        if (url == null) url = "jdbc:postgresql://localhost:5432/postgres"; // guess
        String user = System.getenv("SPRING_DATASOURCE_USERNAME");
        if (user == null) user = "postgres";
        String pass = System.getenv("SPRING_DATASOURCE_PASSWORD");
        if (pass == null) pass = "postgres";

        try (Connection conn = DriverManager.getConnection(url, user, pass);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, email, password_hash, roles FROM app_user")) {
            
            while(rs.next()) {
                System.out.println("ID: " + rs.getLong("id") + ", Email: " + rs.getString("email") + ", Roles: " + rs.getString("roles") + ", Hash: " + rs.getString("password_hash"));
            }
        }
    }
}
