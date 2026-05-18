package be.ugent.ledc.core.datastructures.rules;


import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Toon Boeckling
 * <p>
 * This interface represents a Rule, which is a Predicate evaluated on some datatype
 * @param <D> Type of data on which this Rule acts.
 */
public interface Rule<D> extends Predicate<D>
{
    /**
     * Get the set of attributes involved in the Rule object.
     *
     * @return set of attributes involved in the Rule object
     */
    Set<String> getInvolvedAttributes();

    /**
     * Returns true if given attribute is involved in the Rule object and false otherwise.
     * By default, an attribute is involved if it is not null and contained in the set of involved attributes.
     *
     * @param attribute attribute to check whether it is involved in the Rule object
     * @return true if given attribute is involved in the Rule object, false otherwise
     */
    default boolean involves(String attribute)
    {
        return attribute != null && getInvolvedAttributes().contains(attribute);
    }

    /**
     * Returns true if Rule is satisfied by a given DataObject object and false otherwise.
     *
     * @param data Data for which it has to be checked whether it satisfies the Rule
     * @return true if Rule is satisfied by a given DataObject object and false otherwise
     */
    default boolean isSatisfied(D data)
    {
        return test(data);
    }

    /**
     * Returns true if Rule is failed by a given DataObject object and false otherwise.
     *
     * @param data Data object for which it has to be checked whether it fails the Rule
     * @return true if Rule is failed by a given DataObject object and false otherwise
     */
    default boolean isFailed(D data)
    {
        return !isSatisfied(data);
    }

    /**
     * True if the involved attributes of this Rule are a superset of the given attributes
     * @param attributes
     * @return 
     */
    default public boolean involvesAll(Set<String> attributes)
    {
        return getInvolvedAttributes().containsAll(attributes);
    }
    
    /**
     * True if the involved attributes of this Rule are a superset of the given attributes
     * @param attributes
     * @return 
     */
    default public boolean involvesAll(String ... attributes)
    {
        return involvesAll(Stream.of(attributes).collect(Collectors.toSet()));
    }
    
    /**
     * True if the involved attributes of this Rule are a subset of the given attributes
     * @param attributes
     * @return 
     */
    default public boolean allInvolved(Set<String> attributes)
    {
        return attributes.containsAll(getInvolvedAttributes());
    }
    
    /**
     * True if the involved attributes of this Rule are a subset of the given attributes
     * @param attributes
     * @return 
     */
    default public boolean allInvolved(String ... attributes)
    {
        return allInvolved(Stream.of(attributes).collect(Collectors.toSet()));
    }
}
