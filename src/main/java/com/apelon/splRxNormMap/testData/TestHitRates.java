package com.apelon.splRxNormMap.testData;

import gov.va.akcds.util.wbDraftFacts.DraftFact;
import gov.va.akcds.util.wbDraftFacts.DraftFacts;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
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
		File draftFactsFile = new File("../../akcds/splData/data/splDraftFacts.txt.zip");
		File draftFactsFolder = new File("../../akcds/splData/target/draftFactsByID");
		DraftFacts draftFacts = new DraftFacts(draftFactsFile, draftFactsFolder);
		
		DataMaps dm = (DataMaps)new ObjectInputStream(new BufferedInputStream(new FileInputStream(new File("data/mapData")))).readObject();
		
		//We only have 8 or 9 digits of the drug code.  RXNorm has more.  Create new maps from rxNorm that have 8 and 9 digits, resectively.
		HashSet<String> rxNormDrugCodeEightMatch = new HashSet<String>();
		HashSet<String> rxNormDrugCodeNineMatch = new HashSet<String>();
		
		for (String s : dm.getNdcAsKey().keySet())
		{
			if (s.length() >= 8)
			{
				rxNormDrugCodeEightMatch.add(s.substring(0, 8));
			}
			if (s.length() >= 8)
			{
				rxNormDrugCodeNineMatch.add(s.substring(0, 9));
			}
		}
		
		
		HashSet<String> dropForOtherReason_ = new HashSet<String>();
		
		BufferedReader bf = new BufferedReader(new FileReader(new File("data/droppedSetIDs.txt")));
		String s;
		while ((s = bf.readLine()) != null)
		{
			s = s.trim();
			if (s.length() == 0 || s.startsWith("#"))
			{
				continue;
			}
			dropForOtherReason_.add(s);
		}
		
		System.out.println("Loaded " + dropForOtherReason_.size() + " items from the aux drop file");
		
		
		//check for match by setId
		int splCount = 0;
		int splHitCount = 0;
		int drugCodeHitCount = 0;
		int noMatchCount = 0;
		int dropListCount = 0;
		
		
		for (String f : draftFactsFolder.list())
		{
			boolean foundMatch = false;
			f = f.substring(0, f.length() - 4);
			splCount++;
			
			if (dropForOtherReason_.contains(f))
			{
				dropListCount++;
				continue;
			}
			
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
				if (drugCode.length() < 8 || drugCode.length() > 9)
				{
					System.err.println("Oops - wrong length: " + drugCode);
				}
				
				if (rxNormDrugCodeEightMatch.contains(drugCode) || rxNormDrugCodeNineMatch.contains(drugCode))
				{
					drugCodeHitCount++;
					foundMatch = true;
				}
			}
			
			if (!foundMatch)
			{
				noMatchCount++;
			}
		}
		System.out.println("SPLs: " + splCount + " Drop for drop list: " + dropListCount + " SPL Hit Count: " + splHitCount 
				+ " NDC Hit Count: " + drugCodeHitCount + " No match: " + noMatchCount);
	}
}
