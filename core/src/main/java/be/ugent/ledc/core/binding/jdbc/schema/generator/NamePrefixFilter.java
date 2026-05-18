package be.ugent.ledc.core.binding.jdbc.schema.generator;

import be.ugent.ledc.core.binding.jdbc.schema.TableSchema;

public class NamePrefixFilter implements TableSchemaFilter
{
    private String prefix;
    
    private String mode;
    
    public static final String REJECT = "REJECT";
    public static final String ACCEPT = "ACCEPT";

    public NamePrefixFilter()
    {
        this.prefix = "";
        this.mode = ACCEPT;
    }

    public NamePrefixFilter(String prefix, String mode)
    {
        this.prefix = prefix;
        this.mode = mode;
    }

    /**
     * Get the value of prefix
     *
     * @return the value of prefix
     */
    public String getPrefix()
    {
        return prefix;
    }

    /**
     * Set the value of prefix
     *
     * @param prefix new value of prefix
     */
    public void setPrefix(String prefix)
    {
        this.prefix = prefix;
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
            if(signature.getName().toLowerCase().startsWith(prefix.toLowerCase()))
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
            if(!signature.getName().toLowerCase().startsWith(prefix.toLowerCase()))
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
