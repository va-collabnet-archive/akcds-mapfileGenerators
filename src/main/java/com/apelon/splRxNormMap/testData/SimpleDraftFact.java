package com.apelon.splRxNormMap.testData;

public class SimpleDraftFact
{
	String drugCode_;
	String splSetId_;
	boolean foundMatch_;
	
	SimpleDraftFact(String splSetId, String drugCode)
	{
		drugCode_ = drugCode.replaceAll("-", "");
		if (drugCode_.length() > 0 && (drugCode_.length() < 8 || drugCode_.length() > 9))
		{
			System.err.println("Oops - wrong length: " + drugCode_);
		}
		this.splSetId_ = splSetId;
	}
}
