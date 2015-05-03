/**
 * 
 */
package org.deidentifier.arx.clustering;

import java.util.Arrays;

/**
 * @author Fabian Stahnke
 *
 */
public final class TassaRecord implements IGeneralizable {
    
    private final int[]  recordContent;
    private final int   hashCode;
    
    private TassaCluster assignedCluster;
    
    public TassaRecord(int[] content)
    {
        recordContent = content;
        hashCode = calculateHashCode();
    }
    
    private int calculateHashCode() {
        int result = 0;
        for (int i : recordContent) {
            result = result * 524287 + i;
        }
        return result;
    }
    
    @Override
    public int hashCode() {
        return hashCode;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (this.hashCode() == o.hashCode() && o instanceof TassaRecord) {
            return (Arrays.equals(recordContent, ((TassaRecord) o).recordContent));
        }
        return false;
    }
    
    public double getGeneralizationCost() {
        return 0;
    }
    
    public int[] getValues() {
        return recordContent;
    }
    
    public int[] getTransformation() {
        // return recordContent, because there is no generalization
        return recordContent;
    }
    
    public TassaCluster getAssignedCluster() {
        return assignedCluster;
    }
    public void setAssignedCluster(TassaCluster cluster) {
        assignedCluster = cluster;
    }

    @Override
    public int[][] getValuesByAttribute() {
        // TODO Auto-generated method stub
        return null;
    }
}
