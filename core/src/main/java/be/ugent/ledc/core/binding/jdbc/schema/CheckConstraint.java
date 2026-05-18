package be.ugent.ledc.core.binding.jdbc.schema;

import be.ugent.ledc.core.binding.jdbc.DBMS;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class CheckConstraint
{
    private final String checkClause;
    
    private final Set<String> involvedAttributes;

    public CheckConstraint(String checkClause, Set<String> attributeNames, DBMS vendor)
    {
         this.checkClause = checkClause;
         this.involvedAttributes = attributeNames
            .stream()
            .filter(a -> checkClause
                .matches(".*(" + vendor.getAgent().escaping().apply(a) + "|" + a + ").*"))
            .collect(Collectors.toSet());
    }

    public String getCheckClause()
    {
        return checkClause;
    }

    public Set<String> getInvolvedAttributes()
    {
        return involvedAttributes;
    }
    
    public boolean involves(String attribute)
    {
        return this.involvedAttributes.contains(attribute);
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 17 * hash + Objects.hashCode(this.checkClause);
        return hash;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final CheckConstraint other = (CheckConstraint) obj;
        if (!Objects.equals(this.checkClause, other.checkClause))
        {
            return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        return checkClause;
    }
}
