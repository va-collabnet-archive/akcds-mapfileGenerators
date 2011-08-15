package com.apelon.splRxNormMap;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.Hashtable;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.apelon.dts.client.DTSException;
import com.apelon.dts.client.association.ConceptAssociation;
import com.apelon.dts.client.attribute.DTSProperty;
import com.apelon.dts.client.concept.ConceptAttributeSetDescriptor;
import com.apelon.dts.client.concept.DTSConcept;
import com.apelon.dts.client.concept.DTSConceptQuery;
import com.apelon.dts.client.concept.DTSSearchOptions;
import com.apelon.dts.client.concept.OntylogConcept;
import com.apelon.dts.client.namespace.Namespace;
import com.apelon.dtsUtil.DbConn;
import com.apelon.splRxNormMap.data.DataMaps;
import com.apelon.splRxNormMap.data.NdcAsKey;
import com.apelon.splRxNormMap.data.NdcAsKeyData;
import com.apelon.splRxNormMap.data.SplAsKey;
import com.apelon.splRxNormMap.data.SplAsKeyData;

/**
 * Connect to a DTS server hosting RXNorm, and generate mapping file that get us from:
 * 
 * SPL Set ID -> VUID
 * NDC -> VUID
 * 
 * The result file can be read by the ViewDataFile class.
 * 
 * Any concepts from RXNorm that don't provide one of the above mappings are ignored.
 * 
 * Initial notes from Carol:
 * 
 * rxnorm has spl set id as a property, it also has VUID. VUID is a property in NDFRT too so we can go from label to RxNorm to NDF-rt by
 * means of spl_set_id and VUID however we know there are gaps, so we need to see what they are when you search in RxNorm and there are more
 * than one hit with that spl set id, you will want the one with TTY = SCD which stands for "Semantic Clinical Drug". 
 * if we find that this doesn't work well...we can also try NDC, but since there are numerous ncds
 * on a single lable, the label may be easier...but again, we'll have to see what the coverage is 
 * like in RxNorm of SPL_SET_ID
 * 
 * so let's do that, based on the latest RxNorm (being loaded to the same DTS instance for you), we create a spl_set_id to VUID mapping that
 * is used during the load process to create the triple format in workbench
 * 
 * 
 * In cases where you find an SPL_SET_ID in RxNorm but that concept has a TTY = SBD (branded drugs) and there is no VUID, you will have to
 * see if it has a "tradename_of" association. This will lead to a TTY= SCD (clinical drugs)which will likely have a VUID.
 * 
 * For example, from the list we have Bosetan with SPL_SET_ID = 749e42fb-2fe0-45dd-9268-b43bb3f4081c.
 * 
 * The RxNorm Branded Drugs "bosentan 62.5 MG Oral Tablet [349253]" and "bosentan 125 MG Oral Tablet [Tracleer] [656660]" are found with the
 * SPL_SET_ID property. Neither of these have a VUID. Following the inverse association "tradename_of" leads to
 * "bosentan 62.5 MG Oral Tablet [349253]" and "bosentan 125 MG Oral Tablet [656659]" Looking at these two concepts we confirm they are
 * clinical drugs that give us the VUIDs = 4015827 and 4015828. These are found on the corresponding VA Products =
 * "BOSENTAN 62.5MG TAB [VA Product]" and "BOSENTAN 125MG TAB [VA Product]".
 * 
 * Looking at the label, confirms that these are the correct VA products.
 * 
 * In other words I think the search could go like this:
 * 
 * Find SPL_SET_ID in RxNorm
 *        For RxNorm concepts with VUID
 *                Find corresponding VUID in NDF-RT
 *                        Match is a VA Product -> Done
 *                        Match is not a VA Product -> Log to "Not VA Product"
 *        For RxNorm concepts without VUID
 *                Find value of "tradename_of" association
 *                        Get VUID  from target
 *                                Find corresponding VUID in NDF-RT
 *                                        Match is a VA Product -> Done
 *                                        Match is not a VA Product -> Log to "Not VA Product"
 *               No "tradename_of" association -> Log to  "No Mapping"
 *               
 * Note, as of 8/11/11, we tweaked some of the logic, and now utilize all of the vuids - direct and tradename of.  So we now always get the
 * tradename of VUIDs as well.  Also removed the SBD and SCD checks - some of these were re-written later in the process, 
 * when we get to NDF-RT.
 * @author Dan Armbrust
 */

public class GenerateMapFile
{
	//At somepoint, this should be mavenized.  Currently, it just lines up kinda-sorta with a maven layout.
	private File outputDirectory;
	
	private Hashtable<String, NdcAsKey> ndcAsKey_ = new Hashtable<String, NdcAsKey>();
	private Hashtable<String, SplAsKey> splAsKey_ = new Hashtable<String, SplAsKey>();
	
	private HashSet<String> lockedNDCs = new HashSet<String>();
	private HashSet<String> lockedSPLs = new HashSet<String>();
	
	private Hashtable<String, HashSet<String>> tradenameOfCache_ = new Hashtable<String, HashSet<String>>();
	
	private OntylogConcept[] codesToProcess_; 
	int processPos_ = 0;
	
	
	public static void main(String[] args) throws Exception
	{
		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.WARN);
		GenerateMapFile gmf = new GenerateMapFile();
		gmf.outputDirectory = new File("target/");
		gmf.execute();
	}

	public void execute() throws Exception
	{
		File params = new File(outputDirectory, "../config/dts_conn_params.txt");
		DbConn dbConn = new DbConn(params);
		
		Namespace ns = dbConn.getNameQuery().findNamespaceById(dbConn.getNamespace());
		System.out.println("*** Connected to: " + dbConn.toString() + " " + ns.toString() + " ***");
		
		String pattern = "*";   
		DTSSearchOptions options = new DTSSearchOptions();
		//options.setLimit(500);
		options.setNamespaceId(dbConn.getNamespace());
		System.out.println("Searching for RXNorm Concepts");
		
		codesToProcess_ = dbConn.getSearchQuery().findConceptsWithNameMatching(pattern, options);

		System.out.println("Found " + codesToProcess_.length + " RXNorm Concept Codes");
		
		dbConn.close();
		
		Processor[] p = new Processor[3]; //Seem to be limited to 3 at the moment... 
		
		for (int i = 0; i < p.length; i++)
		{
			p[i] = new Processor(new DbConn(params));
			Thread t = new Thread(p[i]);
			t.setName("" + i);
			t.start();
		}
		
		while (true)
		{
			boolean allFinished = true;
			for (int i = 0; i < p.length; i++)
			{
				if (!p[i].isFinished())
				{
					allFinished = false;
					break;
				}
			}
			if (allFinished)
			{
				break;
			}
			else
			{
				Thread.sleep(10000);
			}
		}
		
		System.out.println();
		System.out.println("Processing Complete - Storing results");
		
		ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(new File(outputDirectory, "splRxNormMapData"))));
		DataMaps dm = new DataMaps(dbConn.toString() + " " + ns.toString(), ndcAsKey_, splAsKey_);
		oos.writeObject(dm);
		
		oos.flush();
		oos.close();
		
		System.out.println("Map creation complete");
	}
	
	private class Processor implements Runnable
	{
		private boolean isFinished_ = false;
		private DTSConceptQuery conceptQuery_;
		private DbConn dbConn_;
		
		public Processor(DbConn dbConn)
		{
			dbConn_ = dbConn;
			conceptQuery_ = dbConn.getConceptQueryInstance();
		}
		
		public boolean isFinished()
		{
			return isFinished_;
		}
		
		@Override
		public void run()
		{
			try
			{
				int pos = -1;
				//grab a process position
				synchronized (codesToProcess_)
				{
					pos = processPos_++;
				}
				
				while  (pos < codesToProcess_.length)
				{
					DTSConcept dtsConcept = conceptQuery_.findConceptByCode(codesToProcess_[pos].getCode(), dbConn_.getNamespace(),
							ConceptAttributeSetDescriptor.ALL_ATTRIBUTES);
					
					codesToProcess_[pos] = null;
					
					HashSet<String> vuids = new HashSet<String>();
					HashSet<String> ndcs = new HashSet<String>();
					HashSet<String> splSetIds = new HashSet<String>();
					HashSet<String> ttys = new HashSet<String>();
					HashSet<String> tradenameOfVuids = new HashSet<String>();
					String code = dtsConcept.getCode();
					
					for (DTSProperty dp : dtsConcept.getFetchedProperties())
					{
						if (dp.getName().equals("VUID"))
						{
							vuids.add(dp.getValue());
						}
						else if (dp.getName().equals("NDC"))
						{
							ndcs.add(dp.getValue().toUpperCase());
						}
						else if (dp.getName().equals("SPL_SET_ID"))
						{
							splSetIds.add(dp.getValue().toUpperCase());
						}
						else if (dp.getName().equals("TTY"))
						{
							ttys.add(dp.getValue());
						}
					}
					
					tradenameOfVuids = followTradeNameOf(conceptQuery_, dbConn_, dtsConcept);
					
					
					//Store the gathered info into each of the data stores.
					//First using the ndc as a key.
					for (String ndcValue : ndcs)
					{	
						synchronized (lockedNDCs)
						{
							while (lockedNDCs.contains(ndcValue))
							{
								//wait for another thread to finish mucking with this NDC
								lockedNDCs.wait();
							}
							if (!lockedNDCs.add(ndcValue))
							{
								System.err.println("Design flaw!");
							}
						}
						
						//Our thread has now locked this ndc value.
						NdcAsKey ndcAsKey =  ndcAsKey_.get(ndcValue);
						
						NdcAsKeyData codeData = new NdcAsKeyData(code, ttys, vuids, splSetIds);
						
						if (ndcAsKey == null)
						{
							ndcAsKey = new NdcAsKey(ndcValue, codeData);
							ndcAsKey_.put(ndcValue, ndcAsKey);
						}
						else
						{
							ndcAsKey.getCodes().add(codeData);
						}
						
						//Release our lock
						synchronized (lockedNDCs)
						{
							lockedNDCs.remove(ndcValue);
							lockedNDCs.notifyAll();
						}
					}
					
					//Then using SPL as key
					for (String splValue : splSetIds)
					{	
						synchronized (lockedSPLs)
						{
							while (lockedSPLs.contains(splValue))
							{
								//wait for another thread to finish mucking with this NDC
								lockedSPLs.wait();
							}
							if (!lockedSPLs.add(splValue))
							{
								System.err.println("Design flaw!");
							}
						}
						
						//Our thread has now locked this spl value.
						SplAsKey splAsKey =  splAsKey_.get(splValue);
						
						SplAsKeyData codeData = new SplAsKeyData(code, ttys, vuids, ndcs, tradenameOfVuids);
						
						if (splAsKey == null)
						{
							splAsKey = new SplAsKey(splValue, codeData);
							splAsKey_.put(splValue, splAsKey);
						}
						else
						{
							splAsKey.getCodes().add(codeData);
						}
						
						//Release our lock
						synchronized (lockedSPLs)
						{
							lockedSPLs.remove(splValue);
							lockedSPLs.notifyAll();
						}
					}
					
					//Move to the next process position
					synchronized (codesToProcess_)
					{
						pos = processPos_++;
						if (pos % 50 == 0)
						{
							System.out.println();
						}
						else
						{
							System.out.print(".");
						}
						if (pos % 1000 == 0)
						{
							System.out.println(pos);
						}
					}
				}
			}
			catch (Exception e)
			{
				System.err.println("Thread processing Error in thread " + Thread.currentThread().getName());
				e.printStackTrace();
				System.exit(-1);
			}
			isFinished_ = true;
		}
	}
	
	private HashSet<String> followTradeNameOf(DTSConceptQuery conceptQuery, DbConn dbConn, DTSConcept dtsConcept) throws DTSException
	{
		HashSet<String> tradeNameVuids = new HashSet<String>();
		
		ConceptAssociation[] assns = dtsConcept.getFetchedInverseConceptAssociations();
		HashSet<String> codes = new HashSet<String>();
		for (ConceptAssociation assn : assns)
		{
			if (assn.getAssociationType().getInverseName().equalsIgnoreCase("tradename_of"))
			{
				codes.add(assn.getFromConcept().getCode());
			}
		}
		
		for (String code : codes)
		{
			//get the TTY and VUIDs
			HashSet<String> vuids = tradenameOfCache_.get(code); 
				
			if (vuids == null)
			{
				vuids = new HashSet<String>();
			
				DTSConcept tradeNameConcept = conceptQuery.findConceptByCode(code, dbConn.getNamespace(), ConceptAttributeSetDescriptor.ALL_ATTRIBUTES);
				
				for (DTSProperty dp : tradeNameConcept.getFetchedProperties())
				{
					if (dp.getName().equals("VUID"))
					{
						vuids.add(dp.getValue());
					}
				}
				tradenameOfCache_.put(code, vuids);
			}
			tradeNameVuids.addAll(vuids);
		}
		
		return tradeNameVuids;
	}
}
