package be.ugent.ledc.core.config.parameter;

public class Flag
{
    private final String name;
    private final String shortName;
    private final String description;

    public Flag(String name, String shortName, String description)
    {
        this.name = name;
        this.shortName = shortName;
        this.description = description;
    }

    public String getName()
    {
        return name;
    }

    public String getShortName()
    {
        return shortName;
    }

    public String getDescription()
    {
        return description;
    }
}
