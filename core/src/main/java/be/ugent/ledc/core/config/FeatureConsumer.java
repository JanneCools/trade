package be.ugent.ledc.core.config;

import be.ugent.ledc.core.ParseException;

public interface FeatureConsumer<T>
{
    T buildFromFeatures(FeatureMap featureMap) throws ParseException;
}
