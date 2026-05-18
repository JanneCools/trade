package be.ugent.ledc.core.binding.jdbc.agents;

import be.ugent.ledc.core.binding.BindingException;
import be.ugent.ledc.core.binding.jdbc.RelationalDB;
import java.sql.Connection;
import java.util.function.Function;

public interface JDBCAgent
{    
    public String getConnectionString(RelationalDB rdbInfo);
    
    public Function<String, String> escaping();
    
    public SchemaAgent getSchemaAgent() throws BindingException;
    
    public MigrationAgent getMigrationAgent() throws BindingException;
    
    public DDLGenerator getDDLGenerator(RelationalDB rdbInfo, boolean overwriteTables, TableModifiers tableModifiers, boolean deleteTables) throws BindingException;
    
    public String explain(String query);
    
    public void specifySchema(Connection conn, RelationalDB info) throws BindingException;
}
