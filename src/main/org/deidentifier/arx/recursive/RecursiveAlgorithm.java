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

        // Convert input and output to array of string arrays
        DataConverter converter = new DataConverter();
        String[][] output = converter.toArray(outHandle);
        Map<String, String[][]> hierarchies = converter.toMap(data.getDefinition());
        String[] header = converter.getHeader(inHandle);
        
        int numOutliers = -1;
        
        while (numOutliers > 1 || numOutliers == -1) {
        //for (int i = 0; i < 3; i++) {
            
            // Create new data object for next anonymization step
            DefaultData outliers = Data.create();
            outliers.getDefinition().read(data.getDefinition());
            outliers.add(header); // add header to outlier object
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
            for (int j = 0; j < outHandle.getNumRows(); j++) {
                if (outHandle.isOutlier(j)) {
                    outliers.add(rowIter.next());
                    rows.add(j);
                }
            }
            
            // Calculate and print the current loss of the output and the number of suppressed entries
            double outputLoss = new UtilityMeasureLoss<Double>(header, hierarchies, AggregateFunction.GEOMETRIC_MEAN).evaluate(output).getUtility();
            numOutliers = rows.size();
            System.out.println("Suppressed entries: " + numOutliers + ", Information Loss: " + outputLoss);
            
            if (numOutliers > 1) {
             // Anonymize the outliers and get the handle for the result
                result = anonymizer.anonymize(outliers, config);
                inHandle = outliers.getHandle();
                outHandle = result.getOutput(false);
                
                // Iterate over result and write all non-outliers to the output
                rowIter = outHandle.iterator();
                rowIter.next(); // skip header
                ListIterator<Integer> intIter = rows.listIterator();
                for (int j = 0; j < outHandle.getNumRows(); j++) {
                    int k = intIter.next();
                    if (!outHandle.isOutlier(j)) {
                        output[k] = rowIter.next();
                    }
                }
            }
        }
    }
}
