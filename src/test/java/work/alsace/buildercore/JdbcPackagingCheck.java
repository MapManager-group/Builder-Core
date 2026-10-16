package work.alsace.buildercore;

import java.sql.Driver;
import java.sql.DriverManager;
import java.util.ServiceLoader;

/** 用打包后的 JAR 运行，验证服务声明、MySQL 驱动加载及 SQLite 本地读写。 */
public final class JdbcPackagingCheck {
    public static void main(String[] args) throws Exception {
        var drivers = ServiceLoader.load(Driver.class).stream().map(p -> p.type().getName()).toList();
        assert drivers.contains("com.mysql.cj.jdbc.Driver") : drivers;
        assert drivers.contains("org.sqlite.JDBC") : drivers;
        Class.forName("com.mysql.cj.jdbc.Driver");
        assert DriverManager.getDriver("jdbc:mysql://127.0.0.1/test").acceptsURL("jdbc:mysql://127.0.0.1/test");
        Class.forName("org.sqlite.JDBC");
        try (var connection = DriverManager.getConnection("jdbc:sqlite::memory:");
             var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE check_data (value TEXT)");
            statement.execute("INSERT INTO check_data VALUES ('BuilderCore')");
            try (var result = statement.executeQuery("SELECT value FROM check_data")) {
                assert result.next() && result.getString(1).equals("BuilderCore");
            }
        }
        System.out.println("Packaged JDBC services, MySQL driver and SQLite native read/write passed");
    }
}
