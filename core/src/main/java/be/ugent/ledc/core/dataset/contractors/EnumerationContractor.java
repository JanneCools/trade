package be.ugent.ledc.core.dataset.contractors;

import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EnumerationContractor<T> implements Predicate<T>
{
    private final Set<T> permittedValues;

    public EnumerationContractor(Set<T> permittedValues)
    {
        this.permittedValues = permittedValues;
    }
    
    public EnumerationContractor(T ...permittedValues)
    {
        this(Stream.of(permittedValues).collect(Collectors.toSet()));
    }

    @Override
    public boolean test(T t)
    {
        return permittedValues.contains(t);
    }

    public Set<T> getPermittedValues()
    {
        return permittedValues;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EnumerationContractor<?> that = (EnumerationContractor<?>) o;
        return Objects.equals(permittedValues, that.permittedValues);
    }

    @Override
    public int hashCode() {
        return Objects.hash(permittedValues);
    }

    @Override
    public String toString() {
        return "EnumerationContractor{" +
                "permittedValues=" + permittedValues +
                '}';
    }
}
