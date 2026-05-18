package be.ugent.ledc.core.config;

import be.ugent.ledc.core.ParseException;

public interface FeatureBuilder<T>
{
    public FeatureMap buildFeatureMap(T t) throws ParseException;
}
