package be.ugent.ledc.core.binding.jdbc;

import be.ugent.ledc.core.binding.BindingException;
import java.sql.Connection;
import java.sql.SQLException;

public abstract class JDBCOperator
{
    private final JDBCBinder binder;

    private Connection connection;

    public JDBCOperator(JDBCBinder binder)
    {
        this.binder = binder;
    }

    public JDBCBinder getBinder()
    {
        return binder;
    }

    public Connection getConnection() throws BindingException, SQLException
    {
        //Check if connection is null or needs to reopen
        if(connection == null || connection.isClosed())
            connection = getBinder().connect();
        
        return connection;
    }
    
    public void closeConnection() throws SQLException
    {
        if(connection != null && !connection.isClosed())
            connection.close();
    }
}
