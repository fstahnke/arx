package org.deidentifier.arx.recursive;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.deidentifier.arx.ARXAnonymizer;
import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.ARXResult;
import org.deidentifier.arx.Data;
import org.deidentifier.arx.AttributeType.Hierarchy;
import org.deidentifier.arx.Data.DefaultData;
import org.deidentifier.arx.DataHandle;
import org.deidentifier.arx.utility.DataConverter;

public class RecursiveAlgorithm {

    public void execute(final Data data, final ARXConfiguration config, final ARXAnonymizer anonymizer) throws IOException
    {
        // Execute the first anonymization
        ARXResult result = anonymizer.anonymize(data, config);
        // Get handle for input data and result
        DataHandle inHandle = data.getHandle();
        DataHandle outHandle = result.getOutput(false);
        
        // Convert input and output to array of string arrays
        String[][] input = new DataConverter().toArray(inHandle);
        String[][] output = new DataConverter().toArray(outHandle);
        
        // Create new data object for next anonymization step
        DefaultData outliers = Data.create();
        outliers.getDefinition().read(data.getDefinition());
        outliers.add(inHandle.iterator().next()); // add header to outlier object
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
        System.out.println(rows.size());
        if (rows.size() >= 2)
        // Anonymize the outliers and get the handle for the result
        result = anonymizer.anonymize(outliers, config);
        outHandle = result.getOutput(false);
        
        rowIter = outHandle.iterator();
        for (ListIterator<Integer> intIter = rows.listIterator(); intIter.hasNext();) {
            output[intIter.next()] = rowIter.next();
        }
        
        for (int i = 0; i < output.length; i++) {
            //System.out.println(Arrays.toString(output[i]));
        }
        
    }
}
