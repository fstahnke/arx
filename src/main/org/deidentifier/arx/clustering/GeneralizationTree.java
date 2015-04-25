package org.deidentifier.arx.clustering;

import java.util.HashMap;

import org.deidentifier.arx.framework.data.GeneralizationHierarchy;

// TODO: Try using IdentityHashMap, but be aware of the cache limit of Integer from -128 to +127
public class GeneralizationTree extends HashMap<Integer, GeneralizationNode> {
    
    private static final long serialVersionUID = 1L;
    
    GeneralizationHierarchy   hierarchy;
    GeneralizationNode        root;
    int[][]                   hierarchyArray;
    HashMap<Integer, Integer> cardinalityCache;
    
    public GeneralizationTree(GeneralizationHierarchy hierarchy) {
        this.hierarchy = hierarchy;
        final int maxLevel = hierarchy.getHeight() - 1;
        root = new GeneralizationNode(maxLevel, hierarchy.getArray()[0][maxLevel], hierarchy.getDistinctValues(0), this);
        root.buildTree(hierarchy);
        hierarchyArray = hierarchy.getArray();
        cardinalityCache = getCardinalities();
    }
    
    public GeneralizationNode getLowestCommonAncestor(GeneralizationNode node1, GeneralizationNode node2) {
        
        return null;
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
        final int maxLvl = hierarchyArray[0].length - 1;
        
        for (int i = 1; i < values.length && lvl != maxLvl; i++) {
            while (hierarchyArray[values[i]][lvl] != val) {
                val = hierarchyArray[values[i - 1]][++lvl];
            }
        }
        return lvl;
    }
    
    public int getTransformation(int value, int lvl) {
        return hierarchyArray[value][lvl];
    }
    
    public HashMap<Integer, Integer> getCardinalities() {
        
        final HashMap<Integer, Integer> cardinalities = new HashMap<>(hierarchyArray.length + hierarchyArray[0].length);
        
        for (int[] record : hierarchyArray) {
            cardinalities.put(record[0], 1);
        }
        
        for (int i = 0; i < hierarchyArray.length; i++) {
            for (int j = 1; j < hierarchyArray[0].length; j++) {
                Integer value = cardinalities.get(hierarchyArray[i][j]);
                if (value != null) {
                    cardinalities.put(hierarchyArray[i][j], ++value);
                }
                else {
                    cardinalities.put(hierarchyArray[i][j], 1);
                }
            }
        }
        
        return cardinalities;
    }
    
    public int getCardinality (int value, int lvl) {
        return cardinalityCache.get(hierarchyArray[value][lvl]);
    }
    
    /*public int getCardinality(int value, int lvl) {
        
        int distinctNumber = 0;
        final int transformation = hierarchyArray[value][lvl];
        
        for (final int[] record : hierarchyArray) {
            if (record[lvl] == transformation) {
                distinctNumber++;
            }
        }
        return distinctNumber;
    }*/
    
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
