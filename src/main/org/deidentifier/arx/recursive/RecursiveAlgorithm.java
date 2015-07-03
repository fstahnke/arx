package org.deidentifier.arx.recursive;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.deidentifier.arx.ARXAnonymizer;
import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.ARXResult;
import org.deidentifier.arx.Data;
import org.deidentifier.arx.Data.DefaultData;
import org.deidentifier.arx.DataHandle;
import org.deidentifier.arx.utility.AggregateFunction;
import org.deidentifier.arx.utility.DataConverter;
import org.deidentifier.arx.utility.UtilityMeasureAECS;
import org.deidentifier.arx.utility.UtilityMeasureDiscernibility;
import org.deidentifier.arx.utility.UtilityMeasureLoss;
import org.deidentifier.arx.utility.UtilityMeasureNonUniformEntropy;
import org.deidentifier.arx.utility.UtilityMeasurePrecision;

public class RecursiveAlgorithm {

    public void execute(final Data data, final ARXConfiguration config, final ARXAnonymizer anonymizer) throws IOException
    {
        // Execute the first anonymization
        ARXResult result = anonymizer.anonymize(data, config);
        // Get handle for input data and result
        DataHandle inHandle = data.getHandle();
        DataHandle outHandle = result.getOutput(false);

        DataConverter converter = new DataConverter();
        // Convert input and output to array of string arrays
        String[][] input = converter.toArray(inHandle);
        String[][] output = converter.toArray(outHandle);
        Map<String, String[][]> hierarchies = converter.toMap(data.getDefinition());
        String[] header = converter.getHeader(inHandle);
        
        // Create new data object for next anonymization step
        DefaultData outliers = Data.create();
        outliers.getDefinition().read(data.getDefinition());
        outliers.add(inHandle.iterator().next()); // add header to outlier object
        int numOutliers = 0; // number of outliers
        // Declare list of rows that are currently suppressed
        // Be careful to always consider the header and skip it if necessary
        List<Integer> rows = new ArrayList<Integer>();
        // Create iterator for string arrays to iterate rows
        Iterator<String[]> rowIter = inHandle.iterator();
        rowIter.next(); // Skip header
        
        /* Iterate all rows of the input
         * if a row is suppressed (an outlier), add it to the new data object
         * and add the number of the row to the list
         */
        for (int i = 0; i < outHandle.getNumRows(); i++) {
            String[] currentRow = rowIter.next();
            if (outHandle.isOutlier(i)) {
                outliers.add(currentRow);
                //System.out.println(Arrays.toString(currentRow));
                rows.add(i);
            }
        }

        double outputAECS = new UtilityMeasureAECS().evaluate(output).getUtility();
        double outputDiscernibility = new UtilityMeasureDiscernibility().evaluate(output).getUtility();
        double outputLoss = new UtilityMeasureLoss<Double>(header, hierarchies, AggregateFunction.GEOMETRIC_MEAN).evaluate(output).getUtility();
        double outputEntropy = new UtilityMeasureNonUniformEntropy<Double>(header, input).evaluate(output).getUtility();
        double outputPrecision = new UtilityMeasurePrecision<Double>(header, hierarchies).evaluate(output).getUtility();
        numOutliers = rows.size();
        
        System.out.println("Suppressed entries: " + numOutliers + ", Utility: " + outputLoss);
        if (rows.size() >= 2)
        // Anonymize the outliers and get the handle for the result
        result = anonymizer.anonymize(outliers, config);
        outHandle = result.getOutput(false);
        
        rowIter = outHandle.iterator();
        rowIter.next(); // skip header
        for (ListIterator<Integer> intIter = rows.listIterator(); intIter.hasNext();) {
            int i = intIter.next();
            if (!inHandle.isOutlier(i)) {
                output[i] = rowIter.next();
                numOutliers--;
            }
        }
        
        outputLoss = new UtilityMeasureLoss<Double>(header, hierarchies, AggregateFunction.GEOMETRIC_MEAN).evaluate(output).getUtility();
        System.out.println("Suppressed entries: " + numOutliers + ", Utility: " + outputLoss);
        
    }
}
