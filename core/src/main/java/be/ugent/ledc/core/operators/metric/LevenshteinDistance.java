package be.ugent.ledc.core.operators.metric;

public class LevenshteinDistance implements Metric<Integer, String>
{
    @Override
    /**
     * Compute the Levenshtein distance between Strings s1 and s2
     *
     * @param s1 string 1
     * @param s2 string 2
     * @return levenshtein distance between string s1 and s2
     */
    public Integer distance(String s1, String s2)
    {
        return computeLevenshteinDistance(s1, s2);
    }

    /**
     * Compute the levenshtein distance between two strings
     *
     * @param s1 string 1
     * @param s2 string 2
     * @return levenshtein distance
     */
    private int computeLevenshteinDistance(String s1, String s2)
    {
        int[][] distanceMatrix = computeDistanceMatrix(s1, s2);
        return distanceMatrix[s1.length()][s2.length()];
    }

    /**
     * Compute the distance matrix (LevenshteinDistance) between two given strings.
     *
     * @param s1 string 1
     * @param s2 string 2
     * @return levenshtein distance matrix
     */
    public static int[][] computeDistanceMatrix(String s1, String s2)
    {
        int[][] d = new int[s1.length() + 1][s2.length() + 1];
        // initial values in column 0 and row 0
        for (int i = 0; i < s1.length() + 1; i++)
        {
            d[i][0] = i;
        }
        for (int i = 0; i < s2.length() + 1; i++)
        {
            d[0][i] = i;
        }
        // calculate distance matrix
        for (int i = 1; i < s1.length() + 1; i++)
        {
            for (int j = 1; j < s2.length() + 1; j++)
            {
                int cost = 0;
                if (s2.charAt(j - 1) != s1.charAt(i - 1))
                {
                    cost = 1;
                }
                d[i][j] = Math.min(d[i - 1][j] + 1, Math.min(d[i][j - 1] + 1, d[i - 1][j - 1] + cost));
            }
        }
        return d;
    }
    
    @Override
    public String toString() {
        return "Levenshtein";
    }
}
