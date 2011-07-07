package com.apelon.splRxNormMap.data;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Hashtable;

/**
 * Just a parent class for the serialization of the results.
 * 
 * @author Dan Armbrust
 */

public class DataMaps implements Externalizable
{
	private static final long serialVersionUID = 1L;
	
	private String sourceDescription_;
	private Hashtable<String, NdcAsKey> ndcAsKey_;
	private Hashtable<String, SplAsKey> splAsKey_;
	private long buildDate = System.currentTimeMillis();

	public DataMaps()
	{

	}

	public DataMaps(String sourceDescription, Hashtable<String, NdcAsKey> ndcAsKey, Hashtable<String, SplAsKey> splAsKey)
	{
		sourceDescription_ = sourceDescription;
		ndcAsKey_ = ndcAsKey;
		splAsKey_ = splAsKey;
	}
	
	public String getSourceDescription()
	{
		return sourceDescription_;
	}

	public Hashtable<String, NdcAsKey> getNdcAsKey()
	{
		return ndcAsKey_;
	}

	public Hashtable<String, SplAsKey> getSplAsKey()
	{
		return splAsKey_;
	}
	
	public long getBuildDate()
	{
		return buildDate;
	}


	@Override
	public void writeExternal(ObjectOutput out) throws IOException
	{
		out.writeShort(2);
		out.writeObject(sourceDescription_);
		out.writeObject(ndcAsKey_);
		out.writeObject(splAsKey_);
		out.writeLong(buildDate);

	}

	@SuppressWarnings("unchecked")
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
	{
		short ver = in.readShort();
		if (ver < 1 || ver > 2)
		{
			throw new IOException("Unknown version " + ver);
		}
		
		if (ver >= 1)
		{
			sourceDescription_ = (String)in.readObject();
			ndcAsKey_ = (Hashtable<String, NdcAsKey>) in.readObject();
			splAsKey_ = (Hashtable<String, SplAsKey>) in.readObject();
		}
		if (ver == 2)
		{
			buildDate = in.readLong();
		}
	}
}