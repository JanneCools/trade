package be.ugent.ledc.core.datastructures;

import be.ugent.ledc.core.datastructures.Pair;

public class ComplexNumber extends Pair<Double,Double>
{
    public static final ComplexNumber ZERO = new ComplexNumber(0.0, 0.0);
    
    public ComplexNumber(Double real, Double imaginary)
    {
        super(real, imaginary);
    }
    
    public double getReal()
    {
        return super.getFirst();
    }
    
    public double getImaginary()
    {
        return super.getSecond();
    }
    
    @Override
    public String toString()
    {
        if (getImaginary() == 0)
            return Double.toString(getReal());
        if (getReal() == 0)
            return getImaginary()+ "i";
        
        return getImaginary() <  0 
            ? getReal() + " - " + (-getImaginary()) + "i"
            : getReal() + " + " + getImaginary() + "i";
    }

    public double abs()
    {
        return Math.hypot(getReal(), getImaginary());
    }

    public double phase() {
        return Math.atan2(getImaginary(), getReal());
    }

    public ComplexNumber plus(ComplexNumber b)
    {
        return new ComplexNumber(
            getReal() + b.getReal(),
            getImaginary() + b.getImaginary()
        );
    }

    public ComplexNumber minus(ComplexNumber b)
    {
        return new ComplexNumber(
            getReal() - b.getReal(),
            getImaginary() - b.getImaginary()
        );
    }

    public ComplexNumber times(ComplexNumber b)
    {    
        return new ComplexNumber(
            getReal() * b.getReal() - getImaginary() * b.getImaginary(),
            getReal() * b.getImaginary() - getImaginary() * b.getReal()
            
        );
    }

    public ComplexNumber scale(double s)
    {
        return new ComplexNumber(
            s * getReal(),
            s * getImaginary()
        );
    }

    public ComplexNumber conjugate()
    {
        return new ComplexNumber(getReal(), -getImaginary());
    }

    public ComplexNumber reciprocal()
    {
        double s = getReal() * getReal() + getImaginary() * getImaginary();
        return new ComplexNumber(
            getReal() / s,
            -getImaginary() / s);
    }
}
