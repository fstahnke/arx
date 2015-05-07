package org.deidentifier.arx.clustering;

public class ClusterPair implements IGeneralizable, Comparable<IGeneralizable> {
    
    private double generalizationCost;
    private final int hashCode;
    
    private TassaCluster first;
    private TassaCluster second;
    
    public ClusterPair(TassaCluster first, TassaCluster second) {
        this.first = first;
        this.second = second;
        generalizationCost = first.getAddedGC(second);
        hashCode = 31 + first.hashCode() * second.hashCode();
    }
    
    @Override
    public int[] getValues() {
        return first.getValues();
    }
    
    public TassaCluster getFirst() {
        return first;
    }
    
    public TassaCluster getSecond() {
        return second;
    }
    
    @Override
    public double getGeneralizationCost() {
        return generalizationCost;
    }
    
    @Override
    public int[][] getValuesByAttribute() {
        final int[] firstValues = first.getValues();
        final int[] secondValues = second.getValues();
        final int numAtt = firstValues.length;
        final int[][] valuesByAttribute = new int[numAtt][2];
        for(int i = 0; i < numAtt; i++) {
            valuesByAttribute[i][0] = firstValues[i];
            valuesByAttribute[i][1] = secondValues[i];
        }
        return valuesByAttribute;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ClusterPair) {
            ClusterPair other = (ClusterPair) obj;
            if (first == other.first && second == other.second) {
                return true;
            }
            if (first == other.second && second == other.first) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public int compareTo(IGeneralizable o) {
        return Double.compare(this.getGeneralizationCost(), o.getGeneralizationCost());
    }
    
}
