package be.ugent.ledc.core.datastructures.relation;

import be.ugent.ledc.core.datastructures.Couple;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * A generic interface to model binary relations over the domain of a datatype T
 * @author abronsel
 * @param <T> 
 */
public interface IBinaryRelation<T>
{
    /**
     * Returns true if this BinaryRelation contains the couple (t1,t2). 
     * @param t1
     * @param t2
     * @return 
     */
    public default boolean contains(T t1, T t2)
    {
        return contains(new Couple<>(t1,t2));
    }
    
    /**
     * Returns true if this BinaryRelation contains the couple c. 
     * @param couple
     * @return 
     */
    public boolean contains(Couple<T> couple);
    
    /**
     * Adds the couple (t1,t2) to this BinaryRelation.
     * @param t1
     * @param t2
     */
    public default void add(T t1, T t2)
    {
        add(new Couple<>(t1,t2));
    }
    
    /**
     * Adds the couple to this relation.
     * @param couple
     */
    public void add(Couple<T> couple);
    
    /**
     * Deletes the couple (t1,t2) from this BinaryRelation. Important: elements
     * are not deleted from the domain.
     * @param t1
     * @param t2
     */
    public default void delete(T t1, T t2)
    {
        delete(new Couple<>(t1,t2));
    }
    
    /**
     * Deletes the couple from this relation. Important: elements
     * are not deleted from the domain.
     * @param couple
     */
    public void delete(Couple<T> couple);
    
    /**
     * Returns a stream over all domain element involved in this BinaryRelation.
     * @return 
     */
    public Stream<T> elementStream();
    
    /**
     * Returns a stream over all couples contained in BinaryRelation.
     * @return 
     */
    public Stream<Couple<T>> stream();
    
    /**
     * Returns true if this BinaryRelation is reflexive. This means that for any
     * element t involved in the domain, the BinaryRelation must contain (t,t).
     * False otherwise.
     * @return 
     */
    public default boolean isReflexive()
    {
        return elementStream().allMatch(t -> contains(t,t));
    }
    
    /**
     * Returns true if this BinaryRelation is symmetric. This means that for any
     * couple (t1, t2), the BinaryRelation must contain (t2,t1). False otherwise.
     * @return 
     */
    public default boolean isSymmetric()
    {
        return stream().allMatch(c -> contains(c.getSecond(), c.getFirst()));
    }
    
    /**
     * Returns true if this BinaryRelation is anti-symmetric. This means that for any
     * couple (t1, t2) for which t1 and t2 are different, the BinaryRelation
     * does not contain (t2,t1). False otherwise.
     * @return 
     */
    public default boolean isAntisymmetric()
    {
        return stream()
            .filter(c -> !Objects.equals(c.getFirst(), c.getSecond()))
            .noneMatch(c -> contains(c.getSecond(), c.getFirst()));
    }
    
    /**
     * Returns true if this BinaryRelation is transitive. This means that for any two
     * couples (t1, t2) and (t2, t3), the BinaryRelation must contain (t1,t3).
     * False otherwise.
     * @return 
     */
    public default boolean isTransitive()
    {
        return stream()
            .allMatch(c1 -> stream()
                .filter(c2 -> Objects.equals(c1.getSecond(), c2.getFirst()))
                .allMatch(c2 -> contains(c1.getFirst(), c2.getSecond()))
            );
    }
    
    /**
     * Returns the transitive closure of this BinaryRelation.
     * @return 
     */
    public BinaryRelation<T> transitiveClosure();
    
    /**
     * Returns the reflexive closure of this BinaryRelation.
     * @return 
     */
    public BinaryRelation<T> reflexiveClosure();
}
