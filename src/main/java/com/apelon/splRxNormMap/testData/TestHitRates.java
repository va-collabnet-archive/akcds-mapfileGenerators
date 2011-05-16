package com.apelon.splRxNormMap.testData;

import gov.va.akcds.util.wbDraftFacts.DraftFact;
import gov.va.akcds.util.wbDraftFacts.DraftFacts;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashSet;

import com.apelon.splRxNormMap.data.DataMaps;

public class TestHitRates
{

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception
	{
		File draftFactsFile = new File("../../akcds/splData/splDraftFacts.txt.zip");
		File draftFactsFolder = new File("../../akcds/splData/target/draftFactsByID");
		DraftFacts draftFacts = new DraftFacts(draftFactsFile, draftFactsFolder);
		
		DataMaps dm = (DataMaps)new ObjectInputStream(new BufferedInputStream(new FileInputStream(new File("data/mapData")))).readObject();
		
		HashSet<String> eightMatch = new HashSet<String>();
		HashSet<String> nineMatch = new HashSet<String>();
		
		for (String s : dm.getNdcAsKey().keySet())
		{
			if (s.length() >= 8)
			{
				eightMatch.add(s.substring(0, 8));
			}
			if (s.length() >= 8)
			{
				nineMatch.add(s.substring(0, 9));
			}
		}
		
		
		//check for match by setId
		int splCount = 0;
		int splHitCount = 0;
		int drugCodeHitCount = 0;
		int noMatch = 0;
		
		
		for (String f : draftFactsFolder.list())
		{
			boolean foundMatch = false;
			f = f.substring(0, f.length() - 4);
			splCount++;
			
			if (dm.getSplAsKey().containsKey(f))
			{
				splHitCount++;
				foundMatch = true;
			}
			
			ArrayList<DraftFact> df =  draftFacts.getFacts(f);
			
			
			if (df.size() > 0)
			{
				String drugCode = df.get(0).getDrugCode();
				
				drugCode = drugCode.replaceAll("-", "");
				
				if (eightMatch.contains(drugCode) || nineMatch.contains(drugCode))
				{
					drugCodeHitCount++;
					foundMatch = true;
				}
			}
			
			if (!foundMatch)
			{
				noMatch++;
			}
		}
		System.out.println("SPLs: " + splCount + " SPL Hit Count: " + splHitCount + " ndc hit count: " + drugCodeHitCount + " No match: " + noMatch);
		
	}

}
