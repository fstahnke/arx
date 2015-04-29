/**
 * 
 */
package org.deidentifier.arx.clustering;

import java.util.Arrays;

/**
 * @author Fabian Stahnke
 *
 */
public final class TassaRecord extends AbstractCluster {
    
    /**
     * 
     */
    private static final long serialVersionUID = -7132802338053695462L;
    
    
    public final int[]  recordContent;
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

    @Override
    public double getGeneralizationCost() {
        return 0;
    }

    @Override
    public int[] getTransformedValues() {
        // return recordContent, because there is no generalization
        return recordContent;
    }
}
