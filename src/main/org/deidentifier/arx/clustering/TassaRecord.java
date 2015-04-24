/**
 * 
 */
package org.deidentifier.arx.clustering;

import java.util.Arrays;

/**
 * @author Fabian Stahnke
 *
 */
public class TassaRecord {

	public final int[] recordContent;
	public TassaCluster assignedCluster;
	private final int hashCode;
	
	public TassaRecord(int[] content)
	{
		recordContent = content;
		hashCode = Arrays.hashCode(recordContent);
	}
	
	public int getSize() { return this.recordContent.length; }
	
	public int hashCode() {
		return hashCode;
	}
	
	public boolean equals(Object o) {
		if (o instanceof TassaRecord) {
			return (Arrays.equals(recordContent, ((TassaRecord)o).recordContent));
		}
		return false;
	}
	
	

}
