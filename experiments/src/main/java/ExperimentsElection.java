import chronos.ChronosException;
import chronos.datastructures.Signal;
import chronos.datastructures.TemporalDataset;
import chronos.rules.TRuleset;
import core.ParseException;
import core.RepairException;
import core.binding.DataReadException;
import core.cost.AggregateCostFunction;
import core.datastructures.Pair;
import sigma.datastructures.contracts.DateTimeContractor;
import sigma.datastructures.contracts.SigmaContractorFactory;
import sigma.repair.NullBehavior;
import sigma.repair.bounding.PartitionedBounding;
import sigma.repair.cost.functions.*;
import sigma.repair.cost.models.NonConstantCostModel;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class ExperimentsElection extends Experiments<LocalDateTime> {

    public ExperimentsElection(
            String path, String queryFilename, String rulesetFilename, String timeAttribute, String partitionAttribute, boolean earlyStop) {
        super(path, queryFilename, rulesetFilename, timeAttribute, partitionAttribute, earlyStop);
    }

    public ExperimentsElection(ExperimentsElection other) {
        super(other);
    }

    @Override
    public void createTemporalDatasets() throws ChronosException {
        DateTimeContractor contractor = new DateTimeContractor(LocalDateTime::plusSeconds, ChronoUnit.SECONDS);
        this.partitionedDatasets = TemporalDataset.create(fullDataset, timeAttribute, contractor, partitionAttribute);
    }

    private <T extends Comparable<? super T>> Map<Integer, T> getMostFrequent(String attribute) throws ChronosException {
        Map<Integer, T> mapping = new HashMap<>();

        for (String key: this.partitionedDatasets.keySet()) {
            TemporalDataset<LocalDateTime> dataset = this.partitionedDatasets.get(key);
            Signal<LocalDateTime, T> signal = dataset.getAttributeSignal(attribute);

            Map<T, Integer> frequencies = new HashMap<>();
            LocalDateTime curr = signal.start();
            while (curr != null) {
                T value = signal.get(curr);
                if (value != null) {
                    int count = frequencies.getOrDefault(value, 0);
                    frequencies.put(value, count + 1);
                }
                curr = signal.nextIndex(curr);
            }

            int max = 0;
            T maxValue = null;
            for (Map.Entry<T, Integer> entry: frequencies.entrySet()) {
                if (entry.getValue() > max) {
                    max = entry.getValue();
                    maxValue = entry.getKey();
                }
            }
            mapping.put(Integer.parseInt(key), maxValue);
        }

        return mapping;
    }

    private Map<Integer, Long> getMostFrequentBins(String attribute, long binSize) throws ChronosException {
        Map<Integer, Long> mapping = new HashMap<>();

        for (String key: this.partitionedDatasets.keySet()) {
            TemporalDataset<LocalDateTime> dataset = this.partitionedDatasets.get(key);
            Signal<LocalDateTime, Long> signal = dataset.getAttributeSignal(attribute);

            Map<Long, Integer> frequencies = new HashMap<>();
            LocalDateTime curr = signal.start();
            while (curr != null) {
                Long value = signal.get(curr);
                if (value != null) {
                    long bin = value / binSize;
                    Long index = bin * binSize + (binSize / 2);
                    frequencies.merge(index, 1, Integer::sum);
                }
                curr = signal.nextIndex(curr);
            }

            int max = 0;
            Long maxValue = null;
            for (Map.Entry<Long, Integer> entry: frequencies.entrySet()) {
                if (entry.getValue() > max) {
                    max = entry.getValue();
                    maxValue = entry.getKey();
                }
            }
            mapping.put(Integer.parseInt(key), maxValue);
        }

        return mapping;
    }

    @Override
    protected NonConstantCostModel getNonConstantCostModel(Set<String> exclude) throws ChronosException {
        Map<String, IterableCostFunction<?>> costFunctions = new HashMap<>();

        BigDecimal zero = new BigDecimal("0");
        BigDecimal hundred = new BigDecimal("100");

        costFunctions.put("page_title", new IterableCostFunctionWrapper<>(new EditDistanceCostFunction<String>()));
        costFunctions.put("type", new IterableCostFunctionWrapper<>(new EditDistanceCostFunction<String>()));

        Map<Integer, Long> frequenciesVotes = getMostFrequent("votes_for_election");
        costFunctions.put("votes_for_election", new IterableCostFunctionWrapper<>(new PartitionTargetCostFunction<>(
                SigmaContractorFactory.LONG, SigmaContractorFactory.INTEGER, "page_id#curr", frequenciesVotes
        )));

        Map<Integer, Long> frequenciesNeededVotes = getMostFrequent("needed_votes");
        costFunctions.put("needed_votes", new IterableCostFunctionWrapper<>(new PartitionTargetCostFunction<>(
                SigmaContractorFactory.LONG, SigmaContractorFactory.INTEGER, "page_id#curr", frequenciesNeededVotes
        )));

        Map<Integer, BigDecimal> frequenciesTurnout = getMostFrequent("turnout");
        RangedDistanceCostFunction<BigDecimal,?> turnoutCost1 = new RangedDistanceCostFunction<>(SigmaContractorFactory.BIGDECIMAL_ONE_DECIMAL, zero, hundred);
        PartitionTargetCostFunction<BigDecimal,?,?,?> turnoutCost2 = new PartitionTargetCostFunction<>(
                SigmaContractorFactory.BIGDECIMAL_TWO_DECIMALS, SigmaContractorFactory.INTEGER, "page_id#curr", frequenciesTurnout
        );
        RoundingCostFunction<BigDecimal,?> turnoutCost3 = new RoundingCostFunction<>(SigmaContractorFactory.BIGDECIMAL_TWO_DECIMALS, new Pair<>(zero, hundred));
        costFunctions.put("turnout", new IterableCostFunctionWrapper<>(
                AggregateCostFunction.createAggregateByProduct(turnoutCost3, AggregateCostFunction.createAggregateBySum(Map.of(turnoutCost1, 1, turnoutCost2, 1)))
        ));

        Map<Integer, Long> frequenciesElectoral1 = getMostFrequent("electoral_vote1");
        RangedDistanceCostFunction<Long,?> electoral1Cost1 = new RangedDistanceCostFunction<>(SigmaContractorFactory.LONG, 1L, 1000L);
        PartitionTargetCostFunction<Long,?,?,?> electoral1Cost2 = new PartitionTargetCostFunction<>(
                SigmaContractorFactory.LONG, SigmaContractorFactory.INTEGER, "page_id#curr", frequenciesElectoral1
        );
        costFunctions.put("electoral_vote1", new IterableCostFunctionWrapper<>(AggregateCostFunction.createAggregateBySum(
                Map.of(electoral1Cost1, 1, electoral1Cost2, 1)
        )));

        Map<Integer, Long> frequenciesPopular1 = getMostFrequentBins("popular_vote1", 500000L);
        RangedDistanceCostFunction<Long,?> popular1Cost1 = new RangedDistanceCostFunction<>(SigmaContractorFactory.LONG, 1L, 2000000000L);
        PartitionTargetCostFunction<Long,?,?,?> popular1Cost2 = new PartitionTargetCostFunction<>(
                SigmaContractorFactory.LONG, SigmaContractorFactory.INTEGER, "page_id#curr", frequenciesPopular1
        );
        costFunctions.put("popular_vote1", new IterableCostFunctionWrapper<>(AggregateCostFunction.createAggregateBySum(
                Map.of(popular1Cost1, 1, popular1Cost2, 1)
        )));

        Map<Integer, BigDecimal> frequenciesPerc1 = getMostFrequent("percentage1");
        costFunctions.put("percentage1", new IterableCostFunctionWrapper<>(new PartitionTargetCostFunction<>(
                SigmaContractorFactory.BIGDECIMAL_ONE_DECIMAL, SigmaContractorFactory.INTEGER, "page_id#curr", frequenciesPerc1
        )));

        Map<Integer, Long> frequenciesElectoral2 = getMostFrequent("electoral_vote2");
        RangedDistanceCostFunction<Long,?> electoral2Cost1 = new RangedDistanceCostFunction<>(SigmaContractorFactory.LONG, 1L, 1000L);
        PartitionTargetCostFunction<Long,?,?,?> electoral2Cost2 = new PartitionTargetCostFunction<>(
                SigmaContractorFactory.LONG, SigmaContractorFactory.INTEGER, "page_id#curr", frequenciesElectoral2
        );
        costFunctions.put("electoral_vote2", new IterableCostFunctionWrapper<>(AggregateCostFunction.createAggregateBySum(
                Map.of(electoral2Cost1, 1, electoral2Cost2, 1)
        )));

        Map<Integer, Long> frequenciesPopular2 = getMostFrequentBins("popular_vote2", 500000L);
        RangedDistanceCostFunction<Long,?> popular2Cost1 = new RangedDistanceCostFunction<>(SigmaContractorFactory.LONG, 1L, 2000000000L);
        PartitionTargetCostFunction<Long,?,?,?> popular2Cost2 = new PartitionTargetCostFunction<>(
                SigmaContractorFactory.LONG, SigmaContractorFactory.INTEGER, "page_id#curr", frequenciesPopular2
        );
        costFunctions.put("popular_vote2", new IterableCostFunctionWrapper<>(AggregateCostFunction.createAggregateBySum(
                Map.of(popular2Cost1, 1, popular2Cost2, 1)
        )));

        Map<Integer, BigDecimal> frequenciesPerc2 = getMostFrequent("percentage2");
        costFunctions.put("percentage2", new IterableCostFunctionWrapper<>(new PartitionTargetCostFunction<>(
                SigmaContractorFactory.BIGDECIMAL_ONE_DECIMAL, SigmaContractorFactory.INTEGER, "page_id#curr", frequenciesPerc2
        )));

        return new NonConstantCostModel(TRuleset.unfold(costFunctions), Map.of(
                "popular_vote1#curr", new PartitionedBounding<>(this.fullDataset, "page_id", SigmaContractorFactory.LONG, "popular_vote1", 40000),
                "popular_vote1#next", new PartitionedBounding<>(this.fullDataset, "page_id", SigmaContractorFactory.LONG, "popular_vote1", 40000),
                "popular_vote2#curr", new PartitionedBounding<>(this.fullDataset, "page_id", SigmaContractorFactory.LONG, "popular_vote2", 40000),
                "popular_vote2#next", new PartitionedBounding<>(this.fullDataset, "page_id", SigmaContractorFactory.LONG, "popular_vote2", 40000)
        ));
    }

    public static void main(String[] args) throws ChronosException, IOException, ParseException, RepairException, DataReadException {
        long startTotal = System.currentTimeMillis();

        String path = "data/election";
        String datasetFilename = "dataset.csv";
        String rulesetFilename = "rules_stationary.rbx";
        String timeAttribute = "value_valid_from";
        String partitionAttribute = "page_id";

        boolean earlyStop = true;
        boolean baseline = true;
        int numAnchors = 1;

        boolean reportOnSeriesLevel = false;

        Set<String> validateAttributes = Set.of(
                "votes_for_election", "needed_votes", "turnout",
                "electoral_vote1", "popular_vote1", "percentage1",
                "electoral_vote2", "popular_vote2", "percentage2"
        );

        System.out.println("Running ExperimentsElection with " + (baseline ? "baseline" : "customized") + " cost model and " + numAnchors + " anchors.");

        int amount = 10;
        double precision = 0.0;
        double recall = 0.0;
        double f1 = 0.0;
        double executionTime = 0.0;
        double executionTimePreprocessing = 0.0;
        Map<String, Validator.Metrics> metricsPerAttribute = validateAttributes.stream().collect(Collectors.toMap(
                a -> a, a -> new Validator.Metrics()
        ));
        Validator<LocalDateTime> validator = new Validator<>(path + "/error_locations.txt", validateAttributes);
        for (int i = 0; i < amount; i++) {
            System.out.println("Run " + i);
            ExperimentsElection rw = new ExperimentsElection(path, datasetFilename, rulesetFilename, timeAttribute, partitionAttribute, earlyStop);

            rw.readDatasetFromPath(";");
            rw.readRulesFromPath();
            double durationPreprocessing = rw.initialize(baseline, new HashSet<>(), NullBehavior.NO_REPAIR);
            double duration = rw.run(numAnchors, validator, new HashSet<>(), validateAttributes);

            int numCells = rw.fullDataset.getSize() * rw.fullDataset.getContract().getAttributes().size();
            validator.setPartitionedLocations(rw.convertRepairLocations(validator.getPartitionedLocations()));
            Map<String, List<Validator.Location<LocalDateTime>>> convertedLocations = rw.convertRepairLocations();
            List<Double> metrics = validator.validate(rw.validationPath, convertedLocations, numCells);
            precision += metrics.get(0);
            recall += metrics.get(1);
            f1 += metrics.get(2);
            executionTime += duration;
            executionTimePreprocessing += durationPreprocessing;
            System.out.println(durationPreprocessing + " + " + duration + ": " + metrics);

            if (reportOnSeriesLevel) {
                Map<String, Validator.Metrics> metrics2 = validator.validateTimeSeriesPerAttributes(
                        rw.partitionedDatasets.keySet(), validateAttributes, convertedLocations
                );
                for (String attribute : validateAttributes) {
                    Validator.Metrics attrMetrics = metricsPerAttribute.get(attribute);
                    Validator.Metrics curMetrics = metrics2.get(attribute);
                    attrMetrics.truePositives += curMetrics.truePositives;
                    attrMetrics.numRepairs += curMetrics.numRepairs;
                    attrMetrics.numShouldBeRepaired += curMetrics.numShouldBeRepaired;
                    attrMetrics.precisions.add(curMetrics.precision);
                    attrMetrics.recalls.add(curMetrics.recall);
                    attrMetrics.f1s.add(curMetrics.f1);
                }
            }
        }
        System.out.println("Avg precision: " + precision / amount);
        System.out.println("Avg recall: " + recall / amount);
        System.out.println("Avg f1: " + f1 / amount);
        System.out.println("Avg execution time preprocessing: " + executionTimePreprocessing / amount + " seconds");
        System.out.println("Avg execution time: " + executionTime / amount + " seconds");

        if (reportOnSeriesLevel) {
            System.out.println("\nMetrics per attribute:");
            for (String attribute : validateAttributes) {
                double avgPrecision2 = metricsPerAttribute.get(attribute).precisions.stream()
                        .mapToDouble(Double::doubleValue).average().orElse(0.0);
                double avgRecall2 = metricsPerAttribute.get(attribute).recalls.stream()
                        .mapToDouble(Double::doubleValue).average().orElse(0.0);
                double avgF12 = metricsPerAttribute.get(attribute).f1s.stream()
                        .mapToDouble(Double::doubleValue).average().orElse(0.0);

                double stdPrecision2 = getStd(metricsPerAttribute.get(attribute).precisions, avgPrecision2);
                double stdRecall2 = getStd(metricsPerAttribute.get(attribute).recalls, avgRecall2);
                double stdF12 = getStd(metricsPerAttribute.get(attribute).f1s, avgF12);

                System.out.println("\t" + attribute + ":");
                System.out.println("\t\t Avg NumShouldBeRepaired: " + metricsPerAttribute.get(attribute).numShouldBeRepaired);
                System.out.println("\t\t Avg NumRepairs: " + metricsPerAttribute.get(attribute).numRepairs);
                System.out.println("\t\tAvg precision: " + avgPrecision2 + " (with std " + stdPrecision2 + ")");
                System.out.println("\t\tAvg recall: " + avgRecall2 + " (with std " + stdRecall2 + ")");
                System.out.println("\t\tAvg f1: " + avgF12 + " (with std " + stdF12 + ")");
            }
        }

        long endTotal = System.currentTimeMillis();
        System.out.println("Total runtime: " + (endTotal - startTotal)/1000.0 + " seconds");
        System.out.println("Running ExperimentsElection with " + (baseline ? "baseline" : "customized") + " cost model and " + numAnchors + " anchors.");
    }

//    public static void main(String[] args) throws ChronosException, IOException, ParseException, RepairException, DataReadException {
//        long startTotal = System.currentTimeMillis();
//
//        String path = "data/election";
//        String datasetFilename = "dataset.csv";
//        String rulesetFilename = "rules_stationary.rbx";
//        String timeAttribute = "value_valid_from";
//        String partitionAttribute = "page_id";
//
//        boolean earlyStop = true;
//        boolean baseline = true;
//        int numAnchors = 5;
//
//        boolean reportOnSeriesLevel = true;
//
//        Set<String> validateAttributes = Set.of(
//                "votes_for_election", "needed_votes", "turnout",
//                "electoral_vote1", "popular_vote1", "percentage1",
//                "electoral_vote2", "popular_vote2", "percentage2"
//        );
//
//        ExperimentsElection rw = new ExperimentsElection(path, datasetFilename, rulesetFilename, timeAttribute, partitionAttribute, earlyStop);
//
//        System.out.println("Running ExperimentsElection with " + (baseline ? "baseline" : "customized") + " cost model and " + numAnchors + " anchors.");
//
//        // read dataset and rules
//        long start = System.currentTimeMillis();
//        rw.readDatasetFromPath(";");
//        rw.readRulesFromPath();
//        long stop = System.currentTimeMillis();
//        System.out.println("Time for reading dataset and rules: " + (stop - start)/1000.0 + " seconds");
//
//        int numCells = rw.fullDataset.getSize() * rw.fullDataset.getContract().getAttributes().size();
//        System.out.println("Total cells: " + numCells);
//
//        double dur = rw.initialize(baseline, new HashSet<>(), NullBehavior.NO_REPAIR);
//        System.out.println("Time for initialization: " + dur + " seconds");
//
//        // execute repair
//        int amount = 10;
//        List<Double> precisions = new ArrayList<>();
//        List<Double> recalls = new ArrayList<>();
//        List<Double> f1s = new ArrayList<>();
//        double executionTime = 0.0;
//        Map<String, Validator.Metrics> metricsPerAttribute = validateAttributes.stream().collect(Collectors.toMap(
//                a -> a, a -> new Validator.Metrics()
//        ));
//        Validator<LocalDateTime> validator = new Validator<>(path + "/error_locations.txt", validateAttributes);
//        for (int i = 0; i < amount; i++) {
//            System.out.println("Run " + i);
//            ExperimentsElection copy = new ExperimentsElection(rw);
//            double duration = copy.run(numAnchors, validator, new HashSet<>(), validateAttributes);
//
//            validator.setPartitionedLocations(copy.convertRepairLocations(validator.getPartitionedLocations()));
//            Map<String, List<Validator.Location<LocalDateTime>>> convertedLocations = copy.convertRepairLocations();
//            List<Double> metrics = validator.validate(copy.validationPath, convertedLocations, numCells);
//            precisions.add(metrics.get(0));
//            recalls.add(metrics.get(1));
//            f1s.add(metrics.get(2));
//            executionTime += duration;
//            System.out.println(duration + ": " + metrics);
//
//            if (reportOnSeriesLevel) {
//                Map<String, Validator.Metrics> metrics2 = validator.validateTimeSeriesPerAttributes(
//                        rw.partitionedDatasets.keySet(), validateAttributes, convertedLocations
//                );
//                for (String attribute : validateAttributes) {
//                    Validator.Metrics attrMetrics = metricsPerAttribute.get(attribute);
//                    Validator.Metrics curMetrics = metrics2.get(attribute);
//                    attrMetrics.truePositives += curMetrics.truePositives;
//                    attrMetrics.numRepairs += curMetrics.numRepairs;
//                    attrMetrics.numShouldBeRepaired += curMetrics.numShouldBeRepaired;
//                    attrMetrics.precisions.add(curMetrics.precision);
//                    attrMetrics.recalls.add(curMetrics.recall);
//                    attrMetrics.f1s.add(curMetrics.f1);
//                }
//            }
//        }
//
//        double avgPrecision = precisions.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
//        double avgRecall = recalls.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
//        double avgF1 = f1s.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
//
//        double stdPrecision = getStd(precisions, avgPrecision);
//        double stdRecall = getStd(recalls, avgRecall);
//        double stdF1 = getStd(f1s, avgF1);
//
//        System.out.println("Avg precision: " + avgPrecision + " (with std " + stdPrecision + ")");
//        System.out.println("Avg recall: " + avgRecall + " (with std " + stdRecall + ")");
//        System.out.println("Avg f1: " + avgF1 + " (with std " + stdF1 + ")");
//        System.out.println("Execution time preprocessing: " + dur + " seconds");
//        System.out.println("Avg execution time: " + executionTime / amount + " seconds");
//
//        if (reportOnSeriesLevel) {
//            System.out.println("\nMetrics per attribute:");
//            for (String attribute : validateAttributes) {
//                double avgPrecision2 = metricsPerAttribute.get(attribute).precisions.stream()
//                        .mapToDouble(Double::doubleValue).average().orElse(0.0);
//                double avgRecall2 = metricsPerAttribute.get(attribute).recalls.stream()
//                        .mapToDouble(Double::doubleValue).average().orElse(0.0);
//                double avgF12 = metricsPerAttribute.get(attribute).f1s.stream()
//                        .mapToDouble(Double::doubleValue).average().orElse(0.0);
//
//                double stdPrecision2 = getStd(metricsPerAttribute.get(attribute).precisions, avgPrecision2);
//                double stdRecall2 = getStd(metricsPerAttribute.get(attribute).recalls, avgRecall2);
//                double stdF12 = getStd(metricsPerAttribute.get(attribute).f1s, avgF12);
//
//                System.out.println("\t" + attribute + ":");
//                System.out.println("\t\t Avg NumShouldBeRepaired: " + metricsPerAttribute.get(attribute).numShouldBeRepaired);
//                System.out.println("\t\t Avg NumRepairs: " + metricsPerAttribute.get(attribute).numRepairs);
//                System.out.println("\t\tAvg precision: " + avgPrecision2 + " (with std " + stdPrecision2 + ")");
//                System.out.println("\t\tAvg recall: " + avgRecall2 + " (with std " + stdRecall2 + ")");
//                System.out.println("\t\tAvg f1: " + avgF12 + " (with std " + stdF12 + ")");
//            }
//        }
//
//        long endTotal = System.currentTimeMillis();
//        System.out.println("Total runtime: " + (endTotal - startTotal)/1000.0/60.0 + " minutes");
//        System.out.println("Running ExperimentsElection with " + (baseline ? "baseline" : "customized") + " cost model and " + numAnchors + " anchors.");
//    }

}
