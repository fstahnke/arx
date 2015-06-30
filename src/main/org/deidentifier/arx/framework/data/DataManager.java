/*
 * ARX: Powerful Data Anonymization
 * Copyright 2012 - 2015 Florian Kohlmayer, Fabian Prasser
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.deidentifier.arx.framework.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.deidentifier.arx.DataDefinition;
import org.deidentifier.arx.RowSet;
import org.deidentifier.arx.criteria.DPresence;
import org.deidentifier.arx.criteria.HierarchicalDistanceTCloseness;
import org.deidentifier.arx.criteria.PrivacyCriterion;
import org.deidentifier.arx.framework.check.distribution.DistributionAggregateFunction;
import org.deidentifier.arx.framework.check.distribution.DistributionAggregateFunction.DistributionAggregateFunctionGeneralization;

import com.carrotsearch.hppc.IntObjectOpenHashMap;
import com.carrotsearch.hppc.IntOpenHashSet;

/**
 * Holds all data needed for the anonymization process.
 * 
 * @author Fabian Prasser
 * @author Florian Kohlmayer
 */
public class DataManager {

    /**
     * Internal representation of attribute types. Quasi-identifiers are split
     * into the ones to which generalization is applied and the ones to which
     * microaggregation is applied
     * 
     * @author Florian Kohlmayer
     * @author Fabian Prasser
     * 
     */
    public static class AttributeTypeInternal {
        public static final int IDENTIFYING                       = 3;
        public static final int INSENSITIVE                       = 2;
        public static final int QUASI_IDENTIFYING_GENERALIZED     = 0;
        public static final int QUASI_IDENTIFYING_MICROAGGREGATED = 4;
        public static final int SENSITIVE                         = 1;
    }

    /** The data. */
    private final Data                                 dataAnalyzed;

    /** The data which is generalized */
    private final Data                                 dataGeneralized;

    /** The data which is insensitive */
    private final Data                                 dataStatic;

    /** The original input header. */
    private final String[]                             header;

    /** The generalization hierarchiesQI. */
    private final GeneralizationHierarchy[]            hierarchiesGeneralized;

    /** The hierarchy heights for each QI. */
    private final int[]                                hierarchiesHeights;

    /** The sensitive attributes. */
    private final Map<String, GeneralizationHierarchy> hierarchiesSensitive;

    /** The indexes of sensitive attributes. */
    private final Map<String, Integer>                 indexesSensitive;

    /** The maximum level for each QI. */
    private final int[]                                maxLevels;

    /** The microaggregation functions. */
    private final DistributionAggregateFunction[]      microaggregationFunctions;

    /** Header for microaggregated attributes */
    private final String[]                             microaggregationHeader;

    /** Map for microaggregated attributes */
    private final int[]                                microaggregationMap;

    /** The number of microaggregation attributes in the dataDI */
    private final int                                  microaggregationNumAttributes;

    /** The start index of the microaggregation attributes in the dataDI */
    private final int                                  microaggregationStartIndex;

    /** The minimum level for each QI. */
    private final int[]                                minLevels;

    /** The research subset, if any. */
    private RowSet                                     subset     = null;

    /** The size of the research subset. */
    private int                                        subsetSize = 0;

    /**
     * Creates a new data manager from pre-encoded data.
     * 
     * @param header
     * @param data
     * @param dictionary
     * @param definition
     * @param criteria
     * @param function
     */
    public DataManager(final String[] header,
                       final int[][] data,
                       final Dictionary dictionary,
                       final DataDefinition definition,
                       final Set<PrivacyCriterion> criteria,
                       final Map<String, DistributionAggregateFunction> functions) {

        // Store research subset
        for (PrivacyCriterion c : criteria) {
            if (c instanceof DPresence) {
                subset = ((DPresence) c).getSubset().getSet();
                subsetSize = ((DPresence) c).getSubset().getArray().length;
                break;
            }
        }

        // Store columns for reordering the output
        this.header = header;

        Set<String> attributesGemeralized = definition.getQuasiIdentifiersWithGeneralization();
        Set<String> attributesSensitive = definition.getSensitiveAttributes();
        Set<String> attributesMicroaggregated = definition.getQuasiIdentifiersWithMicroaggregation();
        Set<String> attributesInsensitive = definition.getInsensitiveAttributes();

        // Init dictionary
        final Dictionary dictionaryGeneralized = new Dictionary(attributesGemeralized.size());
        final Dictionary dictionaryAnalyzed = new Dictionary(attributesSensitive.size() +
                                                             attributesMicroaggregated.size());
        final Dictionary dictionaryStatic = new Dictionary(attributesInsensitive.size());

        // Init maps for reordering the output
        final int[] mapGeneralized = new int[dictionaryGeneralized.getNumDimensions()];
        final int[] mapAnalyzed = new int[dictionaryAnalyzed.getNumDimensions()];
        final int[] mapStatic = new int[dictionaryStatic.getNumDimensions()];
        this.microaggregationMap = new int[attributesMicroaggregated.size()];

        // Indexes
        this.microaggregationStartIndex = attributesSensitive.size();
        this.microaggregationNumAttributes = attributesMicroaggregated.size();
        int indexStatic = 0;
        int indexGeneralized = 0;
        int indexAnalyzed = 0;
        int indexSensitive = 0;
        int indexMicroaggregated = this.microaggregationStartIndex;
        int counter = 0;

        // A map for column indices. map[i*2]=attribute type, map[i*2+1]=index
        // position. */
        final int[] map = new int[header.length * 2];
        final String[] headerGH = new String[dictionaryGeneralized.getNumDimensions()];
        final String[] headerDI = new String[dictionaryAnalyzed.getNumDimensions()];
        final String[] headerIS = new String[dictionaryStatic.getNumDimensions()];
        microaggregationHeader = new String[attributesMicroaggregated.size()];

        for (final String column : header) {
            final int idx = counter * 2;
            if (attributesGemeralized.contains(column)) {
                map[idx] = AttributeTypeInternal.QUASI_IDENTIFYING_GENERALIZED;
                map[idx + 1] = indexGeneralized;
                mapGeneralized[indexGeneralized] = counter;
                dictionaryGeneralized.registerAll(indexGeneralized, dictionary, counter);
                headerGH[indexGeneralized] = header[counter];
                indexGeneralized++;
            } else if (attributesMicroaggregated.contains(column)) {
                map[idx] = AttributeTypeInternal.QUASI_IDENTIFYING_MICROAGGREGATED;
                map[idx + 1] = indexMicroaggregated;
                mapAnalyzed[indexMicroaggregated] = counter;
                dictionaryAnalyzed.registerAll(indexMicroaggregated, dictionary, counter);
                headerDI[indexMicroaggregated] = header[counter];
                indexMicroaggregated++;
                microaggregationMap[indexAnalyzed] = counter;
                microaggregationHeader[indexAnalyzed] = header[counter];
                indexAnalyzed++;
            } else if (attributesInsensitive.contains(column)) {
                map[idx] = AttributeTypeInternal.INSENSITIVE;
                map[idx + 1] = indexStatic;
                mapStatic[indexStatic] = counter;
                dictionaryStatic.registerAll(indexStatic, dictionary, counter);
                headerIS[indexStatic] = header[counter];
                indexStatic++;
            } else if (attributesSensitive.contains(column)) {
                map[idx] = AttributeTypeInternal.SENSITIVE;
                map[idx + 1] = indexSensitive;
                mapAnalyzed[indexSensitive] = counter;
                dictionaryAnalyzed.registerAll(indexSensitive, dictionary, counter);
                headerDI[indexSensitive] = header[counter];
                indexSensitive++;
            } else {
                // TODO: CHECK: Changed default? - now all undefined attributes
                // are identifying! Previously they were sensitive?
                map[idx] = AttributeTypeInternal.IDENTIFYING;
                map[idx + 1] = -1;
            }
            counter++;
        }

        // encode Data
        final Data[] ddata = encode(data,
                                    map,
                                    mapGeneralized,
                                    mapAnalyzed,
                                    mapStatic,
                                    dictionaryGeneralized,
                                    dictionaryAnalyzed,
                                    dictionaryStatic,
                                    headerGH,
                                    headerDI,
                                    headerIS);
        dataGeneralized = ddata[0];
        dataAnalyzed = ddata[1];
        dataStatic = ddata[2];

        // Initialize minlevels
        minLevels = new int[attributesGemeralized.size()];
        hierarchiesHeights = new int[attributesGemeralized.size()];
        maxLevels = new int[attributesGemeralized.size()];

        // Build hierarchiesQI
        hierarchiesGeneralized = new GeneralizationHierarchy[attributesGemeralized.size()];
        for (int i = 0; i < header.length; i++) {
            final int idx = i * 2;
            if (attributesGemeralized.contains(header[i]) &&
                map[idx] == AttributeTypeInternal.QUASI_IDENTIFYING_GENERALIZED) {
                final int dictionaryIndex = map[idx + 1];
                final String name = header[i];
                if (definition.getHierarchy(name) != null) {
                    hierarchiesGeneralized[dictionaryIndex] = new GeneralizationHierarchy(name,
                                                                                          definition.getHierarchy(name),
                                                                                          dictionaryIndex,
                                                                                          dictionaryGeneralized);
                } else {
                    throw new IllegalStateException("No hierarchy available for attribute (" +
                                                    header[i] + ")");
                }
                // Initialize hierarchy height and minimum / maximum
                // generalization
                hierarchiesHeights[dictionaryIndex] = hierarchiesGeneralized[dictionaryIndex].getArray()[0].length;
                final Integer minGenLevel = definition.getMinimumGeneralization(name);
                minLevels[dictionaryIndex] = minGenLevel == null ? 0 : minGenLevel;
                final Integer maxGenLevel = definition.getMaximumGeneralization(name);
                maxLevels[dictionaryIndex] = maxGenLevel == null ? hierarchiesHeights[dictionaryIndex] - 1
                        : maxGenLevel;
            }
        }

        // Build map with hierarchies for sensitive attributes
        Map<String, String[][]> sensitiveHierarchies = new HashMap<String, String[][]>();
        for (PrivacyCriterion c : criteria) {
            if (c instanceof HierarchicalDistanceTCloseness) {
                HierarchicalDistanceTCloseness t = (HierarchicalDistanceTCloseness) c;
                sensitiveHierarchies.put(t.getAttribute(), t.getHierarchy().getHierarchy());
            }
        }

        // Build generalization hierarchies for sensitive attributes
        hierarchiesSensitive = new HashMap<String, GeneralizationHierarchy>();
        indexesSensitive = new HashMap<String, Integer>();
        int index = 0;
        for (int i = 0; i < header.length; i++) {
            final String name = header[i];
            final int idx = i * 2;
            if (sensitiveHierarchies.containsKey(name) &&
                map[idx] == AttributeTypeInternal.SENSITIVE) {
                final int dictionaryIndex = map[idx + 1];
                final String[][] hiers = sensitiveHierarchies.get(name);
                if (hiers != null) {
                    hierarchiesSensitive.put(name, new GeneralizationHierarchy(name,
                                                                               hiers,
                                                                               dictionaryIndex,
                                                                               dictionaryAnalyzed));
                }
            }

            // Store index for sensitive attributes
            if (attributesSensitive.contains(header[i])) {
                indexesSensitive.put(name, index);
                index++;
            }
        }

        // Build map with hierarchies for microaggregated attributes
        Map<String, String[][]> maHierarchies = new HashMap<String, String[][]>();
        for (String attribute : functions.keySet()) {
            if (functions.get(attribute) instanceof DistributionAggregateFunctionGeneralization) {
                maHierarchies.put(attribute, definition.getHierarchy(attribute));
            }
        }

        // Build generalization hierarchies for microaggregated attributes
        Map<String, int[][]> hierarchiesMA = new HashMap<String, int[][]>();
        index = 0;
        for (int i = 0; i < header.length; i++) {
            final String name = header[i];
            final int idx = i * 2;
            if (maHierarchies.containsKey(name) &&
                map[idx] == AttributeTypeInternal.QUASI_IDENTIFYING_MICROAGGREGATED) {
                final int dictionaryIndex = map[idx + 1];
                final String[][] hiers = maHierarchies.get(name);
                if (hiers != null) {
                    hierarchiesMA.put(name, new GeneralizationHierarchy(name,
                                                                        hiers,
                                                                        dictionaryIndex,
                                                                        dictionaryAnalyzed).map);
                }
            }
        }

        // finalize dictionary
        dictionaryGeneralized.finalizeAll();
        dictionaryAnalyzed.finalizeAll();
        dictionaryStatic.finalizeAll();

        // Init microaggregation functions
        microaggregationFunctions = new DistributionAggregateFunction[attributesMicroaggregated.size()];
        for (int i = 0; i < header.length; i++) {
            final int idx = i * 2;
            if (attributesMicroaggregated.contains(header[i]) &&
                map[idx] == AttributeTypeInternal.QUASI_IDENTIFYING_MICROAGGREGATED) {
                final int dictionaryIndex = map[idx + 1] - microaggregationStartIndex;
                final String name = header[i];
                if (definition.getMicroAggregationFunction(name) != null) {
                    microaggregationFunctions[dictionaryIndex] = functions.get(name);
                    microaggregationFunctions[dictionaryIndex].initialize(dictionaryAnalyzed.getMapping()[dictionaryIndex +
                                                                                                          microaggregationStartIndex],
                                                                          definition.getDataType(name),
                                                                          hierarchiesMA.get(name));
                } else {
                    throw new IllegalStateException("No microaggregation function defined for attribute (" +
                                                    header[i] + ")");
                }
            }
        }
    }

    /**
     * Returns the input data that will be analyzed.
     * 
     * @return the data
     */
    public Data getDataAnalyzed() {
        return dataAnalyzed;
    }

    /**
     * Returns the input data that will be generalized.
     * 
     * @return the data
     */
    public Data getDataGeneralized() {
        return dataGeneralized;
    }

    /**
     * Returns the static input data.
     * 
     * @return the data
     */
    public Data getDataStatic() {
        return dataStatic;
    }

    /**
     * Returns the distribution of the given sensitive attribute in the original
     * dataset. Required for t-closeness.
     * 
     * @param attribute
     * @return distribution
     */
    public double[] getDistribution(String attribute) {

        // TODO: Distribution size equals the size of the complete dataset
        // TODO: Good idea?
        final int index = indexesSensitive.get(attribute);
        final int distinct = dataAnalyzed.getDictionary().getMapping()[index].length;
        final int[][] data = dataAnalyzed.getArray();

        // Initialize counts: iterate over all rows or the subset
        final int[] cardinalities = new int[distinct];
        for (int i = 0; i < data.length; i++) {
            if (subset == null || subset.contains(i)) {
                cardinalities[data[i][index]]++;
            }
        }

        // compute distribution
        final double total = subset == null ? data.length : subsetSize;
        final double[] distribution = new double[cardinalities.length];
        for (int i = 0; i < distribution.length; i++) {
            distribution[i] = (double) cardinalities[i] / total;
        }
        return distribution;
    }

    /**
     * The original data header.
     * 
     * @return
     */
    public String[] getHeader() {
        return header;
    }

    /**
     * Returns the heights of the hierarchiesQI.
     * 
     * @return
     */

    public int[] getHierachiesHeights() {
        return hierarchiesHeights;
    }

    /**
     * Returns the generalization hierarchiesQI.
     * 
     * @return the hierarchiesQI
     */
    public GeneralizationHierarchy[] getHierarchies() {
        return hierarchiesGeneralized;
    }

    /**
     * Returns the maximum levels for the generalizaiton.
     * 
     * @return the maximum level for each QI
     */
    public int[] getHierarchiesMaxLevels() {
        return maxLevels;
    }

    /**
     * Returns the minimum levels for the generalizations.
     * 
     * @return
     */

    public int[] getHierarchiesMinLevels() {
        return minLevels;
    }

    /**
     * Returns the microaggregation functions.
     * 
     * @return
     */
    public DistributionAggregateFunction[] getMicroaggregationFunctions() {
        return microaggregationFunctions;
    }

    /**
     * Returns the header for the according buffer
     * @return
     */
    public String[] getMicroaggregationHeader() {
        return microaggregationHeader;
    }

    /**
     * Returns the map for the according buffer
     * @return
     */
    public int[] getMicroaggregationMap() {
        return microaggregationMap;
    }

    /**
     * Gets the number of attributes to which microaggregation will be applied
     * in dataAnalyzed.
     * 
     * @return
     */
    public int getMicroaggregationNumAttributes() {
        return microaggregationNumAttributes;
    }

    /**
     * Gets the start index of the attributes to which microaggregation will be
     * applied in dataAnalyzed.
     * 
     * @return
     */
    public int getMicroaggregationStartIndex() {
        return microaggregationStartIndex;
    }

    /**
     * Returns the tree for the given sensitive attribute, if a generalization
     * hierarchy is associated. Required for t-closeness with hierarchical
     * distance EMD
     * 
     * @param attribute
     * @return tree
     */
    public int[] getTree(String attribute) {

        final int[][] data = dataAnalyzed.getArray();
        final int index = indexesSensitive.get(attribute);
        final int[][] hierarchy = hierarchiesSensitive.get(attribute).map;
        final int totalElementsP = subset == null ? data.length : subsetSize;
        final int height = hierarchy[0].length - 1;
        final int numLeafs = hierarchy.length;

        // TODO: Size could be calculated?!
        final ArrayList<Integer> treeList = new ArrayList<Integer>();
        treeList.add(totalElementsP);
        treeList.add(numLeafs);
        treeList.add(height);

        // Init all freq to 0
        for (int i = 0; i < numLeafs; i++) {
            treeList.add(0);
        }

        // Count frequencies
        final int offsetLeafs = 3;
        for (int i = 0; i < data.length; i++) {
            if (subset == null || subset.contains(i)) {
                int previousFreq = treeList.get(data[i][index] + offsetLeafs);
                previousFreq++;
                treeList.set(data[i][index] + offsetLeafs, previousFreq);
            }
        }

        // Init extras
        for (int i = 0; i < numLeafs; i++) {
            treeList.add(-1);
        }

        // Temporary class for nodes
        class TNode {
            IntOpenHashSet children = new IntOpenHashSet();
            int            level    = 0;
            int            offset   = 0;
        }

        final int offsetsExtras = offsetLeafs + numLeafs;
        final IntObjectOpenHashMap<TNode> nodes = new IntObjectOpenHashMap<TNode>();
        final ArrayList<ArrayList<TNode>> levels = new ArrayList<ArrayList<TNode>>();

        // Init levels
        for (int i = 0; i < hierarchy[0].length; i++) {
            levels.add(new ArrayList<TNode>());
        }

        // Build nodes
        for (int i = 0; i < hierarchy[0].length; i++) {
            for (int j = 0; j < hierarchy.length; j++) {
                final int nodeID = hierarchy[j][i];
                TNode curNode = null;

                if (!nodes.containsKey(nodeID)) {
                    curNode = new TNode();
                    curNode.level = i;
                    nodes.put(nodeID, curNode);
                    final ArrayList<TNode> level = levels.get(curNode.level);
                    level.add(curNode);
                } else {
                    curNode = nodes.get(nodeID);
                }

                if (i > 0) { // first add child
                    curNode.children.add(hierarchy[j][i - 1]);
                }
            }
        }

        // For all nodes
        for (final ArrayList<TNode> level : levels) {
            for (final TNode node : level) {

                if (node.level > 0) { // only inner nodes
                    node.offset = treeList.size();

                    treeList.add(node.children.size());
                    treeList.add(node.level);

                    final int[] keys = node.children.keys;
                    final boolean[] allocated = node.children.allocated;
                    for (int i = 0; i < allocated.length; i++) {
                        if (allocated[i]) {
                            treeList.add(node.level == 1 ? keys[i] + offsetsExtras
                                    : nodes.get(keys[i]).offset);
                        }
                    }

                    treeList.add(0); // pos_e
                    treeList.add(0); // neg_e
                }
            }
        }

        final int[] treeArray = new int[treeList.size()];
        int count = 0;
        for (final int val : treeList) {
            treeArray[count++] = val;
        }

        return treeArray;
    }

    /**
     * Encodes the data.
     * 
     * @param data
     * @param map
     * @param mapGeneralized
     * @param mapAnalyzed
     * @param mapStatic
     * @param dictionaryGeneralized
     * @param dictionaryAnalyzed
     * @param dictionaryStatic
     * @param headerGeneralized
     * @param headerAnalyzed
     * @param headerStatic
     * @return
     */
    private Data[] encode(final int[][] data,
                          final int[] map,
                          final int[] mapGeneralized,
                          final int[] mapAnalyzed,
                          final int[] mapStatic,
                          final Dictionary dictionaryGeneralized,
                          final Dictionary dictionaryAnalyzed,
                          final Dictionary dictionaryStatic,
                          final String[] headerGeneralized,
                          final String[] headerAnalyzed,
                          final String[] headerStatic) {

        // Parse the dataset
        final int[][] valsGH = new int[data.length][];
        final int[][] valsDI = new int[data.length][];
        final int[][] valsIS = new int[data.length][];

        int index = 0;
        for (final int[] tuple : data) {

            // Process a tuple
            final int[] tupleGH = new int[headerGeneralized.length];
            final int[] tupleDI = new int[headerAnalyzed.length];
            final int[] tupleIS = new int[headerStatic.length];

            for (int i = 0; i < tuple.length; i++) {
                final int idx = i * 2;
                int aType = map[idx];
                final int iPos = map[idx + 1];
                switch (aType) {
                case AttributeTypeInternal.QUASI_IDENTIFYING_GENERALIZED:
                    tupleGH[iPos] = tuple[i];
                    break;
                case AttributeTypeInternal.IDENTIFYING:
                    // Ignore
                    break;
                case AttributeTypeInternal.INSENSITIVE:
                    tupleIS[iPos] = tuple[i];
                    break;
                case AttributeTypeInternal.QUASI_IDENTIFYING_MICROAGGREGATED:
                    tupleDI[iPos] = tuple[i];
                    break;
                case AttributeTypeInternal.SENSITIVE:
                    tupleDI[iPos] = tuple[i];
                    break;
                }
            }
            valsGH[index] = tupleGH;
            valsIS[index] = tupleIS;
            valsDI[index] = tupleDI;
            index++;
        }

        // Build data object
        final Data[] result = { new Data(valsGH,
                                         headerGeneralized,
                                         mapGeneralized,
                                         dictionaryGeneralized),
                new Data(valsDI, headerAnalyzed, mapAnalyzed, dictionaryAnalyzed),
                new Data(valsIS, headerStatic, mapStatic, dictionaryStatic) };
        return result;
    }
}
