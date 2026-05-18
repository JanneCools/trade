package be.ugent.ledc.core.binding.jdbc.agents;

import be.ugent.ledc.core.binding.jdbc.RelationalDB;

public interface SchemaAgent
{
    public static final String TABLE_NAME                   = "TABLE_NAME";
    public static final String TABLE_TYPE                   = "TABLE_TYPE";
    public static final String COLUMN_NAME                  = "COLUMN_NAME";
    public static final String IS_AUTO_INCREMENT            = "IS_AUTO_INCREMENT";
    public static final String DATA_TYPE                    = "DATA_TYPE";
    public static final String DATA_TYPE_LENGTH             = "DATA_TYPE_LENGTH";
    //this was added to accomodate MySQL's tinyint(1) -> a tinyint with display length 1. automatically mapped to a Boolean by the JDBC driver.
    public static final String DATA_TYPE_DISPLAY_LENGTH     = "DATA_TYPE_DISPLAY_LENGTH";
    public static final String DATA_TYPE_SCALE              = "DATA_TYPE_SCALE";
    public static final String DATA_TYPE_PRECISION          = "DATA_TYPE_PRECISION";
    public static final String DATA_TYPE_PRECISION_RADIX    = "DATA_TYPE_PRECISION_RADIX";
    public static final String IS_NULLABLE                  = "IS_NULLABLE";
    public static final String CONSTRAINT_NAME              = "CONSTRAINT_NAME";
    public static final String LEFT_TABLE_NAME              = "LEFT_TABLE_NAME";
    public static final String RIGHT_TABLE_NAME             = "RIGHT_TABLE_NAME";
    public static final String LEFT_COLUMN_NAME             = "LEFT_COLUMN_NAME";
    public static final String RIGHT_COLUMN_NAME            = "RIGHT_COLUMN_NAME";
    public static final String INDEX_NAME                   = "INDEX_NAME";
    public static final String DELETE_RULE                  = "DELETE_RULE";
    public static final String UPDATE_RULE                  = "UPDATE_RULE";
    public static final String SEQUENCE_NAME                = "SEQUENCE_NAME";
    public static final String DEFAULT_VALUE                = "DEFAULT_VALUE";
    public static final String CHECK_CONSTRAINT             = "CHECK_CONSTRAINT";
    
    public String getColumnQuery(RelationalDB dbConnectionInfo);
    public String getPrimaryKeyQuery(RelationalDB dbConnectionInfo);
    public String getForeignKeyQuery(RelationalDB dbConnectionInfo);
    public String getSequenceQuery(RelationalDB dbConnectionInfo);
    public String getIndexQuery(RelationalDB dbConnectionInfo);
    public String getCheckConstraintQuery(RelationalDB dbConnectionInfo);
    public String getUniqueConstraintQuery(RelationalDB dbConnectionInfo);
}
