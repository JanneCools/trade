package be.ugent.ledc.sigma.repair.cost.functions;

import be.ugent.ledc.core.cost.CostFunction;
import be.ugent.ledc.core.dataset.DataObject;

import static java.lang.Integer.min;

public class EditDistanceCostFunction <T extends Comparable<? super T>> implements CostFunction<T> {

    @Override
    public int computeCost(T originalValue, T repairedValue, DataObject originalObject) {
        if (originalValue == null) {
            return 1;
        }

        if (repairedValue == null) {
            return Integer.MAX_VALUE;
        }

        // compute edit distance between the string values
        String origString = originalValue.toString();
        String repairedString = repairedValue.toString();
        int m = origString.length();
        int n = repairedString.length();
        int[][] distances = new int[m+1][n+1];

        for (int i = 0; i <= m; i++)
            distances[i][0] = i;
        for (int j = 0; j <= n; j++)
            distances[0][j] = j;

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                int penalty = origString.charAt(i-1) == repairedString.charAt(j-1) ? 0 : 1;
                distances[i][j] = min(
                        min(distances[i-1][j] + 1,distances[i][j-1] + 1),
                        distances[i-1][j-1] + penalty
                );
            }
        }

        return distances[m][n];
    }
}
