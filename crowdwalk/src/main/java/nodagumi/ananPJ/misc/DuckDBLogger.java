package nodagumi.ananPJ.misc;

import org.duckdb.DuckDBAppender;
import org.duckdb.DuckDBConnection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class DuckDBLogger {
    public enum Type { VARCHAR, INTEGER, DOUBLE, JSON };
    public record Column(String name, Type type) {};

    String filePath;
    DuckDBConnection connection;
    ScheduledExecutorService commitScheduler;
    Map<String, ConcurrentLinkedDeque<List<Object>>> insertionQueueMap = new HashMap<>();

    private DuckDBLogger(String filePath) {
        this.filePath = filePath;
    }

    public static DuckDBLogger create(String filePath) throws IOException, SQLException {
        DuckDBLogger duckDBLogger = new DuckDBLogger(filePath);
        duckDBLogger.init();
        return duckDBLogger;
    }

    private void init() throws IOException, SQLException {
        Files.deleteIfExists(Paths.get(filePath));
        Files.deleteIfExists(Paths.get(filePath + ".wal"));
        connection = (DuckDBConnection) DriverManager.getConnection("jdbc:duckdb:" + filePath);
        connection.setAutoCommit(false);
        commitScheduler = Executors.newSingleThreadScheduledExecutor();
        commitScheduler.scheduleWithFixedDelay(() -> processQueue(), 100, 100, TimeUnit.MILLISECONDS);
    }

    public void close() throws SQLException {
        commitScheduler.submit(() -> processQueue());
        commitScheduler.shutdown();
        try {
            commitScheduler.awaitTermination(1, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        connection.setAutoCommit(true);
        connection.prepareStatement("CHECKPOINT").execute();
        connection.close();

        // Compact the file
        try {
            Path target = Paths.get(filePath);
            Path source = target.getParent().resolve("source." + target.getFileName());
            Files.move(target, source);
            try (DuckDBConnection connection = (DuckDBConnection) DriverManager.getConnection("jdbc:duckdb:" + source)) {
                connection.prepareStatement("ATTACH '" + target.toString().replace("'", "''") + "' AS target;").execute();
                connection.prepareStatement("COPY FROM DATABASE source TO target;").execute();
                connection.prepareStatement("CHECKPOINT").execute();
            }
            Files.deleteIfExists(source);
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    private void processQueue() {
        insertionQueueMap.entrySet().forEach(entry -> {
            try {
                int counter = entry.getValue().size();
                List<Object> values;
                try (DuckDBAppender appender = connection.createAppender(DuckDBConnection.DEFAULT_SCHEMA, entry.getKey())) {
                    while (--counter >= 0 && (values = entry.getValue().poll()) != null) {
                        insert(appender, values);
                    }
                }
                connection.commit();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    void insert(DuckDBAppender appender, List<Object> values) throws SQLException {
        appender.beginRow();
        values.forEach(value -> {
            try {
                if (value instanceof Double) {
                    appender.append((double) value);
                } else if (value instanceof Integer) {
                    appender.append((int) value);
                } else {
                    appender.append(value.toString());
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
        appender.endRow();
    }

    public void createTable(String tableName, List<Column> columns) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("create table " + tableName
                + "(" + columns.stream()
                .map( v -> v.name + ' ' + v.type.name())
                .collect(Collectors.joining(","))+ ");")) {
            statement.execute();
        }
        connection.commit();
        insertionQueueMap.put(tableName, new ConcurrentLinkedDeque<>());
    }

    public void insert(String tableName, List<Object> values) {
        insertionQueueMap.get(tableName).add(values);
    }
}
