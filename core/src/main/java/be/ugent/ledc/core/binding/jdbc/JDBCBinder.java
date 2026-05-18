package be.ugent.ledc.core.binding.jdbc;

import be.ugent.ledc.core.binding.BindingException;
import be.ugent.ledc.core.binding.jdbc.schema.DatabaseSchema;
import be.ugent.ledc.core.binding.jdbc.schema.generator.Schematizer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Implements a general binding with a database.
 */
public class JDBCBinder
{
    private final RelationalDB rdb;
    
    private final Charset encoding;

    public JDBCBinder(RelationalDB rdb, Charset encoding)
    {
        this.rdb = rdb;
        this.encoding = encoding;
    }
    
    public JDBCBinder(RelationalDB rdb)
    {
        this(rdb, StandardCharsets.UTF_8);
    }

    public Connection connect() throws BindingException, SQLException
    {
        //Set properties
        Properties props = new Properties();
        props.setProperty("user", rdb.getUsername());
        props.setProperty("password", rdb.getPassword());
        props.setProperty("charSet", encoding.name());

        //Make a connection
        Connection conn = DriverManager.getConnection(rdb.getConnectionString(), props);

        //Set the schema, if applicable
        rdb.getVendor().getAgent().specifySchema(conn, rdb);

        return conn;

    }

    public RelationalDB getRdb()
    {
        return rdb;
    }

    public Charset getEncoding()
    {
        return encoding;
    }
    
    public DatabaseSchema getSchema() throws BindingException
    {
        return new Schematizer().schematize(rdb);
    }
}
