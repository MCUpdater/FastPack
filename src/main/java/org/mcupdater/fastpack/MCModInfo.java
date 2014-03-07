package org.mcupdater.fastpack;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class MCModInfo
{
	@SerializedName("modid")
	public String modId;
	public String name;
	public String description = "";
	public String url = "";
	public String updateUrl = "";
	public String version = "";
	public List<String> authors = new ArrayList<>();
	public List<String> authorList = new ArrayList<>();
	public String credits = "";
	public String parent = "";
}
