import com.moandjiezana.toml.TomlWriter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Validator<T extends Comparable<? super T>> {

    public static class Location<T extends Comparable<? super T>> {
        public final int index;
        public final T indexValue;
        public final String attribute;
        public final String originalValue;
        public final String repairedValue;

        public Location(int index, String attribute, String originalValue, String repairedValue) {
            this.index = index;
            this.indexValue = null;
            this.attribute = attribute;
            this.originalValue = originalValue;
            this.repairedValue = repairedValue;
        }

        public Location(int index, T indexValue, String attribute, String originalValue, String repairedValue) {
            this.index = index;
            this.indexValue = indexValue;
            this.attribute = attribute;
            this.originalValue = originalValue;
            this.repairedValue = repairedValue;
        }
    }

    private final int numRepairs;
    private Map<String, List<Location<T>>> partitionedLocations;

    public Validator(String validationPath) throws IOException {
        List<String> lines = Files.readAllLines(new File(Paths.get(validationPath).toUri()).toPath().toAbsolutePath(), Charset.defaultCharset());
        numRepairs = lines.size();
        partitionedLocations = new HashMap<>();
        for (String line: lines) {
            String[] info = line.split(";");
            String partitionValue = info[0].trim();
            int lineIndex = Integer.parseInt(info[1].trim());
            String attribute = info[2].trim();
            String correctValue = info[3].trim();
            String incorrectValue = info[4].trim();

            if (!partitionedLocations.containsKey(partitionValue)) partitionedLocations.put(partitionValue, new ArrayList<>());
            partitionedLocations.get(partitionValue).add(new Location<>(lineIndex, attribute, incorrectValue, correctValue));
        }

        // sort by line index
        partitionedLocations.replaceAll((key, list) -> {
            list.sort(Comparator.comparingInt(loc -> loc.index));
            return list;
        });
    }

    public Map<String, List<Location<T>>> getPartitionedLocations() {
        return partitionedLocations;
    }

    public void setPartitionedLocations(Map<String, List<Location<T>>> partitionedLocations) {
        this.partitionedLocations = partitionedLocations;
    }

    public List<Double> validate(String outputfile, Map<String, List<Location<T>>> repairLocations, int totalCells) throws IOException {
        Map<String, Object> data = new HashMap<>();

        // keep track of some statistics
        long truePositives = 0;
        long falsePositives = 0;
        long falseNegatives = 0;
        long numRepairs = 0;
        long numShouldBeRepaired = this.numRepairs;

        // get all partition keys that have repairs
        Set<String> keys = new HashSet<>(this.partitionedLocations.keySet());
        keys.addAll(repairLocations.keySet());

        // inspect every partition key
        for (String key: keys) {
            SortedMap<T, Object> timestamps = new TreeMap<>();
            List<Location<T>> trueLocations = this.partitionedLocations.get(key);
            List<Location<T>> locations = repairLocations.get(key);

            // sort locations based on index and attribute
            if (trueLocations != null)
                trueLocations.sort((l1,l2) -> {
                    if (l1.index == l2.index) return l1.attribute.compareTo(l2.attribute);
                    else return l1.index - l2.index;
                });
            if (locations != null)
                locations.sort((l1,l2) -> {
                    if (l1.index == l2.index) return l1.attribute.compareTo(l2.attribute);
                    else return l1.index - l2.index;
                });
            int trueLocationsSize = trueLocations == null ? 0 : trueLocations.size();
            int locationsSize = locations == null ? 0 : locations.size();

            // compare repairs
            int i = 0;
            int j = 0;
            T curIndexValue = null;
            Map<String, String> info = new HashMap<>();
            while (i < trueLocationsSize && j < locationsSize) {
                Location<T> trueLocation = trueLocations.get(i);
                Location<T> repairLocation = locations.get(j);
                if (trueLocation.index < repairLocation.index) {
                    falseNegatives++;
                    if (curIndexValue != null && trueLocation.indexValue != curIndexValue) {
                        timestamps.put(curIndexValue, info);
                        info = new HashMap<>();
                    }
                    String statement = trueLocation.originalValue + " -> " + trueLocation.originalValue + " (FN: " + trueLocation.repairedValue + ")";
                    info.put(trueLocation.attribute, statement);
                    i++;
                    curIndexValue = trueLocation.indexValue;
                } else if (trueLocation.index > repairLocation.index) {
                    falsePositives++;
                    if (curIndexValue != null && repairLocation.indexValue != curIndexValue) {
                        timestamps.put(curIndexValue, info);
                        info = new HashMap<>();
                    }
                    String statement = repairLocation.originalValue + " -> " + repairLocation.repairedValue + " (FP)";
                    info.put(repairLocation.attribute, statement);
                    numRepairs++;
                    j++;
                    curIndexValue = repairLocation.indexValue;
                } else if (trueLocation.attribute.equals(repairLocation.attribute)) {
                    truePositives++;
                    if (curIndexValue != null && trueLocation.indexValue != curIndexValue) {
                        timestamps.put(curIndexValue, info);
                        info = new HashMap<>();
                    }
                    String statement = trueLocation.originalValue + " -> " + repairLocation.repairedValue + " (TP: " + trueLocation.repairedValue +")";
                    info.put(trueLocation.attribute, statement);
                    numRepairs++;
                    i++;
                    j++;
                    curIndexValue = trueLocation.indexValue;
                } else if (trueLocation.attribute.compareTo(repairLocation.attribute) < 0) {
                    falseNegatives++;
                    if (curIndexValue != null && trueLocation.indexValue != curIndexValue) {
                        timestamps.put(curIndexValue, info);
                        info = new HashMap<>();
                    }
                    String statement = trueLocation.originalValue + " -> " + trueLocation.originalValue + " (FN: " + trueLocation.repairedValue + ")";
                    info.put(trueLocation.attribute, statement);
                    i++;
                    curIndexValue = trueLocation.indexValue;
                } else {
                    falsePositives++;
                    if (curIndexValue != null && repairLocation.indexValue != curIndexValue) {
                        timestamps.put(curIndexValue, info);
                        info = new HashMap<>();
                    }
                    String statement = repairLocation.originalValue + " -> " + repairLocation.repairedValue + " (FP)";
                    info.put(repairLocation.attribute, statement);
                    numRepairs++;
                    j++;
                    curIndexValue = repairLocation.indexValue;
                }
            }

            while (i < trueLocationsSize) {
                Location<T> trueLocation = trueLocations.get(i);
                falseNegatives++;
                if (curIndexValue != null && trueLocation.indexValue != curIndexValue) {
                    timestamps.put(curIndexValue, info);
                    info = new HashMap<>();
                }
                String statement = trueLocation.originalValue + " -> " + trueLocation.originalValue + " (FN: " + trueLocation.repairedValue + ")";
                info.put(trueLocation.attribute, statement);
                i++;
                curIndexValue = trueLocation.indexValue;
            }

            while (j < locationsSize) {
                Location<T> repairLocation = locations.get(j);
                falsePositives++;
                if (curIndexValue != null && repairLocation.indexValue != curIndexValue) {
                    timestamps.put(curIndexValue, info);
                    info = new HashMap<>();
                }
                String statement = repairLocation.originalValue + " -> " + repairLocation.repairedValue + " (FP)";
                info.put(repairLocation.attribute, statement);
                numRepairs++;
                j++;
                curIndexValue = repairLocation.indexValue;
            }

            if (curIndexValue != null) timestamps.put(curIndexValue, info);
            data.put(key, timestamps);
        }

        long trueNegatives = totalCells - truePositives - falseNegatives - falsePositives;
        System.out.println("total cells: " + totalCells);

        double accuracy = (double) (truePositives + trueNegatives) / totalCells;
        double precision = (double) truePositives / (double) numRepairs;
        double recall = (double) truePositives / (double) numShouldBeRepaired;
        double f1 = (2 * precision * recall) / (precision + recall);

        // write global analysis
        data.put("expected_repairs", numShouldBeRepaired);
        data.put("actual_repairs", numRepairs);
        data.put("accuracy", accuracy);
        data.put("precision", precision);
        data.put("recall", recall);
        data.put("f1", f1);

        TomlWriter writer = new TomlWriter();
        File file = new File(outputfile);
        file.createNewFile();
        writer.write(data, file);

        return List.of(precision, recall, f1);
    }
}
