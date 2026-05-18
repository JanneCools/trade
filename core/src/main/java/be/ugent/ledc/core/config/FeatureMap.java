package be.ugent.ledc.core.config;

import be.ugent.ledc.core.ParseException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;

public class FeatureMap
{   
    private final LinkedHashMap<String, Object> features;

    public FeatureMap()
    {
        this.features = new LinkedHashMap<>();
    }
    
    public FeatureMap addFeature(String featureName, Object featureValue)
    {
        this.features.put(featureName, featureValue);
        return this;
    }
    
    public FeatureMap addFeatureWhenNotNull(String featureName, Object featureValue)
    {
        if(featureValue != null)
            this.features.put(featureName, featureValue);
        return this;
    }
    
    public FeatureMap addFeatureWhenNotEmpty(String featureName, String featureValue)
    {
        if(featureValue != null && !featureValue.trim().isEmpty())
            this.features.put(featureName, featureValue);
        return this;
    }

    public LinkedHashMap<String, Object> getFeatures()
    {
        return features;
    }
    
    public Object get(String featureName)
    {
        return features.get(featureName);
    }
    
    public String getString(String featureName) throws ParseException
    {
        if(features.get(featureName) == null)
            return null;
        
        if(features.get(featureName) instanceof String)
            return (String) features.get(featureName);
        
        throw new ParseException("Cannot retrieve String for feature " + featureName);
    }
    
    public LocalDate getDate(String featureName) throws ParseException
    {
        if(features.get(featureName) == null)
            return null;
        
        if(features.get(featureName) instanceof String)
            return LocalDate.parse((String)features.get(featureName));
        
        throw new ParseException("Cannot retrieve LocalDate for feature " + featureName);
    }
    
    public Integer getInteger(String featureName) throws ParseException
    {
        if(features.get(featureName) == null)
            return null;
        
        if(features.get(featureName) instanceof Integer)
            return (Integer) features.get(featureName);
        
        throw new ParseException("Cannot retrieve Integer for feature " + featureName);
    }
    
    public Boolean getBoolean(String featureName) throws ParseException
    {
        if(features.get(featureName) == null)
            return null;
        
        if(features.get(featureName) instanceof Boolean)
            return (Boolean) features.get(featureName);
        
        throw new ParseException("Cannot retrieve Integer for feature " + featureName);
    }
    
    public Double getDouble(String featureName) throws ParseException
    {
        if(features.get(featureName) == null)
            return null;
               
        if(features.get(featureName) instanceof BigDecimal)
            return ((BigDecimal) features.get(featureName)).doubleValue();
        
        if(features.get(featureName) instanceof Integer)
            return ((Integer) features.get(featureName)).doubleValue();
        
        if(features.get(featureName) instanceof Double)
            return (Double) features.get(featureName);
        
        throw new ParseException("Cannot retrieve Double for feature " + featureName);
    }
    
    public FeatureMap getFeatureMap(String featureName) throws ParseException
    {
        if(features.get(featureName) == null)
            return null;
        
        if(features.get(featureName) instanceof FeatureMap)
            return (FeatureMap) features.get(featureName);
        
        throw new ParseException("Cannot retrieve FeatureMap for feature " + featureName);
    }
    
    public List getList(String featureName) throws ParseException
    {
        if(features.get(featureName) == null)
            return null;
        
        if(features.get(featureName) instanceof List)
            return (List) features.get(featureName);
        
        throw new ParseException("Cannot retrieve List for feature " + featureName);
    }
    
    @Override
    public String toString()
    {
        return features.toString();
    }
}
