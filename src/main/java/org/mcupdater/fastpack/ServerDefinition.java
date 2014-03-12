package org.mcupdater.fastpack;

import org.apache.commons.codec.language.Soundex;
import org.apache.commons.lang3.StringUtils;
import org.mcupdater.model.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ServerDefinition
{
	private ServerList entry;
	private List<Import> imports;
	private List<Module> modules;
	private List<ConfigFile> tempConfigs;

	public ServerDefinition() {
		this.entry = new ServerList();
		this.imports = new ArrayList<>();
		this.modules = new ArrayList<>();
		this.tempConfigs = new ArrayList<>();
	}

	public void addImport(Import newImport) {
		this.imports.add(newImport);
	}

	public void addConfig(ConfigFile newConfig) {
		tempConfigs.add(newConfig);
	}

	public void addModule(Module newMod) {
		modules.add(newMod);
	}

	public void setServerEntry(ServerList newEntry) {
		this.entry = newEntry;
	}

	public ServerList getServerEntry() {
		return entry;
	}

	public List<Import> getImports() {
		return imports;
	}

	public void assignConfigs() {
		//this.modules.get(0).setConfigs(tempConfigs);
		Soundex snd = new Soundex();
		int distance;
		for (ConfigFile config : tempConfigs) {
			//System.out.println(config.getPath() + " - " + snd.soundex(config.getPath().substring(config.getPath().indexOf("/"))));
			Module tempModule = null;
			distance = 10000;
			for (Module mod : modules) {
				int newDistance = StringUtils.getLevenshteinDistance(config.getPath().substring(config.getPath().indexOf("/")), mod.getId());
				if (snd.soundex(mod.getId()).equals(snd.soundex(config.getPath().substring(config.getPath().indexOf("/"))))) {
					newDistance -= 10;
				} else if (snd.soundex(mod.getName()).equals(snd.soundex(config.getPath().substring(config.getPath().indexOf("/"))))) {
					newDistance -= 10;
				}
				//System.out.println(" >" + mod.getId() + " - " + snd.soundex(mod.getId()));
				if (newDistance < distance) {
					tempModule = mod;
					distance = newDistance;
				}
			}
			System.out.println(config.getPath() + ": " + tempModule.getName() + " (" + distance +")");
			modules.get(modules.indexOf(tempModule)).getConfigs().add(config);
		}
	}

	public List<Module> getModules() {
		return modules;
	}

	public void sortMods() {
		Collections.sort(modules, new ModuleComparator());
	}
}
