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

public class SplAsKeyData implements Externalizable
{
	private static final long serialVersionUID = 1L;
	private String code_;
	private HashSet<String> tty_;
	private HashSet<String> vuid_;
	private HashSet<String> ndc_;
	private HashSet<String> tradenameOfVuid_;

	public SplAsKeyData()
	{

	}

	public SplAsKeyData(String code, HashSet<String> tty, HashSet<String> vuids, HashSet<String> ndcs, HashSet<String> tradenameOfVuids)
	{
		this.code_ = code;
		this.tty_ = tty;
		vuid_ = vuids;
		ndc_ = ndcs;
		tradenameOfVuid_ = tradenameOfVuids;
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

	public HashSet<String> splNdc()
	{
		return ndc_;
	}
	
	public HashSet<String> getTradenameOfVuids()
	{
		return tradenameOfVuid_;
	}

	public String toString()
	{
		return "Code: " + code_ + " TTYs: " + Arrays.toString(tty_.toArray()) 
			+ " VUIDs: " + Arrays.toString(vuid_.toArray()) 
			+ " ndcs " + Arrays.toString(ndc_.toArray())
			+ " tradenameOfVuids: " + Arrays.toString(tradenameOfVuid_.toArray());
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException
	{
		out.writeShort(2);
		out.writeObject(code_);
		out.writeObject(tty_);
		out.writeObject(vuid_);
		out.writeObject(ndc_);
		out.writeObject(tradenameOfVuid_);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
	{
		short ver = in.readShort();
		if (ver >= 1 || ver <= 2)
		{
			code_ = (String) in.readObject();
			tty_ = (HashSet<String>) in.readObject();
			vuid_ = (HashSet<String>) in.readObject();
			ndc_ = (HashSet<String>) in.readObject();
			if (ver >= 2)
			{
				tradenameOfVuid_ = (HashSet<String>) in.readObject();
			}
		}
		else
		{
			throw new IOException("Unknown version " + ver);
		}
	}

}
