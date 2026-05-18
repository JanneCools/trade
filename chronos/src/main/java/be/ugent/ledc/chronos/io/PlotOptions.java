package be.ugent.ledc.chronos.io;

import be.ugent.ledc.core.operators.UnitScore;
import be.ugent.ledc.core.util.ListOperations;
import java.util.List;

public class PlotOptions
{   
    private final List<String> hexColors;
    
    private final UnitScore sampleRate;
    
    private final String title;
    
    private final String xAxisCaption;
    
    private final String yAxisCaption;
    
    private final double minY;
    
    private final double maxY;

    public PlotOptions(List<String> hexColors, UnitScore sampleRate, String title, String xAxisCaption, String yAxisCaption, double minY, double maxY)
    {
        this.hexColors = hexColors;
        this.sampleRate = sampleRate;
        this.title = title;
        this.xAxisCaption = xAxisCaption;
        this.yAxisCaption = yAxisCaption;
        this.minY = minY;
        this.maxY = maxY;
    }

    
    public static class PlotOptionsBuilder
    {    
        private List<String> hexColors;

        private UnitScore sampleRate;

        private String title;
    
        private String xAxisCaption;

        private String yAxisCaption;
        
        private double minY;
    
        private double maxY;
        
        public PlotOptionsBuilder()
        {
            this.hexColors = ListOperations.list("#00008B");
            this.sampleRate = UnitScore.ONE;
            this.title = "";
            this.xAxisCaption = "";
            this.yAxisCaption = "";
            this.minY = 0;
            this.maxY = 1;
        }
        
        public PlotOptions build()
        {
            return new PlotOptions(
                hexColors,
                sampleRate,
                title,
                xAxisCaption,
                yAxisCaption,
                minY,
                maxY
            );
        }
        
        public PlotOptionsBuilder withHexColors(List<String> hexColors)
        {
            this.hexColors = hexColors;
            return this;
        }
        
        public PlotOptionsBuilder withSampleRate(UnitScore sampleRate)
        {
            this.sampleRate = sampleRate;
            return this;
        }
        
        public PlotOptionsBuilder withTitle(String title)
        {
            this.title = title;
            return this;
        }
        
        public PlotOptionsBuilder withXAxisCaption(String xAxisCaption)
        {
            this.xAxisCaption = xAxisCaption;
            return this;
        }
        
        public PlotOptionsBuilder withYAxisCaption(String yAxisCaption)
        {
            this.yAxisCaption = yAxisCaption;
            return this;
        }
        
        public PlotOptionsBuilder withMinY(double minY)
        {
            this.minY = minY;
            return this;
        }
        
        public PlotOptionsBuilder withMaxY(double maxY)
        {
            this.maxY = maxY;
            return this;
        }
    }

    public List<String> getHexColors() {
        return hexColors;
    }

    public UnitScore getSampleRate() {
        return sampleRate;
    }

    public String getTitle() {
        return title;
    }

    public String getxAxisCaption() {
        return xAxisCaption;
    }

    public String getyAxisCaption() {
        return yAxisCaption;
    }

    public double getMinY() {
        return minY;
    }

    public double getMaxY() {
        return maxY;
    }
    
    
}
