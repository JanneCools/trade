package be.ugent.ledc.core.operators.metric;

/**
 * General interface for comparing two objects of type T and express the result as S
 * @author antoon
 * @param <T> The type of data for which objects will be mutually compared (i.e., matched)
 * @param <S> The domain in which results of a match operation are expressed.
 */
public interface Matcher<T, S extends Comparable>
{
    public S compare(T o1, T o2);
}
