package ws.palladian.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * <p>
 * Test for the {@link DatabaseManager} using H2 in-memory database.
 * </p>
 * 
 * @author Philipp Katz
 */
public class DatabaseManagerTest {

    // test prepared statements
    private static final String CREATE_TABLE = "CREATE TABLE test (id INTEGER UNSIGNED NOT NULL AUTO_INCREMENT, name VARCHAR(255), PRIMARY KEY (id));";
    private static final String DROP_TABLE = "DROP TABLE test";
    private static final String INSERT_TEST = "INSERT INTO test (name) VALUES (?)";
    private static final String GET_TEST = "SELECT * FROM test";
    private static final String GET_TEST_BY_NAME = "SELECT * FROM test WHERE name = ?";
    private static final String COUNT_TEST = "SELECT COUNT(*) FROM test";
    private static final String MAX_TEST = "SELECT MAX(id) FROM test";

    // configuration for in-memory database
    private static final String JDBC_DRIVER = "org.h2.Driver";
    private static final String JDBC_URL = "jdbc:h2:mem:test";
    private static final String JDBC_USERNAME = "sa";
    private static final String JDBC_PASSWORD = "";

    /** The class under test. */
    private DatabaseManager databaseManager;

    private static final RowConverter<String> CONVERTER = new RowConverter<String>() {
        @Override
        public String convert(ResultSet resultSet) throws SQLException {
            return resultSet.getString("name");
        }
    };

    @Before
    public void before() {
        databaseManager = DatabaseManagerFactory.create(DatabaseManager.class, JDBC_DRIVER, JDBC_URL, JDBC_USERNAME,
                JDBC_PASSWORD);
        databaseManager
                .runUpdate(CREATE_TABLE);
    }

    @After
    public void after() {
        databaseManager.runUpdate(DROP_TABLE);
    }

    @Test
    public void testRunInsert() {
        assertEquals(1, databaseManager.runInsertReturnId(INSERT_TEST, "bob"));
        assertEquals(2, databaseManager.runInsertReturnId(INSERT_TEST, "mary"));
    }

    @Test
    public void testRunQuery() {
        databaseManager.runInsertReturnId(INSERT_TEST, "bob");
        databaseManager.runInsertReturnId(INSERT_TEST, "mary");
        List<String> result = databaseManager.runQuery(CONVERTER, GET_TEST);
        assertEquals(2, result.size());
        assertEquals("bob", result.get(0));
        assertEquals("mary", result.get(1));
    }

    @Test
    public void testRunSingleQuery() {
        databaseManager.runInsertReturnId(INSERT_TEST, "bob");
        databaseManager.runInsertReturnId(INSERT_TEST, "mary");
        String result = databaseManager.runSingleQuery(CONVERTER, GET_TEST_BY_NAME, "bob");
        assertEquals("bob", result);
    }

    @Test
    public void testRunQueryWithIterator() {
        databaseManager.runInsertReturnId(INSERT_TEST, "bob");
        databaseManager.runInsertReturnId(INSERT_TEST, "mary");
        ResultIterator<String> iterator = databaseManager.runQueryWithIterator(CONVERTER, GET_TEST);
        assertTrue(iterator.hasNext());
        assertEquals("bob", iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals("mary", iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testRunBatchUpdate() {
        final List<String> names = Arrays.asList("bob", "mary", "john", "carol");
        final int[] expectedIds = new int[] { 1, 2, 3, 4 };
        boolean success = databaseManager.runBatchInsert(INSERT_TEST, new BatchDataProvider() {

            @Override
            public List<Object> getData(int number) {
                List<Object> data = new ArrayList<Object>();
                data.add(names.get(number));
                return data;
            }

            @Override
            public int getCount() {
                return names.size();
            }

            @Override
            public void insertedItem(int number, int generatedId) {
                assertEquals(expectedIds[number], generatedId);
            }
        });
        assertTrue(success);
    }

    @Test
    public void testRunBatchUpdateReturnIds() {
        List<List<Object>> params = new ArrayList<List<Object>>();
        params.add(Arrays.asList((Object) "bob"));
        params.add(Arrays.asList((Object) "mary"));
        params.add(Arrays.asList((Object) "john"));
        params.add(Arrays.asList((Object) "carol"));
        int[] generatedIds = databaseManager.runBatchInsertReturnIds(INSERT_TEST, params);
        assertEquals(4, generatedIds.length);
        assertEquals(1, generatedIds[0]);
        assertEquals(2, generatedIds[1]);
        assertEquals(3, generatedIds[2]);
        assertEquals(4, generatedIds[3]);
    }

    @Test
    public void runAggregateQuery() {
        databaseManager.runInsertReturnId(INSERT_TEST, "bob");
        databaseManager.runInsertReturnId(INSERT_TEST, "alice");
        databaseManager.runInsertReturnId(INSERT_TEST, "mary");
        int aggregateResult = databaseManager.runAggregateQuery(COUNT_TEST);
        assertEquals(3, aggregateResult);
        aggregateResult = databaseManager.runAggregateQuery(MAX_TEST);
        assertEquals(3, aggregateResult);
    }

}
