package be.ugent.ledc.chronos.datastructures;

import be.ugent.ledc.core.dataset.DataObject;

import java.time.temporal.Temporal;
import java.util.Objects;

public class TimePoint<T extends Temporal> {

    private final T index;
    private final DataObject dataObject;

    public TimePoint(T index, DataObject dataObject) {
        this.index = index;
        this.dataObject = dataObject;
    }

    public T getIndex() {
        return index;
    }

    public DataObject getDataObject() {
        return dataObject;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TimePoint<?> timePoint = (TimePoint<?>) o;
        return Objects.equals(index, timePoint.index) && Objects.equals(dataObject, timePoint.dataObject);
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, dataObject);
    }

    @Override
    public String toString() {
        return "TimePoint{" +
                "index=" + index +
                ", dataObject=" + dataObject +
                '}';
    }
}
