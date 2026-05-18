package be.ugent.ledc.core.binding.jdbc;

import java.util.Objects;

/**
 * A class that holds information for connecting to a relational database.
 * @author antoon
 */
public class RelationalDB 
{
    /**
     * The vendor (i.e. database system) of the database.
     */
    private final DBMS vendor;

    /**
     * The host to which we connect
     */
    private final String host;

    /**
     * The port
     */
    private final String port;

    /**
     * A username to log in.
     */
    private final String username;

    /**
     * A password to log in;
     */
    private String password;

    /**
     * The name of the specific database to which the connection is made.
     */
    private final String databaseName;
    
    /**
     * Schema name.
     */
    private final String schemaName;
    
    public RelationalDB(DBMS vendor, String host, String port, String username, String password, String databaseName, String schemaName)
    {
        this.vendor = vendor;
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.databaseName = databaseName;
	this.schemaName = schemaName;
    }

    public String getSchemaName() {
	return this.schemaName;
    }

    public String getDatabaseName()
    {
        return this.databaseName;
    }

    public DBMS getVendor()
    {
        return this.vendor;
    }

    public String getUsername()
    {
        return this.username;
    }

    public String getPassword()
    {
        return this.password;
    }

    public String getHost()
    {
        return host;
    }

    public String getPort()
    {
        return port;
    }

    public String getConnectionString()
    {
        return this
            .vendor
            .getAgent()
            .getConnectionString(this);
    }

    public void setPassword(String password)
    {
        this.password = password;
    }
    
    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 67 * hash + Objects.hashCode(this.vendor);
        hash = 67 * hash + Objects.hashCode(this.host);
        hash = 67 * hash + Objects.hashCode(this.port);
        hash = 67 * hash + Objects.hashCode(this.username);
        hash = 67 * hash + Objects.hashCode(this.password);
        hash = 67 * hash + Objects.hashCode(this.databaseName);
        hash = 67 * hash + Objects.hashCode(this.schemaName);
        return hash;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final RelationalDB other = (RelationalDB) obj;
        if (!Objects.equals(this.vendor, other.vendor))
        {
            return false;
        }
        if (!Objects.equals(this.host, other.host))
        {
            return false;
        }
        if (!Objects.equals(this.port, other.port))
        {
            return false;
        }
        if (!Objects.equals(this.username, other.username))
        {
            return false;
        }
        if (!Objects.equals(this.password, other.password))
        {
            return false;
        }
        if (!Objects.equals(this.databaseName, other.databaseName))
        {
            return false;
        }
        if (!Objects.equals(this.schemaName, other.schemaName))
        {
            return false;
        }
        return true;
    }
}
