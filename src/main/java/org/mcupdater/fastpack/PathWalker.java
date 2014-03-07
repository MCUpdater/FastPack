package org.mcupdater.fastpack;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.commons.codec.digest.DigestUtils;
import org.mcupdater.model.*;

import java.io.*;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipFile;

public class PathWalker extends SimpleFileVisitor<Path> {
	private ServerDefinition server;
	private Path rootPath;
	private String urlBase;
	private final String sep = File.separator;
	private static int jarOrder;
	private Gson gson = new Gson();

	public PathWalker(ServerDefinition server, Path rootPath, String urlBase) {
		this.setServer(server);
		this.setRootPath(rootPath);
		this.setUrlBase(urlBase);
	}

	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		Path relativePath = rootPath.relativize(file);
		String downloadURL = urlBase + "/" + relativePath.toString().replace("\\","/").replace(" ", "%20");
		InputStream is = Files.newInputStream(file);
		String md5 = DigestUtils.md5Hex(is);
		String name = file.getFileName().toString();
		String id;
		int order = 0;
		name = name.substring(0,name.lastIndexOf("."));
		id = name.replace(" ", "");
		String depends = "";
		Boolean required = true;
		ModType modType = ModType.Regular;
		HashMap<String,String> mapMeta = new HashMap<>();
		//System.out.println(relativePath.toString());
		if (relativePath.toString().contains(".DS_Store")) { return FileVisitResult.CONTINUE; }
		if (relativePath.toString().indexOf(sep) >= 0) {
			switch (relativePath.toString().substring(0, relativePath.toString().indexOf(sep))) {
				case "bin":
				case "resources":
				case "saves":
				case "screenshots":
				case "stats":
				case "texturepacks":
				case "texturepacks-mp-cache":
				case "assets":
				case "resourcepacks":
					return FileVisitResult.CONTINUE;
				//
				case "instMods":
				case "jar":
				{
					modType = ModType.Jar;
					order = ++jarOrder;
					break;
				}
				case "coremods":
				{
					modType = ModType.Coremod;
					break;
				}
				case "config":
				{
					String newPath = relativePath.toString();
					if (sep.equals("\\")) {
						newPath = newPath.replace("\\", "/");
					}
					ConfigFile newConfig = new ConfigFile(downloadURL, newPath, false, md5);
					server.addConfig(newConfig);
					return FileVisitResult.CONTINUE;
				}
				case "lib":
				case "libraries":
					modType = ModType.Library;
					break;
				case "optional":
					required = false;
				default:
				{
					if (file.endsWith("litemod")) {
						modType = ModType.Litemod;
					}
				}
			}
		}
		try {
			ZipFile zf = new ZipFile(file.toFile());
			System.out.println(file.toString() + ": " + zf.size() + " entries in file.");
			if (zf.getEntry("mcmod.info") != null) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(zf.getInputStream(zf.getEntry("mcmod.info"))));
				MCModInfo info;
				JsonParser parser = new JsonParser();
				JsonElement rootElement = parser.parse(reader);
				if (rootElement.isJsonArray())
				{
					JsonArray jsonList = rootElement.getAsJsonArray();
					info = gson.fromJson(jsonList.get(0), MCModInfo.class);
				} else {
					if (rootElement.getAsJsonObject().has("modlist")) {
						info = gson.fromJson(rootElement.getAsJsonObject().getAsJsonArray("modlist").get(0), MCModInfo.class);
					} else {
						info = gson.fromJson(rootElement, MCModInfo.class);
					}
				}
				id = info.modId;
				name = info.name;
				String authors;
				if (info.authors.size() > 0) {
					authors = info.authors.toString();
				} else {
					authors = info.authorList.toString();
				}
				mapMeta.put("version", info.version);
				mapMeta.put("authors", authors.substring(1,authors.length()-1));
				mapMeta.put("description", info.description);
				mapMeta.put("credits", info.credits);
				mapMeta.put("url", info.url);
				reader.close();
			}
			zf.close();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			List<PrioritizedURL> urls = new ArrayList<>();
			urls.add(new PrioritizedURL(downloadURL,0));
			Module newMod = new Module(name,id,urls,depends,required,modType,order,false,false,true,md5,new ArrayList<ConfigFile>(),"both",null,mapMeta,"","",new ArrayList<GenericModule>());
			server.addModule(newMod);
		}
		return FileVisitResult.CONTINUE;
	}

	public void setRootPath(Path rootPath) {
		this.rootPath = rootPath;
	}

	public void setUrlBase(String urlBase) {
		this.urlBase = urlBase;
	}

	public void setServer(ServerDefinition server) {
		this.server = server;
	}
}
