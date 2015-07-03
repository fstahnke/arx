package org.deidentifier.arx.recursive;

import java.io.IOException;

import org.deidentifier.arx.ARXAnonymizer;
import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.Data;
import org.deidentifier.arx.AttributeType.Hierarchy;
import org.deidentifier.arx.criteria.KAnonymity;

public class RecursiveTest {
    
    public static void main(String[] args) throws IOException {
        
        RecursiveAlgorithm recursiveInstance = new RecursiveAlgorithm();
        
      //final Data data2 = Data.create("data/adult_subset.csv", ';');
        final Data data = Data.create("data/adult.csv", ';');
        data.getDefinition().setAttributeType("age", Hierarchy.create("data/adult_hierarchy_age.csv", ';'));
        data.getDefinition().setAttributeType("education", Hierarchy.create("data/adult_hierarchy_education.csv", ';'));
        data.getDefinition().setAttributeType("marital-status", Hierarchy.create("data/adult_hierarchy_marital-status.csv", ';'));
        data.getDefinition().setAttributeType("native-country", Hierarchy.create("data/adult_hierarchy_native-country.csv", ';'));
        data.getDefinition().setAttributeType("occupation", Hierarchy.create("data/adult_hierarchy_occupation.csv", ';'));
        data.getDefinition().setAttributeType("race", Hierarchy.create("data/adult_hierarchy_race.csv", ';'));
        data.getDefinition().setAttributeType("salary-class", Hierarchy.create("data/adult_hierarchy_salary-class.csv", ';'));
        data.getDefinition().setAttributeType("sex", Hierarchy.create("data/adult_hierarchy_sex.csv", ';'));
        data.getDefinition().setAttributeType("workclass", Hierarchy.create("data/adult_hierarchy_workclass.csv", ';'));
        
        
        
        ARXAnonymizer anonymizer = new ARXAnonymizer();
        
        ARXConfiguration config = ARXConfiguration.create();
        

        config.addCriterion(new KAnonymity(2));
        config.setMaxOutliers(0.3d);
        
        recursiveInstance.execute(data, config, anonymizer);
        
    }
    
}
