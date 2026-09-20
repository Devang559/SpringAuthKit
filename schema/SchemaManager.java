package io.github.devang559.authkit.schema;

import io.github.devang559.authkit.config.AuthKitConfig;
import io.github.devang559.authkit.exception.MigrationRequiredException;
import io.github.devang559.authkit.exception.SchemaMismatchException;
import io.github.devang559.authkit.user.UserField;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class SchemaManager implements AuthKitSchemaManager {

    private static final List<ExpectedColumn> AUTH_USER_COLUMNS = List.of(
            new ExpectedColumn("id", JavaType.UUID, null, false, null),
            new ExpectedColumn("username", JavaType.VARCHAR, 100, true, UserField.USERNAME),
            new ExpectedColumn("email", JavaType.VARCHAR, 254, true, UserField.EMAIL),
            new ExpectedColumn("phone", JavaType.VARCHAR, 30, true, UserField.PHONE),
            new ExpectedColumn("password", JavaType.VARCHAR, 255, false, null),
            new ExpectedColumn("first_name", JavaType.VARCHAR, 100, true, UserField.FIRST_NAME),
            new ExpectedColumn("last_name", JavaType.VARCHAR, 100, true, UserField.LAST_NAME),
            new ExpectedColumn("display_name", JavaType.VARCHAR, 150, true, UserField.DISPLAY_NAME),
            new ExpectedColumn("avatar", JavaType.VARCHAR, 500, true, UserField.AVATAR),
            new ExpectedColumn("email_verified", JavaType.BOOLEAN, null, false, null),
            new ExpectedColumn("phone_verified", JavaType.BOOLEAN, null, false, null),
            new ExpectedColumn("enabled", JavaType.BOOLEAN, null, false, null),
            new ExpectedColumn("account_status", JavaType.VARCHAR, 30, false, null),
            new ExpectedColumn("failed_login_attempts", JavaType.INTEGER, null, true, null),
            new ExpectedColumn("locked_until", JavaType.TIMESTAMP, null, true, null),
            new ExpectedColumn("created_at", JavaType.TIMESTAMP, null, false, null),
            new ExpectedColumn("updated_at", JavaType.TIMESTAMP, null, false, null)
    );

    private static final Set<String> ALL_AUTHKIT_TABLES = Set.of(
            "auth_users", "auth_roles", "auth_permissions", "auth_user_roles",
            "auth_role_permissions", "auth_refresh_tokens", "auth_otps", "auth_audit_events"
    );

    private static final String[] SUPPORTING_TABLE_DDL = {
            "CREATE TABLE IF NOT EXISTS auth_permissions (id %%UUID%% NOT NULL PRIMARY KEY, name VARCHAR(100) NOT NULL, CONSTRAINT uk_auth_permission_name UNIQUE (name))",
            "CREATE TABLE IF NOT EXISTS auth_roles (id %%UUID%% NOT NULL PRIMARY KEY, name VARCHAR(100) NOT NULL, CONSTRAINT uk_auth_role_name UNIQUE (name))",
            "CREATE TABLE IF NOT EXISTS auth_user_roles (user_id %%UUID%% NOT NULL, role_id %%UUID%% NOT NULL, PRIMARY KEY (user_id, role_id))",
            "CREATE TABLE IF NOT EXISTS auth_role_permissions (role_id %%UUID%% NOT NULL, permission_id %%UUID%% NOT NULL, PRIMARY KEY (role_id, permission_id))",
            "CREATE TABLE IF NOT EXISTS auth_refresh_tokens (id %%UUID%% NOT NULL PRIMARY KEY, user_id %%UUID%% NOT NULL, token_hash VARCHAR(255) NOT NULL, issued_at TIMESTAMP NOT NULL, expires_at TIMESTAMP NOT NULL, revoked BOOLEAN NOT NULL, revoked_at TIMESTAMP, replaced_by %%UUID%%, created_at TIMESTAMP NOT NULL)",
            "CREATE TABLE IF NOT EXISTS auth_otps (id %%UUID%% NOT NULL PRIMARY KEY, user_id %%UUID%% NOT NULL, purpose VARCHAR(30) NOT NULL, destination VARCHAR(254) NOT NULL, code_hash VARCHAR(255) NOT NULL, created_at TIMESTAMP NOT NULL, expires_at TIMESTAMP NOT NULL, attempts INTEGER NOT NULL, used BOOLEAN NOT NULL, verified BOOLEAN NOT NULL)",
            "CREATE TABLE IF NOT EXISTS auth_audit_events (id %%UUID%% NOT NULL PRIMARY KEY, event_type VARCHAR(50) NOT NULL, user_id %%UUID%%, identifier VARCHAR(254), client_ip VARCHAR(64), user_agent VARCHAR(1000), success BOOLEAN NOT NULL, details VARCHAR, created_at TIMESTAMP NOT NULL)"
    };

    private final AuthKitConfig config;
    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public SchemaManager(AuthKitConfig config, DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.config = config;
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public SchemaState inspect() {
        DbProduct product = detectProduct();
        Set<String> tables = new HashSet<>();
        Map<String, SchemaState.ColumnMeta> columns = new HashMap<>();
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getTables(null, null, null, new String[]{"TABLE"})) {
                while (rs.next()) {
                    String name = rs.getString("TABLE_NAME").toUpperCase(Locale.ROOT);
                    tables.add(name);
                }
            }
            String actualTable = findTable(meta, "auth_users");
            if (actualTable != null) {
                try (ResultSet rs = meta.getColumns(null, null, actualTable, null)) {
                    while (rs.next()) {
                        String colName = rs.getString("COLUMN_NAME").toLowerCase(Locale.ROOT);
                        String typeName = rs.getString("TYPE_NAME").toUpperCase(Locale.ROOT);
                        int nullable = rs.getInt("NULLABLE");
                        columns.put(colName, new SchemaState.ColumnMeta(colName, typeName,
                                nullable == DatabaseMetaData.columnNullable));
                    }
                }
            }
        } catch (SQLException e) {
            throw new SchemaMismatchException("Failed to inspect database schema: " + e.getMessage());
        }
        return new SchemaState(product, columns, tables);
    }

    @Override
    public SchemaDiff compare() {
        SchemaState state = inspect();
        Set<String> missingColumns = new HashSet<>();
        Set<String> disabledPresent = new HashSet<>();
        Set<String> unexpected = new HashSet<>();
        Set<String> typeMismatches = new HashSet<>();
        Set<String> missingTables = new HashSet<>();

        for (String table : ALL_AUTHKIT_TABLES) {
            if (!state.existingTables().contains(table.toUpperCase(Locale.ROOT))) {
                missingTables.add(table);
            }
        }
        for (ExpectedColumn expected : AUTH_USER_COLUMNS) {
            SchemaState.ColumnMeta actual = state.authUserColumns().get(expected.name());
            if (actual == null) {
                missingColumns.add(expected.name());
                continue;
            }
            if (expected.field() != null && !config.isFieldEnabled(expected.field())) {
                disabledPresent.add(expected.name());
            }
            if (!isTypeCompatible(expected, actual.type())) {
                typeMismatches.add(expected.name() + " (expected " + javaTypeToSql(expected)
                        + ", found " + actual.type() + ")");
            }
        }
        for (String actualCol : state.authUserColumns().keySet()) {
            if (AUTH_USER_COLUMNS.stream().noneMatch(c -> c.name().equals(actualCol))) {
                unexpected.add(actualCol);
            }
        }
        return new SchemaDiff(missingColumns, disabledPresent, unexpected, typeMismatches, missingTables);
    }

    @Override
    public void validate() {
        SchemaDiff diff = compare();
        if (config.getProperties().getSchema().getMode() == io.github.devang559.authkit.config.SchemaMode.EXTERNAL) {
            if (!diff.missingTables().isEmpty()) {
                throw new MigrationRequiredException(
                        "Schema validation failed. Missing authkit tables: " + diff.missingTables()
                                + ". Run your migration tool with the authkit schema scripts.");
            }
            if (!diff.missingColumns().isEmpty()) {
                throw new MigrationRequiredException(
                        "Schema validation failed. Missing columns in auth_users: " + diff.missingColumns()
                                + ". Apply the required migration and try again.");
            }
            if (!diff.typeMismatches().isEmpty()) {
                throw new SchemaMismatchException(
                        "Schema validation failed. Incompatible column types: " + diff.typeMismatches());
            }
            if (!diff.disabledPresent().isEmpty()) {
                log.warn("AuthKit managed columns exist for disabled user fields (safe to remove manually): {}",
                        diff.disabledPresent());
            }
            if (!diff.unexpectedColumns().isEmpty()) {
                log.warn("Unexpected columns found in auth_users: {}", diff.unexpectedColumns());
            }
            log.info("AuthKit schema validation (external mode) complete.");
            return;
        }

        if (!diff.missingTables().isEmpty()) {
            log.info("AuthKit MANAGED mode: creating missing tables {}", diff.missingTables());
            createMissingTables(detectProduct());
        }
        if (!diff.missingColumns().isEmpty()) {
            log.info("AuthKit MANAGED mode: adding missing columns {}", diff.missingColumns());
            addMissingColumns(diff.missingColumns(), detectProduct());
        }
        if (!diff.disabledPresent().isEmpty()) {
            log.warn("AuthKit MANAGED mode: columns exist for disabled fields. " +
                            "AuthKit never drops columns automatically. " +
                            "Review and remove manually if desired: {}",
                    diff.disabledPresent());
        }
        log.info("AuthKit schema (managed mode) is up to date.");
    }

    private String findTable(DatabaseMetaData meta, String name) throws SQLException {
        String upper = name.toUpperCase(Locale.ROOT);
        try (ResultSet rs = meta.getTables(null, null, name, new String[]{"TABLE"})) {
            if (rs.next()) {
                return rs.getString("TABLE_NAME");
            }
        }
        try (ResultSet rs = meta.getTables(null, null, upper, new String[]{"TABLE"})) {
            if (rs.next()) {
                return rs.getString("TABLE_NAME");
            }
        }
        // fall back to scanning all tables
        try (ResultSet rs = meta.getTables(null, null, null, new String[]{"TABLE"})) {
            while (rs.next()) {
                if (rs.getString("TABLE_NAME").equalsIgnoreCase(name)) {
                    return rs.getString("TABLE_NAME");
                }
            }
        }
        return null;
    }

    private DbProduct detectProduct() {
        try (Connection conn = dataSource.getConnection()) {
            String product = conn.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);
            if (product.contains("h2")) {
                return DbProduct.H2;
            }
            if (product.contains("postgresql")) {
                return DbProduct.POSTGRES;
            }
            if (product.contains("mysql")) {
                return DbProduct.MYSQL;
            }
            if (product.contains("mariadb")) {
                return DbProduct.MARIADB;
            }
        } catch (SQLException e) {
            log.warn("Could not determine database product; using generic schema strategy", e);
        }
        return DbProduct.UNKNOWN;
    }

    private void createMissingTables(DbProduct product) {
        jdbcTemplate.execute(createAuthUsersSql(product));
        String uuid = uuidType(product);
        for (String ddl : SUPPORTING_TABLE_DDL) {
            jdbcTemplate.execute(ddl.replace("%%UUID%%", uuid));
        }
    }

    private String createAuthUsersSql(DbProduct product) {
        StringBuilder sb = new StringBuilder("CREATE TABLE IF NOT EXISTS auth_users (");
        List<String> parts = new ArrayList<>();
        for (ExpectedColumn col : AUTH_USER_COLUMNS) {
            parts.add(col.name() + " " + columnType(col, product) + (col.nullable() ? "" : " NOT NULL"));
        }
        parts.add("CONSTRAINT uk_auth_user_username UNIQUE (username)");
        parts.add("CONSTRAINT uk_auth_user_email UNIQUE (email)");
        parts.add("CONSTRAINT uk_auth_user_phone UNIQUE (phone)");
        sb.append(String.join(", ", parts));
        sb.append(")");
        return sb.toString();
    }

    private void addMissingColumns(Set<String> columns, DbProduct product) {
        for (String colName : columns) {
            ExpectedColumn col = AUTH_USER_COLUMNS.stream()
                    .filter(c -> c.name().equals(colName))
                    .findFirst()
                    .orElse(null);
            if (col == null) {
                continue;
            }
            String sql = "ALTER TABLE auth_users ADD COLUMN " + colName + " "
                    + columnType(col, product) + (col.nullable() ? "" : " NOT NULL");
            try {
                jdbcTemplate.execute(sql);
            } catch (Exception e) {
                log.warn("Could not add column {} : {}", colName, e.getMessage());
            }
        }
    }

    private String columnType(ExpectedColumn col, DbProduct product) {
        return switch (col.javaType()) {
            case UUID -> uuidType(product);
            case VARCHAR -> "VARCHAR(" + col.length() + ")";
            case BOOLEAN -> "BOOLEAN";
            case INTEGER -> "INTEGER";
            case TIMESTAMP -> "TIMESTAMP";
        };
    }

    private String uuidType(DbProduct product) {
        return switch (product) {
            case H2, POSTGRES -> "UUID";
            default -> "VARCHAR(36)";
        };
    }

    private boolean isTypeCompatible(ExpectedColumn expected, String actual) {
        return switch (expected.javaType()) {
            case UUID -> actual.contains("UUID") || actual.contains("CHAR") || actual.contains("BINARY");
            case VARCHAR -> actual.startsWith("VARCHAR") || actual.startsWith("CHAR") || actual.startsWith("TEXT");
            case BOOLEAN -> actual.startsWith("BOOLEAN") || actual.startsWith("BIT");
            case INTEGER -> actual.startsWith("INTEGER") || actual.startsWith("INT") || actual.startsWith("SMALLINT");
            case TIMESTAMP -> actual.startsWith("TIMESTAMP") || actual.startsWith("DATETIME") || actual.startsWith("DATE");
        };
    }

    private String javaTypeToSql(ExpectedColumn col) {
        return columnType(col, uuidTypeSafeFallback());
    }

    private DbProduct uuidTypeSafeFallback() {
        return DbProduct.H2;
    }

    private record ExpectedColumn(String name, JavaType javaType, Integer length,
                                  boolean nullable, UserField field) {
    }

    private enum JavaType {
        UUID, VARCHAR, BOOLEAN, INTEGER, TIMESTAMP
    }
}
