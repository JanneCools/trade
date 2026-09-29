package core.config;

import core.ParseException;

public interface FeatureConsumer<T>
{
    T buildFromFeatures(FeatureMap featureMap) throws ParseException;
}
