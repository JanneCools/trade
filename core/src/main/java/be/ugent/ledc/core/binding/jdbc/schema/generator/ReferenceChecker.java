package be.ugent.ledc.core.binding.jdbc.schema.generator;

import be.ugent.ledc.core.binding.BindingException;
import be.ugent.ledc.core.binding.jdbc.JDBCBinder;
import be.ugent.ledc.core.binding.jdbc.RelationalDB;
import be.ugent.ledc.core.binding.jdbc.schema.Reference;
import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;

public class ReferenceChecker
{
    /**
     * Connection information.
     */
    private RelationalDB connectInfo;

    public boolean isViolated(String leftSignatureName, Reference reference) throws MalformedURLException, ClassNotFoundException, InstantiationException, IllegalAccessException, SQLException, BindingException
    {
        //Referenced signature
        String rightSignatureName = reference.getReferenced().getName();
        
        //First part of query
        String query = "SELECT COUNT(*) violations FROM " + leftSignatureName + " WHERE NOT EXISTS (SELECT * FROM " + rightSignatureName + " WHERE ";
        
        //Reference iterator
        Iterator<String> referenceAttributeIterator = reference.keySet().iterator();
        
        //Iterate
        while(referenceAttributeIterator.hasNext())
        {
            //Fetch
            String leftAttribute = referenceAttributeIterator.next();
            
            //Map
            String rightAttribute = reference.get(leftAttribute);
            
            //Update query
            query += leftSignatureName + "."+ leftAttribute + " = " + rightSignatureName + "." + rightAttribute;

            if(referenceAttributeIterator.hasNext())
            {
                query += " AND ";
            }
            else
            {
                query += ")";
            }
        }

        //Create a JDBCBinder
        JDBCBinder binder = new JDBCBinder(connectInfo);

        //Request a connection
        Connection connection = binder.connect();

        //Fetch a statement
        Statement stmt = connection.createStatement();

        //Execute
        ResultSet resultSet = stmt.executeQuery(query);

        //Initialize
        int violations = 0;

        while(resultSet.next())
        {
            violations = resultSet.getInt("violations");
        }

        //Close resultSet
        resultSet.close();

        //Close statement
        stmt.close();

        //Close connection
        connection.close();

        //Return
        return (violations!=0);
    }

    public RelationalDB getConnectInfo()
    {
        return connectInfo;
    }

    public void setConnectInfo(RelationalDB connectInfo)
    {
        this.connectInfo = connectInfo;
    }
}
