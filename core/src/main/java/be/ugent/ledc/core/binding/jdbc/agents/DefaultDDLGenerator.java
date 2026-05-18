package be.ugent.ledc.core.binding.jdbc.agents;

import be.ugent.ledc.core.binding.BindingException;
import be.ugent.ledc.core.binding.jdbc.RelationalDB;
import be.ugent.ledc.core.binding.jdbc.schema.CheckConstraint;
import be.ugent.ledc.core.binding.jdbc.schema.Reference;
import be.ugent.ledc.core.binding.jdbc.schema.DatabaseSchema;
import be.ugent.ledc.core.binding.jdbc.schema.TableSchema;
import be.ugent.ledc.core.binding.jdbc.schema.generator.SchematizeException;
import be.ugent.ledc.core.binding.jdbc.schema.generator.Schematizer;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DefaultDDLGenerator implements DDLGenerator
{
    private final Set<String> datatypesWithLength;
    private final Set<String> datatypesWithPrecision;
    
    /**
     * Connection info about where the DDL must be executed.
     */
    private final RelationalDB dbConnectionInfo;
    
    /**
     * Flag that indicates if tables may be overwritten.
     */
    private final boolean overwriteTables;
    
    /**
     * Indicators how tables may be modified.
     */
    private final TableModifiers tableModifiers;
    
    /**
     * Flag that indicates if tables may be deleted.
     */
    private final boolean deleteTables;
    
    /**
     * A Function that escapes names
     */
    private final Function<String,String> escaping;

    public DefaultDDLGenerator(Function<String,String> escaping, RelationalDB dbConnectionInfo, boolean overwriteTables, TableModifiers tableModifiers, boolean deleteTables) throws BindingException
    {
        this.dbConnectionInfo = dbConnectionInfo;
        this.overwriteTables = overwriteTables;
        this.tableModifiers = tableModifiers;
        this.deleteTables = deleteTables;
        this.escaping = escaping;
        
        datatypesWithLength = new HashSet<>();
        datatypesWithPrecision = new HashSet<>();
    }
    
    @Override
    public List<String> generateDDLScript(DatabaseSchema schema) throws BindingException
    {
        //Initialize list of statements
        LinkedList<String> statements = new LinkedList<>();
         
        //Fetch the current DatabaseSchema
        DatabaseSchema currentSchema = null;
        
        try
        {
            currentSchema = SchemaSpecifier.specify(getSchematizer().schematize(dbConnectionInfo), dbConnectionInfo.getVendor());
        }
        catch (SchematizeException ex)
        {
            Logger.getLogger(DDLGenerator.class.getName()).log(Level.SEVERE, null, ex);
        }

        //Specify schema
        schema = SchemaSpecifier.specify(schema, dbConnectionInfo.getVendor());

        //If tables are allowed to be overwritten
        if(overwriteTables && currentSchema != null)
        {
            //Remove Foreign Key constraints on tables that will be overwritten
            for(TableSchema tableSchema: schema.getTableSchemas())
            {
                //Check if this tableSchema occurs in the current schema
                if(tableSchema != null && currentSchema.containsKey(tableSchema.getName()) && currentSchema.get(tableSchema.getName()).getType().equals("BASE TABLE"))
                {
                    //For each reference of this tableSchema in the current schema
                    for(Reference reference: currentSchema.get(tableSchema.getName()).getReferences())
                    {                        
                        //Add Drop constraint statement
                        statements.add(generateDropForeignKeyConstraint(dbConnectionInfo.getSchemaName(), tableSchema.getName(), reference));
                    }
                }
            }
            
            //Remove tables that will be overwritten
            for(TableSchema tableSchema: schema.getTableSchemas())
            {
                //Check if this tableSchema occurs in the current schema
                if(tableSchema != null && currentSchema.containsKey(tableSchema.getName()) && currentSchema.get(tableSchema.getName()).getType().equals("BASE TABLE"))
                {
                    //Drop this tableSchema
                    statements.add(generateDropTable(dbConnectionInfo.getSchemaName(), tableSchema.getName()));
                }
            }
        }
        
        //If tables that no longer exist are allowed to be deleted
        if(deleteTables && currentSchema != null)
        {
            //Remove Foreign Key constraints on tables that will be deleted
            for(TableSchema tableSchema: currentSchema.getTableSchemas())
            {
                //Check if this tableSchema occurs in the new schema
                if(tableSchema != null && tableSchema.getType().equals("BASE TABLE") && !schema.containsKey(tableSchema.getName()))
                {
                    //For each reference of this tableSchema in the current schema
                    for(Reference reference: currentSchema.get(tableSchema.getName()).getReferences())
                    {                        
                        //Add Drop constraint statement
                        statements.add(generateDropForeignKeyConstraint(dbConnectionInfo.getSchemaName(), tableSchema.getName(), reference));
                    }
                }
            }
            
            //Remove tables that are allowed to be deleted.
            for(TableSchema tableSchema: currentSchema.getTableSchemas())
            {
                //Check if this tableSchema occurs in the current schema
                if(tableSchema != null && tableSchema.getType().equals("BASE TABLE") && schema.containsKey(tableSchema.getName()))
                {
                    //Drop this tableSchema
                    statements.add(generateDropTable(dbConnectionInfo.getSchemaName(), tableSchema.getName()));
                }
            }
        }
        
        //Modify existing tables
        if(tableModifiers.isModifiable() && !overwriteTables && currentSchema != null)
        {
            //For each table in the schema
            for(TableSchema tableSchema: schema.getTableSchemas())
            {
                //Check if this tableSchema occurs in the current schema
                if(tableSchema != null && currentSchema.containsKey(tableSchema.getName()) && currentSchema.get(tableSchema.getName()).getType().equals("BASE TABLE"))
                {
                    //Generate statements for modification of table
                    modifyTable(tableSchema, currentSchema.get(tableSchema.getName()), dbConnectionInfo.getSchemaName(), statements);
                }
            }
        }
        
        //Create new tables and constraints
        createTablesAndConstraints(schema, currentSchema, dbConnectionInfo.getSchemaName(), statements);
        
        //Return
        return statements;
    }
    
    @Override
    public List<String> generateDDLScript(TableSchema tableSchema) throws BindingException
    {
        DatabaseSchema stubSchema = new DatabaseSchema();
        
        stubSchema.addTableSchema(tableSchema);
        
        return generateDDLScript(stubSchema);
    }
    
    public void createTablesAndConstraints(DatabaseSchema schema, DatabaseSchema currentSchema, String schemaName, LinkedList<String> statements)
    {
        for(TableSchema tableSchema: schema.getTableSchemas())
        {
            if(tableSchema != null && (overwriteTables || currentSchema == null || !currentSchema.containsKey(tableSchema.getName())))
            {
                if(tableSchema.getType().equals("BASE TABLE"))
                {
                    statements.add(generateCreateTable(schemaName, tableSchema));
                }
            }
        }
        
        for(TableSchema tableSchema: schema.getTableSchemas())
        {
            if(tableSchema != null && (overwriteTables || currentSchema == null || !currentSchema.containsKey(tableSchema.getName())))
            {
                for(String constraintName: tableSchema.getUniqueConstraints().keySet())
                {
                    statements.add(generateAddUniqueConstraint(schemaName, tableSchema.getName(), constraintName, tableSchema.getUniqueConstraints().get(constraintName)));
                }

                for(Reference reference: tableSchema.getReferences())
                {
                    statements.add(generateAddForeignKey(schemaName, tableSchema.getName(), reference));
                }
                
                for(CheckConstraint check: tableSchema.getCheckConstraints())
                {
                    statements.add(generateAddCheckConstraint(schemaName, tableSchema.getName(), check.getCheckClause(), tableSchema.getCheckConstraints().indexOf(check)));
                }
            }
        }
    }

    public void modifyTable(TableSchema tableSchema, TableSchema currentSignature, String schemaName, LinkedList<String> statements)
    {
        for(String attributeName: tableSchema.getAttributeNames())
        {
            if(!currentSignature.containsKey(attributeName) && tableModifiers.isAddColumns())
            {
                statements.add(generateAddColumn(schemaName, attributeName, tableSchema));
            }
            else
            {
                //Fetch datatypes
                String datatype         = (String)tableSchema.getAttributeProperty(attributeName, TableSchema.PROP_DATATYPE);
                String currentDatatype  = (String)currentSignature.getAttributeProperty(attributeName, TableSchema.PROP_DATATYPE);
                
                //If datatype has changed
                if(!datatype.equals(currentDatatype) && tableModifiers.isChangeDatatypes())
                {
                    //Replace attribute
                    statements.add(generateDropColumn(schemaName, tableSchema.getName(), attributeName));
                    statements.add(generateAddColumn(schemaName, attributeName, tableSchema));
                }
                else
                {
                    //Fetch datatype properties
                    Number datatypeLength           = (Number)tableSchema.getAttributeProperty(attributeName, TableSchema.PROP_DATATYPE_LENGTH);
                    Number currentDatatypeLength    = (Number)currentSignature.getAttributeProperty(attributeName, TableSchema.PROP_DATATYPE_LENGTH);
                    
                    if(tableModifiers.isChangeDatatypes() && datatypeLength != null && currentDatatypeLength != null && datatypeLength.intValue() > currentDatatypeLength.intValue())
                    {
                        statements.add(generateModifyColumn(schemaName, attributeName, tableSchema));
                    }
                }
            }
        }

        if(tableModifiers.isChangeForeignKeys())
        {
            for(Reference reference: currentSignature.getReferences())
            {
                statements.add(generateDropForeignKeyConstraint(schemaName, tableSchema.getName(), reference));
            }

            for(Reference reference: tableSchema.getReferences())
            {
                statements.add(generateAddForeignKey(schemaName, tableSchema.getName(), reference));
            }
        }
        
        if(tableModifiers.isChangeUniqueConstraints())
        {
            for(String uniqueConstraintName: currentSignature.getUniqueConstraints().keySet())
            {
                statements.add(generateDropUniqueConstraint(schemaName, tableSchema.getName(), uniqueConstraintName));
            }

            for(String uniqueConstraintName: tableSchema.getUniqueConstraints().keySet())
            {
                statements.add(generateAddUniqueConstraint(schemaName, tableSchema.getName(), uniqueConstraintName, tableSchema.getUniqueConstraints().get(uniqueConstraintName)));
            }
        }
        
        if(tableModifiers.isDropColumns())
        {
            for(String currentAttributeName: currentSignature.getAttributeNames())
            {
                if(!tableSchema.containsKey(currentAttributeName))
                {
                    statements.add(generateDropColumn(schemaName, tableSchema.getName(), currentAttributeName));
                }
            }
        }
    }
    
    public Schematizer getSchematizer() throws BindingException
    {
        //Return new Schematizer
        return new Schematizer();
    }
    
    public String generateCreateTable(String schemaName, TableSchema tableSchema)
    {
        // Initialize
        String statement = "create table " + escaping.apply(schemaName) + "." + escaping.apply(tableSchema.getName()) + " (";

        // Counter for columns
        int columns = 0;

        // Add column specifications
        for (String columnName : tableSchema.keySet())
        {
            statement += makeColumnClause(columnName, tableSchema).trim();
            
            if ((++columns) != tableSchema.getAttributeNames().size())
            {
                statement += ", ";
            }
        }

        if (tableSchema.getPrimaryKey() != null && !tableSchema.getPrimaryKey().isEmpty())
        {
            // Add primary key
            statement += ", primary key (";

            // Counter for key columns
            int keyColumns = 0;

            for (String keyColumnName : tableSchema.getPrimaryKey())
            {
                statement += escaping.apply(keyColumnName);

                if ((++keyColumns) != tableSchema.getPrimaryKey().size())
                {
                    statement += ", ";
                }
                else
                {
                    statement += ") ";
                }
            }
        }

        // Statement closure
        statement = statement.concat(");");

        // Return
        return statement;
    }
    
    public String generateAddForeignKey(String schemaName, String tableSchemaName, Reference reference)
    {
        //Name check
        referenceCheck(reference);
        
        // Get name of referenced tableSchema
        String referenced = reference.getReferenced().getName();

        // Initialize
        String statement = "alter table " + escaping.apply(schemaName) + "." + escaping.apply(tableSchemaName) + " add constraint " + reference.getName() + " ";

        statement += "foreign key ";

        String baseColumns = "(";
        String refColumns = "(";

        // Column counter
        int columns = 0;

        for (String baseColumn : reference.keySet())
        {
            baseColumns += escaping.apply(baseColumn);
            refColumns += escaping.apply(reference.get(baseColumn));

            if ((++columns) != reference.size())
            {
                baseColumns += ", ";
                refColumns += ", ";
            }
            else
            {
                baseColumns += ") ";
                refColumns += ") ";
            }
        }

        //Reference part
        statement += baseColumns + "references " + escaping.apply(schemaName) + "." + escaping.apply(referenced) + " " + refColumns;
        
        //Delete and cascade strategy
        statement += "on delete " + reference.getOnDelete() + " on update " + reference.getOnUpdate() + ";";
        

        // Return
        return statement;
    }

    public String generateAddUniqueConstraint(String schemaName, String tableSchemaName, String constraintName, Set<String> uniqueAttributeNames)
    {
        String uClause = "";
        
        for(String attributeName: uniqueAttributeNames)
        {
            uClause += escaping.apply(attributeName) + ",";
        }
        
        uClause = uClause.substring(0, uClause.length()-1);
        
        String ddl =  "alter table " + escaping.apply(schemaName) + "." + escaping.apply(tableSchemaName) + " add constraint " + escaping.apply(constraintName) + " unique (" + uClause + ");";
        
        return ddl;
    }
    
    public String generateAddCheckConstraint(String schemaName, String tableSchemaName, String check, int index)
    {        
        return "alter table " + escaping.apply(schemaName) + "." + escaping.apply(tableSchemaName) + " add constraint " + escaping.apply(tableSchemaName.concat("_check_constraint").concat("" + index)) + " check (" + check + ");";
    }
    
    public String generateAddColumn(String schemaName, String columnName, TableSchema tableSchema)
    {
        String statement = "alter table " + escaping.apply(schemaName) + "." + escaping.apply(tableSchema.getName()) + " add column " + makeColumnClause(columnName, tableSchema).trim() + ";";
        return statement;
    }
    
    public String generateModifyColumn(String schemaName, String columnName, TableSchema tableSchema)
    {
        String statement = "alter table " + escaping.apply(schemaName) + "." + escaping.apply(tableSchema.getName()) + " modify column " + escaping.apply(columnName) + " " + getFullDatatype(columnName, tableSchema) + ";";
        return statement;
    }
    
    public String generateDropColumn(String schemaName, String tableSchemaName, String columnName)
    {
        String statement = "alter table " + escaping.apply(schemaName) + "." + escaping.apply(tableSchemaName) + " drop column " + escaping.apply(columnName) + ";";
        return statement;
    }
    
    public String generateDropForeignKeyConstraint(String schemaName, String tableSchemaName, Reference reference)
    {
        return "alter table " + escaping.apply(schemaName) + "." + escaping.apply(tableSchemaName) + " drop constraint " + escaping.apply(reference.getName()) + ";";
    }
   
    public String generateDropUniqueConstraint(String schemaName, String tableSchemaName, String constraintName)
    {      
        return "alter table " + escaping.apply(schemaName) + "." + escaping.apply(tableSchemaName) + " drop constraint " + escaping.apply(constraintName) + ";";
    }
    
    public String generateDropTable(String schemaName, String tableSchemaName)
    {
        return "drop table " + escaping.apply(schemaName) + "." + escaping.apply(tableSchemaName) + ";";
    }
    
    public void referenceCheck(Reference reference)
    {
        if (reference.getName() == null)
        {
            reference.setName("fk_" + System.nanoTime());
        }
    }
    
    public String getIdentitySuffix()
    {
        return "identity";
    }
    
    public String getFullDatatype(String attributeName, TableSchema tableSchema)
    {
        String datatype = (String)tableSchema.getAttributeProperty(attributeName, TableSchema.PROP_DATATYPE);
        
        Number length = (Number) tableSchema.getAttributeProperty(attributeName, TableSchema.PROP_DATATYPE_LENGTH);
        Number precision = (Number) tableSchema.getAttributeProperty(attributeName, TableSchema.PROP_DATATYPE_PRECISION);
        Number scale = (Number) tableSchema.getAttributeProperty(attributeName, TableSchema.PROP_DATATYPE_SCALE);

        if(datatype != null)
        {
            if(precision != null && this.datatypesWithPrecision.contains(datatype))
            {                
                datatype = datatype + " (" + precision.intValue() + "," + ((scale == null)?0:scale.intValue()) + ")";
            }
            else if(length != null && this.datatypesWithLength.contains(datatype))
            {
                datatype = datatype + " (" + length.intValue() + ")";
            }

            return datatype;
        }
        
        return "text";
    }

    public Set<String> getDatatypesWithLength()
    {
        return datatypesWithLength;
    }

    public Set<String> getDatatypesWithPrecision()
    {
        return datatypesWithPrecision;
    }
    
    private String makeColumnClause(String columnName, TableSchema tableSchema)
    {
        String columnClause = "";
        
        String identitySuffix = "";
            
        if (tableSchema.getAttributeProperty(columnName, TableSchema.PROP_IS_AUTOINCREMENT) != null && (Boolean)(tableSchema.getAttributeProperty(columnName, TableSchema.PROP_IS_AUTOINCREMENT)))
        {   
            identitySuffix = getIdentitySuffix();
        }

        String datatype = getFullDatatype(columnName, tableSchema);

        columnClause += escaping.apply(columnName) + " " + datatype + " ";

        //Not null constraint
        if (tableSchema.isKeyAttribute(columnName) || (tableSchema.get(columnName).containsKey(TableSchema.PROP_IS_NULLABLE) && !(Boolean)(tableSchema.getAttributeProperty(columnName, TableSchema.PROP_IS_NULLABLE))))
        {
            columnClause += "not null ";
        }
        
        //Default value constraint
//        if (tableSchema.get(columnName).containsKey(TableSchema.PROP_DEFAULT_VALUE) && tableSchema.getAttributeProperty(columnName, TableSchema.PROP_DEFAULT_VALUE) != null)
//        {
//            SQLGenerator generator = new SQLGenerator();
//            columnClause += "default " + generator.stringValue(tableSchema.getAttributeProperty(columnName, TableSchema.PROP_DEFAULT_VALUE), false) + " ";
//        }
        
        //Identity suffix
        if(identitySuffix.length() == 0)
        {
            columnClause += identitySuffix;
        }
        
        //Return
        return columnClause;
    }

    public Function<String, String> getEscaping()
    {
        return escaping;
    }
}
