package be.ugent.ledc.core.operators;

import be.ugent.ledc.core.DataException;

/**
 * A UnitScore models a real value in the unit interval, with certain arithmetic operations.
 * @author abronsel
 */
public class UnitScore implements Comparable<UnitScore>
{
    /**
     * A UnitScore that models the constant 0.
     */
    public static final UnitScore ZERO = new UnitScore(0.0);
    
    /**
     * A UnitScore that models the constant 1.
     */
    public static final UnitScore ONE  = new UnitScore(1.0);

    /**
     * The value modelled by this UnitScore.
     */
    private final double value;
    
    /**
     * The default constructor where the value is passed.
     * @param value 
     * 
     * @throws DataException if the value exceeds the bounds of the unit interval (i.e., 0 and 1)
     */
    public UnitScore(double value)
    {
        this(value, 0);
    }
    
    /**
     * A constructor where the value is checked to see if it does not exceed
     * the bounds 0 and 1 with a value larger than a given tolerance.It the
     * given value exceeds bounds with a value less than the tolerance, the bounding
     * value is assigned instead.
     * @param value 
     * @param tolerance Accepted tolerance of exceeding the bounds 0 and 1
     * 
     * @throws DataException if the value exceeds the bounds of the unit interval (i.e., 0 and 1)
     */
    public UnitScore(double value, double tolerance)
    {
        if(Double.compare(value, 0) < 0 && -value <= tolerance)
            this.value = 0;
        else if(Double.compare(value, 1) > 0 && value - 1 <= tolerance)
            this.value = 1;
        else if(Double.compare(value, 0) < 0 || Double.compare(value, 1) > 0)
            throw new DataException("UnitScore initialized with value " + value + " not between 0 and 1.");
        else
            this.value = value;
    }

    /**
     * A utility method to compute the complement of the current UnitScore.
     * @return A UnitScore that models the complement of the current UnitScore.
     * The value of the complement is 1 minus the value of the current UnitScore.
     */
    public UnitScore getComplement() {
        return new UnitScore(1.0 - getValue());
    }
    
    /**
     * A static creator method to create a UnitScore from a fraction with a given 
     * numerator and denominator.
     * @param numerator The numerator of the fraction we want to model.
     * @param denominator The denominator of the fraction we want to model.
     * @return A UnitScore that models a value in the unit interval equal to the fraction
     * numerator/denominator.
     */
    public static UnitScore fromFraction(int numerator, int denominator)
    {
        return new UnitScore(((double)numerator) / ((double)denominator));
    }

    /**
     * Gets the double value held by this UnitScore
     * @return 
     */
    public double getValue()
    {
        return value;
    }
    
    /**
     * Returns the maximum of the two given UnitScores a and b.
     * @param a The first UnitScore.
     * @param b The second UnitScore.
     * @return The UnitScore that holds the largest value among a and b.
     */
    public static UnitScore max(UnitScore a, UnitScore b)
    {
        return new UnitScore(Math.max(a.getValue(), b.getValue()));
    }
    
    /**
     * Returns the minimum of the two given UnitScores a and b.
     * @param a The first UnitScore.
     * @param b The second UnitScore.
     * @return The UnitScore that holds the smallest value among a and b.
     */
    public static UnitScore min(UnitScore a, UnitScore b)
    {
        return new UnitScore(Math.min(a.getValue(), b.getValue()));
    }
    
    /**
     * Returns the product of the two given UnitScores a and b.
     * @param a The first UnitScore.
     * @param b The second UnitScore.
     * @return The UnitScore that holds the product of the values of a and b.
     */
    public static UnitScore times(UnitScore a, UnitScore b)
    {
        return new UnitScore(a.getValue() * b.getValue());
    }
    
    /**
     * Returns the square root of the given UnitScore.
     * @param a The given UnitScore.
     * @return The UnitScore that holds the square root of the value of a.
     */
    public static UnitScore sqrt(UnitScore a)
    {
        return new UnitScore(Math.sqrt(a.getValue()));
    }
    
    /**
     * Returns the square of the given UnitScore.
     * @param a The given UnitScore.
     * @return The UnitScore that holds the value of a, squared.
     */
    public static UnitScore squared(UnitScore a)
    {
        return new UnitScore(Math.pow(a.getValue(), 2.0));
    }
    
    /**
     * Returns the cube of the given UnitScore.
     * @param a The given UnitScore.
     * @return The UnitScore that holds the value of a, cubed.
     */
    public static UnitScore cubed(UnitScore a)
    {
        return new UnitScore(Math.pow(a.getValue(), 3.0));
    }
    
    /**
     * Verifies if the given UnitScore is equal to ZERO
     * @param a The given UnitScore.
     * @return True if the given UnitScore a has a value that is equal to 0.
     */
    public static boolean isZero(UnitScore a)
    {
        return Math.abs(a.getValue()) < Double.MIN_VALUE;
    }
    
    /**
     * Verifies if the given UnitScore is equal to ONE
     * @param a The given UnitScore.
     * @return True if the given UnitScore a has a value that is equal to 1.
     */
    public static boolean isOne(UnitScore a)
    {
        return Math.abs(ONE.getValue() - a.getValue()) < Double.MIN_VALUE;
    }

    public static UnitScore plus(UnitScore a, UnitScore b)
    {
        return new UnitScore(a.getValue() + b.getValue());
    }
    
    public static UnitScore minus(UnitScore a, UnitScore b)
    {
        return new UnitScore(Math.max(a.getValue() - b.getValue(), 0));
    }
    
    @Override
    /**
     * Compares this UnitScore with the UnitScore o by directly comparing their values.
     * Returns a positive value if this UnitScore has a value strictly greater than the value of o,
     * a negative value if this UnitScore has a value strictly smaller than the value of o and 0 otherwise.
     */
    public int compareTo(UnitScore o)
    {
        return Double.compare(getValue(), o.getValue());
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + (int) (Double.doubleToLongBits(this.value) ^ (Double.doubleToLongBits(this.value) >>> 32));
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
        final UnitScore other = (UnitScore) obj;
        return Double.doubleToLongBits(this.value) == Double.doubleToLongBits(other.value);
    }

    @Override
    public String toString()
    {
        return Double.toString(value);
    }    
}
