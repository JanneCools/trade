package be.ugent.ledc.core.binding.json;

import be.ugent.ledc.core.binding.DataWriteException;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.dataset.Dataset;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.json.JSONObject;

public class JSONDataWriter
{
    private final JSONBinder binder;

    public JSONDataWriter(JSONBinder binder)
    {
        this.binder = binder;
    }

    public void writeData(Dataset data) throws DataWriteException
    {
        if(data.getSize() == 0)
            return;
        
        if(data.getSize() > 1)
            throw new DataWriteException("Cannot write dataset to JSON file."
                + " Cause: Dataset must contain a single object.");
        
        DataObject o = data.getDataObjects().get(0);
        
        if(o.getAttributes().isEmpty())
            return;
        
        if(o.getAttributes().size() > 1
        || o.getAttributes().contains(JSONBinder.ROOT_NAME))
            throw new DataWriteException("Cannot write dataset to JSON file."
                + " Cause: top-level object must contain a single attribute '"
                + JSONBinder.ROOT_NAME + "'.");
        
        try
        {
            //Dump the object to the given writer
            try ( //Open a writer
                    PrintWriter writer = new PrintWriter(
                            new BufferedWriter(
                                    new OutputStreamWriter(
                                            new FileOutputStream(
                                                    binder.getJsonFile()),
                                            StandardCharsets.UTF_8
                                    )
                            ),
                            true))
            {
                //Dump the object to the given writer
                dump(o, writer, 0);
                //Close the connection to the writer
            }
        }
        catch(FileNotFoundException ex)
        {
            throw new DataWriteException(ex);
        }
    }
    
    private void dump(Object o, PrintWriter writer, int depth) throws DataWriteException
    {
        //JSON null value
        if(o.equals(JSONObject.NULL))
        {
            writer.print(indent(depth) + "null");
        }
        //JSON constant values with no quotes: number and boolean
        else if(o instanceof Number
            ||  o instanceof Boolean)
        {
            writer.print(indent(depth) + o.toString());
        }
        //JSON constant values with quotes: String
        else if(o instanceof String)
        {
            writer.print(indent(depth) + "\"" + o.toString() + "\"");
        }
        //JSON object is mapped to DataObject with recursive membership parsing
        else if(o instanceof DataObject)
        {
            dumpObject((DataObject)o, writer, depth);
        }
        else if(o instanceof List)
        {
            dumpArray((List<Object>)o, writer, depth);
        }
        
        throw new DataWriteException("Unpermitted object type in JSON tree: " + o);
        
    }

    /**
     * Mapping of a JSONObject to a DataObject with recursive parsing
     * @param jsonObject
     * @return 
     */
    private void dumpObject(DataObject o, PrintWriter writer, int depth) throws DataWriteException
    {
        writer.println(indent(depth) + "{");
        
        List<String> keyList = new ArrayList<>(o.getAttributes());
        
        for(int i=0;i<keyList.size(); i++)
        {
            writer.print(indent(depth+1) + "\"" + keyList.get(i) + "\":");
            dump(o.get(keyList.get(i)), writer, depth+1);
            
            if(i == keyList.size() - 1)
                writer.println("");
            else
                writer.println(",");
            
        }
        
        writer.println(indent(depth) + "}");
    }
    
    /**
     * Mapping of a JSONObject to a DataObject with recursive parsing
     * @param jsonObject
     * @return 
     */
    private void dumpArray(List<Object> list, PrintWriter writer, int depth) throws DataWriteException
    {
        writer.println(indent(depth) + "[");
        
        for(int i=0;i<list.size(); i++)
        {
            writer.print(indent(depth+1) + list.get(i));
            dump(list.get(i), writer, depth+1);
        }
        
        writer.println(indent(depth) + "]");
    }
    
    private String indent(int depth)
    {
        return IntStream
            .range(0, depth)
            .mapToObj(i -> "  ")
            .collect(Collectors.joining("")
            );
    }
}
