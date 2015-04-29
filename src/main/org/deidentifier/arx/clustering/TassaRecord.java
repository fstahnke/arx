/**
 * 
 */
package org.deidentifier.arx.clustering;

import java.util.Arrays;

/**
 * @author Fabian Stahnke
 *
 */
public class TassaRecord {
    
    public final int[]  recordContent;
    public TassaCluster assignedCluster;
    private final int   hashCode;
    
    public TassaRecord(int[] content)
    {
        recordContent = content;
        hashCode = Arrays.hashCode(recordContent);
    }
    
    @Override
    public int hashCode() {
        return hashCode;
    }
    
/*    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof TassaRecord) {
            return (Arrays.equals(recordContent, ((TassaRecord) o).recordContent));
        }
        return false;
    }*/
    
}
