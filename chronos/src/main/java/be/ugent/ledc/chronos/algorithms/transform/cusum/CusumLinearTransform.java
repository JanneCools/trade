package be.ugent.ledc.chronos.algorithms.transform.cusum;

import be.ugent.ledc.chronos.datastructures.Signal;
import be.ugent.ledc.core.datastructures.Interval;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

public class CusumLinearTransform<I extends Comparable<? super I>, N extends Number> extends CusumTransform<I,N,Double>
{

    public CusumLinearTransform(int window)
    {
        super(window);
    }

    public CusumLinearTransform(){}
    

    @Override
    public double analyze(Signal<I, N> left, Signal<I, N> right)
    {
        long[] xLeft = xValues(left, left.start());
        long[] xRight= xValues(right, left.start());
        
        for(int i=0;i<xLeft.length;i++)
            xLeft[i] = i;
        
        for(int i=0;i<xRight.length;i++)
            xRight[i] = xLeft.length + i;
        
        double[] yLeft = yValues(left);
        double[] yRight= yValues(right);
        
        //Estimate linear model for left
        double betaLeft = beta(xLeft, yLeft);
        double alphaLeft = alpha(xLeft, yLeft, betaLeft);
        
        //Estimate linear model for right
        double betaRight = beta(xRight, yRight);
        double alphaRight = alpha(xRight, yRight, betaRight);
        
        double d1 = IntStream
            .range(0, xRight.length)
            .mapToDouble(i ->
                Math.abs(yRight[i] - alphaLeft  - xRight[i] * betaLeft) -
                Math.abs(yRight[i] - alphaRight - xRight[i] * betaRight))
            .sum();
        
        double d2 = IntStream
            .range(0, xLeft.length)
            .mapToDouble
            (i -> Math.abs(yLeft[i] - alphaRight - xLeft[i] * betaRight) - 
                  Math.abs(yLeft[i] - alphaLeft - xLeft[i] * betaLeft))
            .sum();
        
        return Math.max(d1,d2);
        
    }
    
    private long[] xValues(Signal<I,N> s, I zero)
    {
        long[] xValues = new long[s.size()];
        
        int i = 0;
        
        for(I idx: s.indexSet())
        {
            xValues[i++] = s
                .getIndexContractor()
                .cardinality(new Interval<>(zero, idx, false, true));
        }
        
        return xValues;
    }
    
    private double[] yValues(Signal<I,N> s)
    {
        double[] yValues = new double[s.size()];
        
        int i = 0;
        
        for(I idx: s.indexSet())
        {
            yValues[i++] = s.get(idx).doubleValue();
        }
        
        return yValues;
    }
    
    private double beta(long[] x, double[] y)
    {
        double xySum = 0.0;
        double xSum = 0.0;
        double x2Sum = 0.0;
        double ySum = 0.0;
        
        for(int i=0; i<x.length; i++)
        {
            xySum += (double)x[i] * y[i];
            xSum += x[i];
            x2Sum+= Math.pow(x[i], 2.0);
            ySum += y[i];
        }
        
        return ((x.length * xySum) - xSum * ySum) / ((x.length * x2Sum) - Math.pow(xSum, 2.0));
    }
    
    private double alpha(long[] x, double[] y, double beta)
    {
        double avgX = LongStream.of(x).summaryStatistics().getAverage();
        double avgY = DoubleStream.of(y).summaryStatistics().getAverage();
        
        return avgY - beta * avgX;
    }
}
