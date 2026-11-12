import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class DbInspect {
    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.err.println("Usage: java DbInspect <url> <user> <pass>");
            System.exit(1);
        }
        String url = args[0];
        String user = args[1];
        String pass = args[2];
        Class.forName("org.postgresql.Driver");
        try (Connection conn = DriverManager.getConnection(url, user, pass);
             Statement stmt = conn.createStatement()) {
            String sql = "select n.nspname, c.relname, conname, pg_get_constraintdef(pc.oid) " +
                         "from pg_constraint pc " +
                         "join pg_class c on pc.conrelid = c.oid " +
                         "join pg_namespace n on c.relnamespace = n.oid " +
                         "where c.relname = 'transaction' and pc.contype = 'c'";
            try (ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    System.out.println(rs.getString(1) + "." + rs.getString(2) + " - " + rs.getString(3) + " -> " + rs.getString(4));
                }
            }
        }
    }
}
