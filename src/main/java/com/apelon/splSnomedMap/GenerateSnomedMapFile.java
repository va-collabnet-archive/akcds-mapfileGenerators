package com.apelon.splSnomedMap;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.TreeMap;

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
import com.apelon.dtsUtil.DbConn;

/**
 * Connect to a DTS server hosting snomed, and generate a mapping file that get us from:
 * 
 * code -> preferred term -> fully specified name.
 * 
 * 
 * @author Dan Armbrust
 */

public class GenerateSnomedMapFile
{
	// At somepoint, this should be mavenized. Currently, it just lines up kinda-sorta with a maven layout.
	private File outputDirectory;

	private TreeMap<String, OntylogConcept> codesToProcess_ = new TreeMap<String, OntylogConcept>();

	private ArrayList<String[]> results_ = new ArrayList<String[]>();

	public static void main(String[] args) throws Exception
	{
		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.WARN);
		GenerateSnomedMapFile gmf = new GenerateSnomedMapFile();
		gmf.outputDirectory = new File("target/");
		gmf.execute();
	}

	public void execute() throws Exception
	{
		File params = new File(outputDirectory, "../config/dts_conn_params_snomed.txt");
		DbConn dbConn = new DbConn(params);

		Namespace ns = dbConn.getNameQuery().findNamespaceById(dbConn.getNamespace());
		System.out.println("*** Connected to: " + dbConn.toString() + " " + ns.toString() + " ***");

		ArrayList<String> patterns = new ArrayList<String>();
		for (int i = 65; i <= 90; i++)
		{
			patterns.add(new String(Character.toChars(i)) + "*");
		}
		for (int i = 0; i <= 9; i++)
		{
			patterns.add(i + "*");
		}
		
		for (String pattern : patterns) 
		{
			DTSSearchOptions options = new DTSSearchOptions();
			//options.setLimit(1);
			options.setNamespaceId(dbConn.getNamespace());
			System.out.println("Searching for snomed Concepts starting for " + pattern);
	
			OntylogConcept[] result = dbConn.getSearchQuery().findConceptsWithNameMatching(pattern, options);
			for (OntylogConcept oc : result)
			{
				codesToProcess_.put(oc.getCode(), oc);
			}
			System.out.println("Found " + codesToProcess_.size() + " Snomed Concept Codes so far");
		}
		System.out.println("Found " + codesToProcess_.size() + " Snomed Concept Codes Total");

		dbConn.close();

		Processor[] p = new Processor[3]; // Seem to be limited to 3 at the moment...

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

		FileWriter fw = new FileWriter(new File(outputDirectory, "snomedCodeNameMap.txt"));

		fw.write("#\r\n");
		fw.write("#Snomed Code\tLegacy Code\tPreferredTerm\tFully Specified Name\r\n");
		fw.write("#\r\n");
		for (String[] strings : results_)
		{
			for (int i = 0; i < strings.length; i++)
			{
				fw.write(strings[i]);
				if (i < strings.length - 1)
				{
					fw.write("\t");
				}
			}

			fw.write("\r\n");
		}

		fw.close();

		System.out.println("Map creation complete - " + results_.size() + " items");
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
				while (codesToProcess_.size() > 0)
				{
					OntylogConcept oc = null;
					// grab a process position
					synchronized (codesToProcess_)
					{
						if (codesToProcess_.size() == 0)
						{
							continue;
						}
						oc = codesToProcess_.remove(codesToProcess_.firstKey());
					}
					
					DTSConcept dtsConcept = conceptQuery_.findConceptByCode(oc.getCode(), dbConn_.getNamespace(),
							ConceptAttributeSetDescriptor.ALL_ATTRIBUTES);

					String code = dtsConcept.getCode();
					String codeInSource = "";
					String preferredTerm = dtsConcept.getFetchedPreferredTerm().getValue();
					String fullySpecifiedName = dtsConcept.getName();
					
					for (DTSProperty p : dtsConcept.getFetchedProperties())
					{
						if (p.getName().equals("Code in Source"))
						{
							codeInSource = p.getValue();
							break;
						}
					}

					synchronized (results_)
					{
						results_.add(new String[] {codeInSource, code, preferredTerm, fullySpecifiedName });
					}

					// Move to the next process position
					if (results_.size() % 50 == 0)
					{
						System.out.println();
					}
					else
					{
						System.out.print(".");
					}
					if (results_.size() % 1000 == 0)
					{
						System.out.println(results_.size());
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
}
