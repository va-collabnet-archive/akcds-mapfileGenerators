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

import com.apelon.dts.client.attribute.DTSProperty;
import com.apelon.dts.client.concept.ConceptAttributeSetDescriptor;
import com.apelon.dts.client.concept.DTSConcept;
import com.apelon.dts.client.concept.DTSConceptQuery;
import com.apelon.dts.client.concept.DTSSearchOptions;
import com.apelon.dts.client.concept.OntylogConcept;
import com.apelon.dts.client.namespace.Namespace;
import com.apelon.splRxNormMap.data.DataMaps;
import com.apelon.splRxNormMap.data.NdcAsKey;
import com.apelon.splRxNormMap.data.NdcAsKeyData;
import com.apelon.splRxNormMap.data.SplAsKeyData;
import com.apelon.splRxNormMap.data.SplAsKey;

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
		DbConn dbConn = new DbConn();
		dbConn.connectDTS(new File(outputDirectory, "../config/dts_conn_params.txt"));
		
		Namespace ns = dbConn.nameQuery.findNamespaceById(dbConn.getNamespace());
		System.out.println("*** Connected to: " + dbConn.toString() + " " + ns.toString() + " ***");
		
		String pattern = "*";   
		DTSSearchOptions options = new DTSSearchOptions();
		options.setLimit(500);
		options.setNamespaceId(dbConn.getNamespace());
		System.out.println("Searching for RXNorm Concepts");
		
		codesToProcess_ = dbConn.searchQuery.findConceptsWithNameMatching(pattern, options);

		System.out.println("Found " + codesToProcess_.length + " RXNorm Concept Codes");
		
		Processor[] p = new Processor[20];
		
		for (int i = 0; i < p.length; i++)
		{
			p[i] = new Processor(dbConn);
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
		
		ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(new File(outputDirectory, "mapData"))));
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
					String code = dtsConcept.getCode();
					
					for (DTSProperty dp : dtsConcept.getFetchedProperties())
					{
						if (dp.getName().equals("VUID"))
						{
							vuids.add(dp.getValue());
						}
						else if (dp.getName().equals("NDC"))
						{
							ndcs.add(dp.getValue());
						}
						else if (dp.getName().equals("SPL_SET_ID"))
						{
							splSetIds.add(dp.getValue());
						}
						else if (dp.getName().equals("TTY"))
						{
							ttys.add(dp.getValue());
						}
					}
					
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
						
						//Our thread has now locked this ndc value.
						SplAsKey splAsKey =  splAsKey_.get(splValue);
						
						SplAsKeyData codeData = new SplAsKeyData(code, ttys, vuids, ndcs);
						
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
				System.err.println("Thread processing Error!");
				e.printStackTrace();
			}
			isFinished_ = true;
		}
	}
}
