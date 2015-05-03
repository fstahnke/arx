package org.deidentifier.arx.clustering;

public interface IGeneralizable {
    
    public int[] getValues();
    public double getGeneralizationCost();
    public int[][] getValuesByAttribute();
    
}
