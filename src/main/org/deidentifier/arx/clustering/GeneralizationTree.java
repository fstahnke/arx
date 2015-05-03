package org.deidentifier.arx.clustering;

import java.util.HashMap;
import org.deidentifier.arx.framework.data.GeneralizationHierarchy;

public class GeneralizationTree {
    
    final GeneralizationHierarchy   hierarchy;
    final int[][]                   hierarchyArray;
    final int[][]                   cardinalityCache;
    final int                       attributeCardinality;
	final int                       maxLevel;
    
    public GeneralizationTree(GeneralizationHierarchy hierarchy) {
        this.hierarchy = hierarchy;
        maxLevel = hierarchy.getHeight() - 1;
        hierarchyArray = hierarchy.getArray();
        cardinalityCache = getCardinalities();
        attributeCardinality = hierarchyArray[0][maxLevel];
    }
    
    public int getGeneralizationLevel(final int[] values, int lvl) {
        
        int val = hierarchyArray[values[0]][lvl];
        
        for (int i = 1; i < values.length && lvl != maxLevel; i++) {
            while (hierarchyArray[values[i]][lvl] != val) {
                val = hierarchyArray[values[i - 1]][++lvl];
            }
        }
        return lvl;
    }
    
    public int getGeneralizationLevel(final int first, final int second, int lvl) {
        for (int val = hierarchyArray[first][lvl]; hierarchyArray[second][lvl] != val; val = hierarchyArray[first][++lvl]);
        return lvl;
    }
    
    public int getTransformation(final int value, final int lvl) {
        return hierarchyArray[value][lvl];
    }
    
    public int getCardinality(final int value, final int lvl) {
        return cardinalityCache[value][lvl];
    }
    
    private int[][] getCardinalities() {
        
        final HashMap<Integer, Integer> cardHashMap = new HashMap<>(hierarchyArray.length + hierarchyArray[0].length);
        
        for (final int[] record : hierarchyArray) {
            for (final int i : record) {
                if (cardHashMap.containsKey(i)) {
                    cardHashMap.put(i, cardHashMap.get(i) + 1);
                } else {
                    cardHashMap.put(i, 1);
                }
            }
        }
        
        final int[][] cardinalities = new int[hierarchyArray.length][hierarchyArray[0].length];
        
        for (int i = 0; i < hierarchyArray.length; i++) {
            for (int j = 0; j < hierarchyArray[0].length; j++) {
                cardinalities[i][j] = cardHashMap.get(hierarchyArray[i][j]);
            }
        }
        
        return cardinalities;
    }
}
