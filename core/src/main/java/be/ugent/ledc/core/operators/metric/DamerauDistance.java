package be.ugent.ledc.core.operators.metric;

public class DamerauDistance implements Metric<Integer, String>
{
    @Override
    public Integer distance(String s1, String s2)
    {
        return computeDamerauDistance(s1, s2);
    }

    private int computeDamerauDistance(String s1, String s2)
    {
        int[][] distanceMatrix = computeDistanceMatrix(s1, s2);
        return distanceMatrix[s1.length()][s2.length()];
    }

    public static int[][] computeDistanceMatrix(String s, String t)
    {
        int[][] d = new int[s.length() + 1][t.length() + 1];
        // initial values in column 0 and row 0
        for (int i = 0; i < s.length() + 1; i++)
        {
            d[i][0] = i;
        }
        for (int j = 0; j < t.length() + 1; j++)
        {
            d[0][j] = j;
        }
        
        // calculate distance matrix
        for (int i = 1; i < s.length() + 1; i++)
        {
            for (int j = 1; j < t.length() + 1; j++)
            {
                char a = s.charAt(i - 1);
                char b = t.charAt(j - 1);
                
                //Calculate the basic cost by comparing characters s[i] and t[j]
                int cost =  a == b ? 0 : 1;
                
                //The regular Levenshtein part
                d[i][j] = Math.min(d[i - 1][j] + 1, Math.min(d[i][j - 1] + 1, d[i - 1][j - 1] + cost));
                
                if(i > 1 && j > 1 && a == t.charAt(j - 2) && b == s.charAt(i - 2))
                {
                    d[i][j] = Math.min(d[i][j], d[i-2][j-2] + cost);
                }
            }
        }
        return d;
    }

    @Override
    public String toString() {
        return "Damerau";
    }
}
