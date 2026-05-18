package be.ugent.ledc.core.datastructures;

/**
 * A Pair for which both values need to be of the same type
 * @author abronsel
 * @param <T> 
 */
public class Couple<T> extends Pair<T, T>
{

    public Couple(T first, T second)
    {
        super(first, second);
    }
    
    /**
     * An order insensitive variant of equals, where it suffices that both objects
     * in the couple are equal, regardless of their position in the couple.
     * @param other
     * @return 
     */
    public boolean iEquals(Couple<T> other)
    {
        return super.equals(other)
            || super.equals(new Couple<>(other.getSecond(), other.getFirst()));
    }
    
}
