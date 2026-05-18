package be.ugent.ledc.core.binding.jdbc.schema.generator;

import be.ugent.ledc.core.binding.jdbc.schema.TableSchema;

public class RegexNameFilter implements TableSchemaFilter
{
    private final String regex;
    
    /**
     * When set to true, the selection of tables is inverted.
     * That is: tables matching the regex will not be included in the diagram.
     */
    private final boolean invert;

    public RegexNameFilter(String regex, boolean invert)
    {
        this.regex = regex;
        this.invert = invert;
    }
    
    public RegexNameFilter(String regex)
    {
        this(regex, false);
    }
    
    @Override
    public boolean accept(TableSchema tableSchema)
    {
        boolean match = tableSchema.getName().matches(regex);
        
        return invert ? !match : match;
    }
}
