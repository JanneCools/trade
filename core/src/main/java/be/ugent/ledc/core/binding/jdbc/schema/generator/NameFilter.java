package be.ugent.ledc.core.binding.jdbc.schema.generator;

import be.ugent.ledc.core.binding.jdbc.schema.TableSchema;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class NameFilter implements TableSchemaFilter
{
    private Set<String> setOfNames;
    private String mode;
    
    public static final String REJECT = "REJECT";
    public static final String ACCEPT = "ACCEPT";

    public NameFilter()
    {
        this(new HashSet<>(), ACCEPT);
    }

    public NameFilter(Set<String> setOfNames, String mode)
    {
        this.setOfNames = setOfNames;
        this.mode = mode;
    }
    
    public NameFilter(Set<String> setOfNames)
    {
        this(setOfNames, ACCEPT);
    }
    
    public NameFilter(String... setOfNames)
    {
        this(Arrays.asList(setOfNames).stream().collect(Collectors.toSet()));
    }

    public Set<String> getSetOfNames()
    {
        return setOfNames;
    }

    public void setSetOfNames(Set<String> setOfNames)
    {
        this.setOfNames = setOfNames;
    }

    public String getMode()
    {
        return mode;
    }

    public void setMode(String mode)
    {
        this.mode = mode;
    }

    @Override
    public boolean accept(TableSchema signature)
    {
        if(ACCEPT.equals(mode))
        {
            if(setOfNames.contains(signature.getName()))
            {
                return true;
            }
            else
            {
                return false;
            }
        }
        if(REJECT.equals(mode))
        {
            if(!setOfNames.contains(signature.getName()))
            {
                return true;
            }
            else
            {
                return false;
            }
        }
        
        return true;
    }
}

