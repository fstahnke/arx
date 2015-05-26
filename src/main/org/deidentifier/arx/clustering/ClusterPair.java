package org.deidentifier.arx.clustering;

public class ClusterPair implements Comparable<TassaCluster> {
    
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
    
    public TassaCluster getFirst() {
        return first;
    }
    
    public TassaCluster getSecond() {
        return second;
    }
    
    public double getGeneralizationCost() {
        return generalizationCost;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        /*
        if (obj instanceof ClusterPair) {
            ClusterPair other = (ClusterPair) obj;
            if (first == other.first && second == other.second) {
                return true;
            }
            if (first == other.second && second == other.first) {
                return true;
            }
        }
        */
        return false;
    }
    
    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public int compareTo(TassaCluster o) {
        return Double.compare(this.getGeneralizationCost(), o.getGC());
    }
    
}
