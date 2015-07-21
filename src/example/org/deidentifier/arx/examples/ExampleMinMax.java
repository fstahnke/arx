package org.deidentifier.arx.examples;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import org.deidentifier.arx.AttributeType;
import org.deidentifier.arx.Data;
import org.deidentifier.arx.DataDefinition;
import org.deidentifier.arx.DataHandle;
import org.deidentifier.arx.AttributeType.Hierarchy;
import org.deidentifier.arx.utility.AggregateFunction;
import org.deidentifier.arx.utility.DataConverter;
import org.deidentifier.arx.utility.UtilityMeasureAECS;
import org.deidentifier.arx.utility.UtilityMeasureDiscernibility;
import org.deidentifier.arx.utility.UtilityMeasureLoss;
import org.deidentifier.arx.utility.UtilityMeasureNonUniformEntropy;
import org.deidentifier.arx.utility.UtilityMeasurePrecision;

public class ExampleMinMax {
    private static final String basePath= "../arx-data/data-heurakles/";
    private static final int qiNum = 4;
    
    private enum Dataset {
        Adult (new String[] { "age",
                              "marital-status",
                              "race",
                              "sex",
                              "education",
                              "native-country",
                              "salary-class",
                              "workclass",
                              "occupation" }),
        Atus (new String[] {  "Age",
                              "Race",
                              "Region",
                              "Sex",
                              "Birthplace",
                              "Citizenship status",
                              "Labor force status",
                              "Marital status",
                              "Highest level of school completed"}),
        Cup  (new String[] {  "AGE",
                              "GENDER",
                              "STATE",
                              "ZIP",
                              "INCOME",
                              "MINRAMNT",
                              "NGIFTALL",
                              "RAMNTALL"}),
        Fars  (new String[] { "iage",
                              "ihispanic",
                              "irace",
                              "isex",
                              "ideathday",
                              "ideathmon",
                              "iinjury",
                              "istatenum"}),
        Ihis   (new String[] { "AGE",
                               "RACEA",
                               "REGION",
                               "SEX",
                               "MARSTAT",
                               "PERNUM",
                               "QUARTER",
                               "YEAR",
                               "EDUC"}),
                              ;
        
        private final String[] attributes;        
        private final DataHandle inputHandle;
        private final DataConverter converter = new DataConverter();
        private final DataDefinition inputDataDef;

        private String[][] inputArray;
        private String[][] outputArray;
        
        Dataset (String[] attributes) {
            this.attributes = attributes;
            
            Data arxData = null;
            String path = basePath + "data/" + this.name() + ".csv";
            try {
                arxData = Data.create(path, ';');
            } catch (IOException e) {
                System.out.println("unable to open dataset" + path);
            }
            if (arxData != null) {
                this.inputHandle = arxData.getHandle();
                this.inputDataDef = inputHandle.getDefinition();
            }
            else {
                this.inputHandle = null;
                this.inputDataDef = null;
            }
            
            Hierarchy hierarchy = null;
            for (int i = 0; i < qiNum; i++) {
                String attr = attributes[i];
                path = basePath + "hierarchies/" + this.name() + "_hierarchy_" + attr + ".csv";
                    try {                        
                        hierarchy = Hierarchy.create(path, ';');
                    } catch (IOException e) {
                        System.out.println("unable to open hierarchy " + path);
                    }
                    inputDataDef.setAttributeType(attr, AttributeType.QUASI_IDENTIFYING_ATTRIBUTE);
                    inputDataDef.setHierarchy(attr, hierarchy);
            }
            
            inputArray = converter.toArray(inputHandle, inputDataDef);
            
            outputArray = new String[inputArray.length][qiNum];
            for (int i = 0; i < inputArray.length; i++) {
                for (int j = 0; j < inputArray[0].length; j++) {
                    outputArray[i][j] = "*";
                }
            }
        }
        
        public String[] getHeader() {
        	Set<String> qis = this.inputDataDef.getQuasiIdentifyingAttributes();
        	return qis.toArray(new String[qis.size()]);
        }
        
        public Map<String, String[][]> getHierarchies() {
            return converter.toMap(inputDataDef);
        }

        private String[][] getInputArray() {
            return this.inputArray;
        }
        
        private String[][] getOutputArray() {
            return this.outputArray;
        }
    }

    public static void main(String[] args) throws IOException {

        String inFormat =  "%13.2f";
        String outFormat = "%16.2f";
        
        for (Dataset dataset : new Dataset[] { Dataset.Adult, Dataset.Atus, Dataset.Cup, Dataset.Fars, Dataset.Ihis}) {

            String[][] input                    = dataset.getInputArray();
            String[][] output                   = dataset.getOutputArray();
            Map<String, String[][]> hierarchies = dataset.getHierarchies();
            String[] header                     = dataset.getHeader();

            // Compute for input
            double inputAECS = new UtilityMeasureAECS().evaluate(input).getUtility();
            double inputDiscernibility = new UtilityMeasureDiscernibility().evaluate(input).getUtility();
            double inputLoss = new UtilityMeasureLoss<Double>(header, hierarchies, AggregateFunction.GEOMETRIC_MEAN).evaluate(input).getUtility();
            double inputEntropy = new UtilityMeasureNonUniformEntropy<Double>(header, input).evaluate(input).getUtility();
            double inputPrecision = new UtilityMeasurePrecision<Double>(header, hierarchies).evaluate(input).getUtility();

            // Compute for output
            double outputAECS = new UtilityMeasureAECS().evaluate(output).getUtility();
            double outputDiscernibility = new UtilityMeasureDiscernibility().evaluate(output).getUtility();
            double outputLoss = new UtilityMeasureLoss<Double>(header, hierarchies, AggregateFunction.GEOMETRIC_MEAN).evaluate(output).getUtility();
            double outputEntropy = new UtilityMeasureNonUniformEntropy<Double>(header, input).evaluate(output).getUtility();
            double outputPrecision = new UtilityMeasurePrecision<Double>(header, hierarchies).evaluate(output).getUtility();

            System.out.println(dataset + " " + Arrays.toString(header));
            System.out.println("  AECS: min = " + String.format(inFormat, inputAECS)           + " / max = " + String.format(outFormat, outputAECS));
            System.out.println("  Disc: min = " + String.format(inFormat, inputDiscernibility) + " / max = " + String.format(outFormat, outputDiscernibility));
            System.out.println("  Loss: min = " + String.format(inFormat, inputLoss)           + " / max = " + String.format(outFormat, outputLoss));
            System.out.println("  Entr: min = " + String.format(inFormat, inputEntropy)        + " / max = " + String.format(outFormat, outputEntropy));
            System.out.println("  Prec: min = " + String.format(inFormat, inputPrecision)      + " / max = " + String.format(outFormat, outputPrecision));
            
            System.out.println();
        }
    }
}
