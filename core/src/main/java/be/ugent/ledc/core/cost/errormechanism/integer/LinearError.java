package be.ugent.ledc.core.cost.errormechanism.integer;

import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.RepairRuntimeException;
import be.ugent.ledc.core.cost.errormechanism.ErrorMechanism;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An error mechanism where the error can be modelled as a linear transformation of the type
 * y = a + (b/c)*x where y is the repair and x is the original value.
 * @author abronsel
 */
public class LinearError implements ErrorMechanism<Integer>
{
    private final int a;
    private final int b;
    private final int c;

    public LinearError(int a, int b, int c)
    {
        this.a = a;
        this.b = b;
        this.c = c;
        if(c == 0)
            throw new RepairRuntimeException("A linear error cannot have c=0. Division by zero.");
    }
    
    public LinearError(int a, int b)
    {
        this(a,b,1);
    }
    
    
    @Override
    public boolean explains(Integer originalValue, Integer repairedValue, DataObject originalObject)
    {
        return originalValue != null &&
               repairedValue != null &&
               a + (originalValue*b)/c == repairedValue;
    }

    @Override
    public List<Integer> explanations(Integer originalValue, DataObject originalObject)
    {
        return originalValue == null
            ? new ArrayList<>()
            : Stream.of(a + (originalValue*b)/c).collect(Collectors.toList());
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + this.a;
        hash = 59 * hash + this.b;
        hash = 59 * hash + this.c;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final LinearError other = (LinearError) obj;
        if (this.a != other.a) {
            return false;
        }
        if (this.b != other.b) {
            return false;
        }
        return this.c == other.c;
    }
    
    
    
}
