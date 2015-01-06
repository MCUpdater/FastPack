package org.mcupdater.fastpack;

import com.google.gson.annotations.SerializedName;

public class LitemodInfo {
	@SerializedName("name")
	public String name;
	public String mcversion;
	public String revision;
	public String author;
	public String version;
	public String description;
	public String url;
	public String tweakClass;
	public String classTransformerClass;
}
