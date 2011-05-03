package com.apelon.splRxNormMap.data;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.HashSet;

/**
 * Data mapping structure
 * 
 * @author Dan Armbrust
 */

public class NdcAsKeyData implements Externalizable
{
	private static final long serialVersionUID = 1L;
	private String code_;
	private HashSet<String> tty_;
	private HashSet<String> vuid_;
	private HashSet<String> splSetId_;

	public NdcAsKeyData()
	{

	}

	public NdcAsKeyData(String code, HashSet<String> tty, HashSet<String> vuids, HashSet<String> splSetIds)
	{
		this.code_ = code;
		this.tty_ = tty;
		vuid_ = vuids;
		splSetId_ = splSetIds;
	}

	public String getCode()
	{
		return code_;
	}

	public HashSet<String> getTty()
	{
		return tty_;
	}

	public HashSet<String> getVuid()
	{
		return vuid_;
	}

	public HashSet<String> splSetId()
	{
		return splSetId_;
	}

	public String toString()
	{
		return "Code: " + code_ + " TTYs: " + Arrays.toString(tty_.toArray()) + " VUIDs: " + Arrays.toString(vuid_.toArray()) + " splSetIds "
				+ Arrays.toString(splSetId_.toArray());
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException
	{
		out.writeShort(1);
		out.writeObject(code_);
		out.writeObject(tty_);
		out.writeObject(vuid_);
		out.writeObject(splSetId_);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
	{
		short ver = in.readShort();
		if (ver == 1)
		{
			code_ = (String) in.readObject();
			tty_ = (HashSet<String>) in.readObject();
			vuid_ = (HashSet<String>) in.readObject();
			splSetId_ = (HashSet<String>) in.readObject();
		}
		else
		{
			throw new IOException("Unknown version " + ver);
		}
	}

}
