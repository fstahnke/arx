/**
 * This class is a draft implementation of the
 * Goldberger & Tassa Algorithm for gereralization
 * by clustering. State February 2014.
 */
package org.deidentifier.arx.clustering;

import java.io.IOException;
import java.util.List;

import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.Data;
import org.deidentifier.arx.AttributeType.Hierarchy;
import org.deidentifier.arx.DataHandleInput;
import org.deidentifier.arx.framework.data.DataManager;
import org.deidentifier.arx.framework.data.Dictionary;

/**
 * @author Fabian Stahnke
 *
 */


public class GTAlgorithm {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public void anonymize() throws IOException {
		
		Data data = importData("data/test.csv", ';');
		DataManager manager = prepareDataManager(data);
		
		org.deidentifier.arx.framework.data.Data dataTable = manager.dataQI; //TODO: Why do I have two "Data

	}
	
	private Data importData(String path, char separator) throws IOException {
		
		final Data data = Data.create(path, separator);
		
		data.getDefinition()
        .setAttributeType("age",
                          Hierarchy.create("data/test_hierarchy_age.csv",
                                           ';'));
		data.getDefinition()
        .setAttributeType("gender",
                          Hierarchy.create("data/test_hierarchy_gender.csv",
                                           ';'));
		data.getDefinition()
        .setAttributeType("zipcode",
                          Hierarchy.create("data/test_hierarchy_zipcode.csv",
                                           ';'));
		
		return data;
		
	}
	
	private DataManager prepareDataManager(Data data) {
		
		final DataHandleInput handleInput = (DataHandleInput)data.getHandle();
		final ARXConfiguration config = ARXConfiguration.create();
		
		final String[] header = handleInput.getHeader();
		final int[][] dataArray = handleInput.data;
		final Dictionary dictionary = handleInput.dictionary;
		return new DataManager(header, dataArray, dictionary, data.getDefinition(), config.getCriteria());
		
		
	}
	

}
