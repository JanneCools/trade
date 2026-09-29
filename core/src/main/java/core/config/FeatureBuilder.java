package core.config;

import core.ParseException;

public interface FeatureBuilder<T>
{
    public FeatureMap buildFeatureMap(T t) throws ParseException;
}
