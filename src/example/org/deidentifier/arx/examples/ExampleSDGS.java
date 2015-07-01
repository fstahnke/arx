package org.deidentifier.arx.example;

import java.util.Map;

import org.deidentifier.arx.ARXResult;
import org.deidentifier.arx.DataDefinition;
import org.deidentifier.arx.DataHandle;
import org.deidentifier.arx.utility.DataConverter;
import org.deidentifier.arx.utility.UtilityMeasureAECS;
import org.deidentifier.arx.utility.UtilityMeasureDiscernibility;
import org.deidentifier.arx.utility.UtilityMeasureLoss;
import org.deidentifier.arx.utility.UtilityMeasureNonUniformEntropy;
import org.deidentifier.arx.utility.UtilityMeasurePrecision;

public class ExampleSDGS {

    public static void main(String[] args) {
        
        // ARX Stuff
        DataDefinition definition = null;
        ARXResult result = null; // TODO
        DataHandle inputHandle = null; // TODO
        DataHandle outputHandle = result.getOutput();
        
        // Prepare
        DataConverter converter = new DataConverter();
        String[][] input = converter.toArray(inputHandle);
        String[][] output = converter.toArray(outputHandle, outputHandle.getView());
        Map<String, String[][]> hierarchies = converter.toMap(definition);
        String[] header = converter.getHeader(inputHandle);

        // Compute for output
        double outputAECS = new UtilityMeasureAECS().evaluate(output);
        double outputDiscernibility = new UtilityMeasureDiscernibility().evaluate(output);
        double outputLoss = new UtilityMeasureLoss(header, hierarchies).evaluate(output);
        double outputEntropy = new UtilityMeasureNonUniformEntropy(header, input).evaluate(output);
        double outputPrecision = new UtilityMeasurePrecision(header, hierarchies).evaluate(output);

        // Compute for input
        double inputAECS = new UtilityMeasureAECS().evaluate(input);
        double inputDiscernibility = new UtilityMeasureDiscernibility().evaluate(input);
        double inputLoss = new UtilityMeasureLoss(header, hierarchies).evaluate(input);
        double inputEntropy = new UtilityMeasureNonUniformEntropy(header, input).evaluate(input);
        double inputPrecision = new UtilityMeasurePrecision(header, hierarchies).evaluate(input);
    }
}
