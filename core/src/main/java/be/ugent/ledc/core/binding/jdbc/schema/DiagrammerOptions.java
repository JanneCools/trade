package be.ugent.ledc.core.binding.jdbc.schema;

import be.ugent.ledc.core.binding.jdbc.schema.generator.RegexNameFilter;
import be.ugent.ledc.core.binding.jdbc.schema.generator.TableSchemaFilter;

public class DiagrammerOptions
{
    private final boolean showCheckConstraints;
    
    private final boolean showRowCount;
    
    private final String regexFilter;
    
    private final boolean invertFilterSelection;
    
    private final boolean noViews;

    public DiagrammerOptions(boolean showCheckConstraints, boolean showRowCount, String regexFilter, boolean invertFilterSelection)
    {
        this(showCheckConstraints, showRowCount, regexFilter, invertFilterSelection, false);
    }
    
    public DiagrammerOptions(boolean showCheckConstraints, boolean showRowCount, String regexFilter, boolean invertFilterSelection, boolean noViews)
    {
        this.showCheckConstraints = showCheckConstraints;
        this.showRowCount = showRowCount;
        this.regexFilter = regexFilter;
        this.invertFilterSelection = invertFilterSelection;
        this.noViews = noViews;
    }
    
    public DiagrammerOptions(boolean showCheckConstraints, boolean showRowCount, String regexFilter)
    {
        this(showCheckConstraints, showRowCount, regexFilter, false, false);
    }

    public boolean isShowCheckConstraints()
    {
        return showCheckConstraints;
    }

    public boolean isShowRowCount()
    {
        return showRowCount;
    }

    public boolean isNoViews()
    {
        return noViews;
    }

    public TableSchemaFilter createFilter()
    {
        return new RegexNameFilter(regexFilter, invertFilterSelection);
    }
}
