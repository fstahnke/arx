package org.deidentifier.arx.clustering;

import java.util.HashMap;
import java.util.IdentityHashMap;
import org.deidentifier.arx.framework.data.GeneralizationHierarchy;

public class GeneralizationTree extends IdentityHashMap<Integer, GeneralizationNode> {
    
    private static final long serialVersionUID = 1L;
    
    GeneralizationHierarchy   hierarchy;
    GeneralizationNode        root;
    int[][]                   hierarchyArray;
    int[][]                   cardinalityCache;
	final int maxLevel;
    
    public GeneralizationTree(GeneralizationHierarchy hierarchy) {
        this.hierarchy = hierarchy;
        maxLevel = hierarchy.getHeight() - 1;
        root = new GeneralizationNode(maxLevel, hierarchy.getArray()[0][maxLevel], hierarchy.getDistinctValues(0), this);
        root.buildTree(hierarchy);
        hierarchyArray = hierarchy.getArray();
        cardinalityCache = getCardinalities();
    }
    
    public GeneralizationNode getLowestCommonAncestor(GeneralizationNode node, int i) {
        
        for (GeneralizationNode commonNode = node; commonNode != root; commonNode = commonNode.parent) {
            if (commonNode.values.contains(i)) {
                return commonNode;
            }
        }
        return root;
    }
    
    public int getGeneralizationLevel(int[] values, int lvl) {
        
        int val = hierarchyArray[values[0]][lvl];
        
        for (int i = 1; i < values.length && lvl != maxLevel; i++) {
            while (hierarchyArray[values[i]][lvl] != val) {
                val = hierarchyArray[values[i - 1]][++lvl];
            }
        }
        return lvl;
    }
    
    public int getTransformation(int value, int lvl) {
        return hierarchyArray[value][lvl];
    }
    
    public int getCardinality(int value, int lvl) {
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
    
    /**
     * Gets the lowest generalization for a given set of integers.
     *
     * @param dataColumn a set of integers
     * @return the lowest level of the hierarchy, where all given integers are part of the subtree
     */
    public GeneralizationNode getLowestCommonAncestor(int[] dataColumn) {
        
        GeneralizationNode commonNode = get(dataColumn[0]);
        
        for (final int i : dataColumn) {
            while (!commonNode.values.contains(i)) {
                commonNode = commonNode.parent;
            }
        }
        
        return commonNode;
    }
}
