package com.apelon.splRxNormMap.data;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Data mapping structure
 * 
 * @author Dan Armbrust
 */

public class NdcAsKey implements Externalizable
{
	private static final long serialVersionUID = 1L;
	private	String ndc_;
	private ArrayList<NdcAsKeyData> codeData_ = new ArrayList<NdcAsKeyData>();

	
	public NdcAsKey()
	{
		
	}
	
	public NdcAsKey(String ndc, NdcAsKeyData codeData)
	{
		ndc_ = ndc;
		codeData_.add(codeData);
	}
	
	public String getNdc()
	{
		return ndc_;
	}

	public ArrayList<NdcAsKeyData> getCodes()
	{
		return codeData_;
	}
	
	public String toString()
	{
		return "NDC: " + ndc_ + " " + Arrays.toString(codeData_.toArray()); 
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException
	{
		out.writeShort(1);
		out.writeObject(ndc_);
		out.writeObject(codeData_);
		
	}

	@SuppressWarnings("unchecked")
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
	{
		short ver = in.readShort();
		if (ver == 1)
		{
			ndc_ = (String)in.readObject();
			codeData_ = (ArrayList<NdcAsKeyData>)in.readObject();
			
		}
		else
		{
			throw new IOException("Unknown version " + ver);
		}
	}
}