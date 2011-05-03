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

public class SplAsKey implements Externalizable
{
	private static final long serialVersionUID = 1L;
	private String spl_;
	private ArrayList<SplAsKeyData> codeData_ = new ArrayList<SplAsKeyData>();

	public SplAsKey()
	{

	}

	public SplAsKey(String spl, SplAsKeyData codeData)
	{
		spl_ = spl;
		codeData_.add(codeData);
	}

	public String getSpl()
	{
		return spl_;
	}

	public ArrayList<SplAsKeyData> getCodes()
	{
		return codeData_;
	}

	public String toString()
	{
		return "SPL Set ID: " + spl_ + " " + Arrays.toString(codeData_.toArray());
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException
	{
		out.writeShort(1);
		out.writeObject(spl_);
		out.writeObject(codeData_);

	}

	@SuppressWarnings("unchecked")
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
	{
		short ver = in.readShort();
		if (ver == 1)
		{
			spl_ = (String) in.readObject();
			codeData_ = (ArrayList<SplAsKeyData>) in.readObject();

		}
		else
		{
			throw new IOException("Unknown version " + ver);
		}
	}
}