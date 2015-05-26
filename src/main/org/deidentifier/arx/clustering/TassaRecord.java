/**
 * 
 */
package org.deidentifier.arx.clustering;

/**
 * @author Fabian Stahnke
 *
 */
public final class TassaRecord implements IGeneralizable {
    
    private final int[]  recordContent;
    //private final int   hashCode;
    
    private TassaCluster assignedCluster;
    
    public TassaRecord(int[] content)
    {
        recordContent = content;
        //hashCode = calculateHashCode();
    }
    /*
    private int calculateHashCode() {
        int result = 0;
        for (int i : recordContent) {
            result = result * 524287 + i;
        }
        return result;
    }
    */
    /*
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
            if (Arrays.equals(recordContent, ((TassaRecord) o).recordContent)) {
                return true;
            }
        }
        return false;
    }
    */
    
    public int[] getValues() {
        return recordContent;
    }
    
    public TassaCluster getAssignedCluster() {
        return assignedCluster;
    }
    public void setAssignedCluster(TassaCluster cluster) {
        assignedCluster = cluster;
    }
}
