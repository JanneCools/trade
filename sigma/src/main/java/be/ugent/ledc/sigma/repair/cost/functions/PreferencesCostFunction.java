package be.ugent.ledc.sigma.repair.cost.functions;

import be.ugent.ledc.core.cost.CostFunction;
import be.ugent.ledc.core.dataset.DataObject;

import java.util.Set;

public class PreferencesCostFunction<T extends Comparable<? super T>> implements CostFunction<T> {

    // preferences of values to repair
    final Set<T> preferences;

    // repair preferences for a null value
    private final Set<T> nullPreferences;

    public PreferencesCostFunction(Set<T> preferences, Set<T> nullPreferences) {
        this.preferences = preferences;
        this.nullPreferences = nullPreferences;
    }

    @Override
    public int computeCost(T originalValue, T repairedValue, DataObject originalObject) {
        if (repairedValue == null)
            return Integer.MAX_VALUE;

        if (originalValue == null) {
            if (nullPreferences.isEmpty() || nullPreferences.contains(repairedValue))
                return 1;
            else
                return 2;
        }

        if (preferences.isEmpty() || preferences.contains(originalValue))
            return 1;
        else
            return 2;
    }
}
