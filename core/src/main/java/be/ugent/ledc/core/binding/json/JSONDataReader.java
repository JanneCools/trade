package be.ugent.ledc.core.binding.json;

import be.ugent.ledc.core.binding.DataReadException;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.dataset.SimpleDataset;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

public class JSONDataReader
{
    private final JSONBinder binder;

    public JSONDataReader(JSONBinder binder)
    {
        this.binder = binder;
    }

    /**
     * Reads a JSON file into a SimpleDataset. This dataset contains one object
     * which has one attribute of which the name is determined by the constant JSONBinder.ROOT_NAME.
     * 
     * The value of this root attribute is:
     * 
     * 1. A DataObject if the top level value of the document is a JSONObject,
     * 2. A List if the top level value of the document is a JSONArray,
     * 3. A constant value or null in any other case.
     * 
     * @return
     * @throws DataReadException 
     */
    public SimpleDataset readData() throws DataReadException
    {
        try
        {
            JSONTokener tokener = new JSONTokener(
                    new BufferedReader(
                        new InputStreamReader(
                            new FileInputStream(binder.getJsonFile()),
                            StandardCharsets.UTF_8)));
            
            Object o = new JSONObject(tokener);
            
            SimpleDataset dataset = new SimpleDataset();
            dataset.addDataObject(new DataObject().set(JSONBinder.ROOT_NAME, o));
            return dataset;
        }
        catch(FileNotFoundException ex)
        {
            throw new DataReadException(ex);
        }
        
        
    }
    
    private Object parse(Object o) throws DataReadException
    {
        //JSON null value
        if(o.equals(JSONObject.NULL))
        {
            return null;
        }
        //JSON constant values: string, number and boolean
        else if(o instanceof String
            ||  o instanceof Number
            ||  o instanceof Boolean)
        {
            return o;
        }
        //JSON object is mapped to DataObject with recursive membership parsing
        else if(o instanceof JSONObject)
        {
            return parseJSONObject((JSONObject)o);
        }
        else if(o instanceof JSONArray)
        {
            return parseJSONArray((JSONArray)o);
        }
        
        throw new DataReadException("Unknown object type in JSON tree: " + o);
        
    }

    /**
     * Mapping of a JSONObject to a DataObject with recursive parsing
     * @param jsonObject
     * @return 
     */
    private DataObject parseJSONObject(JSONObject jsonObject) throws DataReadException
    {
        DataObject o = new DataObject();
        
        for(String key: jsonObject.keySet())
        {
            o.set(key, parse(jsonObject.get(key)));
        }
        
        return o;
    }
    
    /**
     * Mapping of a JSONObject to a DataObject with recursive parsing
     * @param jsonObject
     * @return 
     */
    private List<Object> parseJSONArray(JSONArray jsonArray) throws DataReadException
    {
        List<Object> list = new ArrayList();
       
        for(int i=0; i<jsonArray.length(); i++)
        {
            list.add(parse(jsonArray.get(i)));
        }
        
        return list;
    }
}
