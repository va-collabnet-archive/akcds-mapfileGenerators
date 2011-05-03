package com.apelon.splRxNormMap.data;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * Class that can read in the serialized data maps.
 * 
 * @author Dan Armbrust
 */

public class ViewDataFile
{
	public static DataMaps readData(File f) throws FileNotFoundException, IOException, ClassNotFoundException
	{
		return (DataMaps)new ObjectInputStream(new BufferedInputStream(new FileInputStream(f))).readObject();
	}
	
	public static void main(String[] args) throws FileNotFoundException, IOException, ClassNotFoundException
	{
		DataMaps dm = readData(new File("target/mapData"));
		
		System.out.println(dm.getSourceDescription());
		System.out.println("NDC as key: " + dm.getNdcAsKey().size());
		System.out.println("SPL as key: " + dm.getSplAsKey().size());
	}
}
