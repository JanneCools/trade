package be.ugent.ledc.chronos.algorithms.transform.fourier;

import be.ugent.ledc.chronos.datastructures.Signal;
import be.ugent.ledc.core.datastructures.ComplexNumber;
import be.ugent.ledc.core.statistics.Statistics;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FourierAnalysis
{
    
    public static ComplexNumber[] fastFourierTransform(Signal<Comparable<? super Comparable>, Number> signal)
    {
        return fft(pad(convertSignal(signal)));
    }
    
    private static ComplexNumber[] fft(ComplexNumber[] x)
    {
        int n = x.length;

        // base case
        if (n == 1) return new ComplexNumber[] { x[0] };

        // radix 2 Cooley-Tukey FFT
        if (n % 2 != 0)
        {
            throw new IllegalArgumentException("n is not a power of 2");
        }

        //FFT of even terms
        ComplexNumber[] even = new ComplexNumber[n/2];
        for (int k = 0; k < n/2; k++)
        {
            even[k] = x[2*k];
        }
        
        ComplexNumber[] evenFFT = fft(even);

        //FFT of odd terms
        ComplexNumber[] odd  = even;  
        
        for (int k = 0; k < n/2; k++)
        {
            odd[k] = x[2*k + 1];
        }
        
        ComplexNumber[] oddFFT = fft(odd);

        // combine
        ComplexNumber[] y = new ComplexNumber[n];
        
        for (int k = 0; k < n/2; k++)
        {
            double kth = -2 * k * Math.PI / n;
            ComplexNumber wk = new ComplexNumber(Math.cos(kth), Math.sin(kth));
            
            y[k]       = evenFFT[k].plus (wk.times(oddFFT[k]));
            y[k + n/2] = evenFFT[k].minus(wk.times(oddFFT[k]));
        }
        return y;
    }
    
    public static ComplexNumber[] ifft(ComplexNumber[] x)
    {
        int n = x.length;
        ComplexNumber[] y = new ComplexNumber[n];

        for (int i = 0; i < n; i++)
            y[i] = x[i].conjugate();

        y = fft(y);

        for (int i = 0; i < n; i++)
            y[i] = y[i].conjugate();

        //Scale
        for (int i = 0; i < n; i++)
            y[i] = y[i].scale(1.0 / n);

        return y;
    }
    
    public static ComplexNumber[] discreteFourierTransform(Signal<Comparable<? super Comparable>, Number> signal)
    {
        ComplexNumber[] x = convertSignal(signal);
        
        int n = x.length;
        ComplexNumber zero = new ComplexNumber(0.0, 0.0);
        ComplexNumber[] y = new ComplexNumber[n];
        
        for (int k = 0; k < n; k++)
        {
            y[k] = zero;
            
            for (int j = 0; j < n; j++)
            {
                int power = (k * j) % n;
                double kth = -2 * power *  Math.PI / n;
                ComplexNumber wkj = new ComplexNumber(Math.cos(kth), Math.sin(kth));
                y[k] = y[k].plus(x[j].times(wkj));
            }
        }
        return y;
    }

    public static ComplexNumber[] convertSignal(Signal<Comparable<? super Comparable>, Number> signal)
    {
        ComplexNumber[] complexArray = new ComplexNumber[signal.size()];
        
        int i=0;
        
        for(Comparable<? super Comparable> idx: signal.indexSet())
        {
            complexArray[i++] = signal.get(idx) == null
                ? ComplexNumber.ZERO
                : new ComplexNumber(signal.get(idx).doubleValue(), 0.0);
        }
        
        return complexArray;
    }

    private static ComplexNumber[] pad(ComplexNumber[] orig)
    {
        int n = orig.length;
//        System.out.println("Original: " + n);
//        System.out.println("log2: " + Statistics.log2(n));
//        System.out.println("ceil: " + Math.ceil(Statistics.log2(n)));
//        System.out.println("pow: " + Math.pow(2, Math.ceil(Statistics.log2(n))));
        
        ComplexNumber[] padded = Arrays.copyOf(
            orig,
            (int) Math.pow //Pad with zeros so new length is a power of two
            (
                2,
                Math.ceil(Statistics.log2(n))
            ) 
        );
        
        for(int i=orig.length; i<padded.length; i++)
        {
            padded[i] = ComplexNumber.ZERO;
        }
        
        return padded;
    }

    private static Set<Integer> peaks(ComplexNumber[] data)
    {
        List<Double> amplitudes = new ArrayList<>();
        
        for(int i=0; i<data.length; i++)
        {
            amplitudes.add(data[i].abs());
        }
        
        HashMap<String, Double> quartiles = Statistics.quartiles(amplitudes);
        
        Set<Integer> peaks = new HashSet<>();
        
        double thres = 1.2;
        
        double q3 = quartiles.get("Q3");
        
        for(int i=1; i<data.length-1; i++)
        {
            if(data[i].abs() > 1.5 * q3
                && data[i].abs()/data[i-1].abs() > thres
                && data[i].abs()/data[i+1].abs() > thres)
            {
                peaks.add(i);
            }
        }        
        
        peaks.remove(0);
        
        return peaks;
    }
}
