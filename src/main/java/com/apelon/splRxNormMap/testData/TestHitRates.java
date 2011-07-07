//package com.apelon.splRxNormMap.testData;
//
//import gov.va.akcds.spl.NDA;
//import gov.va.akcds.spl.Spl;
//import gov.va.akcds.util.wbDraftFacts.DraftFact;
//import gov.va.akcds.util.wbDraftFacts.DraftFacts;
//import gov.va.akcds.util.zipUtil.ZipContentsIterator;
//import gov.va.akcds.util.zipUtil.ZipFileContent;
//
//import java.io.BufferedInputStream;
//import java.io.BufferedReader;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.FileReader;
//import java.io.ObjectInputStream;
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.HashSet;
//import java.util.Hashtable;
//import java.util.Set;
//
//import com.apelon.splRxNormMap.data.DataMaps;
//
//public class TestHitRates
//{
//
//	/**
//	 * @param args
//	 * @throws Exception 
//	 */
//	public static void main(String[] args) throws Exception
//	{
//		File draftFactsFile = new File("../../akcds/splData/data/splDraftFacts.txt.zip");
//		File draftFactsFolder = new File("../../akcds/splData/target/draftFactsByID");
//		DraftFacts draftFacts = new DraftFacts(new File[] {draftFactsFile}, draftFactsFolder);
//		
//		DataMaps dm = (DataMaps)new ObjectInputStream(new BufferedInputStream(new FileInputStream(new File("data/mapData")))).readObject();
//		
//		//We only have 8 or 9 digits of the drug code.  RXNorm has more.  Create new maps from rxNorm that have 8 and 9 digits, resectively.
//		HashSet<String> rxNormDrugCodeEightMatch = new HashSet<String>();
//		HashSet<String> rxNormDrugCodeNineMatch = new HashSet<String>();
//		
//		for (String s : dm.getNdcAsKey().keySet())
//		{
//			if (s.length() >= 8)
//			{
//				rxNormDrugCodeEightMatch.add(s.substring(0, 8));
//			}
//			if (s.length() >= 8)
//			{
//				rxNormDrugCodeNineMatch.add(s.substring(0, 9));
//			}
//		}
//		
//		
//		HashSet<String> dropForOtherReason_ = new HashSet<String>();
//		
//		BufferedReader bf = new BufferedReader(new FileReader(new File("data/droppedSetIDs.txt")));
//		String s;
//		while ((s = bf.readLine()) != null)
//		{
//			s = s.trim();
//			if (s.length() == 0 || s.startsWith("#"))
//			{
//				continue;
//			}
//			dropForOtherReason_.add(s);
//		}
//		
//		System.out.println("Loaded " + dropForOtherReason_.size() + " items from the aux drop file");
//		
//		Hashtable<String, SimpleDraftFact> draftFactInfo = new Hashtable<String, SimpleDraftFact>();
//		
//		int splCount = 0;
//		int dropListCount = 0;
//		
//		for (String f : draftFactsFolder.list())
//		{
//			f = f.substring(0, f.length() - 4);
//			splCount++;
//			
//			if (dropForOtherReason_.contains(f))
//			{
//				dropListCount++;
//			}
//			else
//			{
//				ArrayList<DraftFact> df =  draftFacts.getFacts(f);
//				
//				String drugCode = "";
//				
//				if (df.size() > 0)
//				{
//					drugCode = df.get(0).getDrugCode();
//				}
//				draftFactInfo.put(f, new SimpleDraftFact(f, drugCode));
//			}
//		}
//		
//		
//		//check for match by setId
//		int splHitCount = 0;
//		int drugCodeHitCount = 0;
//		int noMatchCount = 0;
//
//		for (SimpleDraftFact sdf : draftFactInfo.values())
//		{
//			boolean foundMatch = false;
//			
//			if (dm.getSplAsKey().containsKey(sdf.splSetId_))
//			{
//				splHitCount++;
//				foundMatch = true;
//			}
//			
//			if (rxNormDrugCodeEightMatch.contains(sdf.drugCode_) || rxNormDrugCodeNineMatch.contains(sdf.drugCode_))
//			{
//				drugCodeHitCount++;
//				foundMatch = true;
//			}
//			
//			if (!foundMatch)
//			{
//				noMatchCount++;
//			}
//			
//			sdf.foundMatch_ = foundMatch;
//		}
//		System.out.println("SPLs: " + splCount + " Drop for drop list: " + dropListCount + " SPL Hit Count: " + splHitCount 
//				+ " NDC Hit Count: " + drugCodeHitCount + " No match: " + noMatchCount);
//		
//		HashSet<NDA> NDAsWithMatches = new HashSet<NDA>();
//		
//		System.out.println("Checking for any other NDA match...");
//
//		Hashtable<String, HashSet<NDA>> splToNDA = loadNDAs(draftFactInfo.keySet());
//		
//		int noMatchSPL = 0;
//		
//		for (SimpleDraftFact sdf : draftFactInfo.values())
//		{
//			if (sdf.foundMatch_)
//			{
//				HashSet<NDA> nda = splToNDA.get(sdf.splSetId_);
//				if (nda != null)
//				{
//					NDAsWithMatches.addAll(nda);
//				}
//				else
//				{
//					System.err.println(sdf.splSetId_);
//					noMatchSPL++;
//				}
//			}
//		}
//		
//		noMatchCount = 0;
//		int ndfOverlapMatch = 0;
//		
//		for (SimpleDraftFact sdf : draftFactInfo.values())
//		{
//			if (!sdf.foundMatch_)
//			{
//				noMatchCount++;
//				
//				HashSet<NDA> ndas = splToNDA.get(sdf.splSetId_);
//				if (ndas != null)
//				{
//					for (NDA nda : ndas)
//					{
//						if (NDAsWithMatches.contains(nda))
//						{
//							sdf.foundMatch_ = true;
//							ndfOverlapMatch++;
//							noMatchCount--;
//							break;
//						}	
//					}
//				}
//				else
//				{
//					System.err.println(sdf.splSetId_);
//					noMatchSPL++;
//				}
//			}
//		}
//		
//		System.out.println("SPL no match? " + noMatchSPL);
//		
//		System.out.println("SPLs: " + splCount + " Drop for drop list: " + dropListCount + " SPL Hit Count: " + splHitCount 
//				+ " NDC Hit Count: " + drugCodeHitCount + " NDC Overlap match: " + ndfOverlapMatch + " No match: " + noMatchCount);
//		
//	}
//	
//	
//	private static Hashtable<String, HashSet<NDA>> loadNDAs(Set<String> onlyInclude) throws Exception
//	{
//		Hashtable<String, HashSet<NDA>> result = new Hashtable<String, HashSet<NDA>>();
//		
//		File dataFile = new File("/media/truecrypt2/Source Data/SPL 2010_11_02", "dm_spl_release_20101102.zip");
//
//		System.out.println(new Date().toString());
//		System.out.println("Reading spl zip file");
//		// process the zip of zips
//		ZipContentsIterator outerZCI = new ZipContentsIterator(dataFile);
//
//		while (outerZCI.hasMoreElements())
//		{
//			// Each of these should be a zip file
//			ZipFileContent nestedZipFile = outerZCI.nextElement();
//
//			if (nestedZipFile.getName().toLowerCase().endsWith(".zip"))
//			{
//				// open up the nested zip file
//				ZipContentsIterator nestedZipFileContents = new ZipContentsIterator(nestedZipFile.getFileBytes());
//
//				ArrayList<ZipFileContent> filesInNestedZipFile = new ArrayList<ZipFileContent>();
//
//				while (nestedZipFileContents.hasMoreElements())
//				{
//					filesInNestedZipFile.add(nestedZipFileContents.nextElement());
//				}
//
//				if (filesInNestedZipFile.size() > 0)
//				{
//					// Pass the elements in to the spl factory
//					Spl spl = new Spl(filesInNestedZipFile, nestedZipFile.getName());
//					if (onlyInclude.contains(spl.getSetId()))
//					{
//						result.put(spl.getSetId(), spl.getUniqueNDAs());
//					}
//				}
//				else
//				{
//					System.err.println("Empty inner zip file? " + nestedZipFile.getName());
//				}
//			}
//			else
//			{
//				System.err.println("Skipping unexpected file in outer zip file: " + nestedZipFile.getName());
//			}
//		}
//		
//		return result;
//	}
//}
