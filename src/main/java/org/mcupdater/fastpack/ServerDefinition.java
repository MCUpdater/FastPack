package org.mcupdater.fastpack;

import org.apache.commons.codec.language.Soundex;
import org.apache.commons.lang3.StringUtils;
import org.mcupdater.model.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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

	public void assignConfigs(boolean debug) {
		System.out.println("Assigning configs to mods\n===============");
		//this.modules.get(0).setConfigs(tempConfigs);
		Soundex snd = new Soundex();
		int distance;
		for (ConfigFile config : tempConfigs) {
			System.out.println(config.getPath() + ":");
			Module tempModule = null;
			distance = 10000;
			String configName = config.getPath().substring(config.getPath().indexOf("/"), config.getPath().lastIndexOf("."));
			for (Module mod : modules) {
				try {
					int newDistance = StringUtils.getLevenshteinDistance(configName, mod.getId());
					for (Map.Entry<String,String> exception : FastPack.configExceptions.entrySet()) {
						if (configName.contains(exception.getKey()) && mod.getId().equals(exception.getValue())) {
							newDistance -= 15;
						}
					}
					if (configName.toLowerCase().contains(mod.getId().toLowerCase())) {
						newDistance -= 10;
					}
					if (configName.toLowerCase().contains(mod.getId().toLowerCase().substring(0, 3))) {
						newDistance -= 1;
					}
					if (snd.soundex(mod.getId()).equals(snd.soundex(configName))) {
						newDistance -= 10;
					} else if (snd.soundex(mod.getName()).equals(snd.soundex(configName))) {
						newDistance -= 10;
					}
					if (newDistance <= 5 || debug) {
						System.out.println("   >" + mod.getId() + " - " + newDistance + " (potential)");
					}
					if (newDistance < distance) {
						tempModule = mod;
						distance = newDistance;
					}
				} catch (Exception e) {
					System.out.println("Problem with Mod " + mod.getName() + " (" + mod.getId() + ") and config " + config.getPath() + " (" + configName + ")");
					e.printStackTrace();
				}
			}
			System.out.println(config.getPath() + ": " + tempModule.getName() + " (" + distance +")\n");
			modules.get(modules.indexOf(tempModule)).getConfigs().add(config);
		}
	}

	public List<Module> getModules() {
		return modules;
	}

	public void sortMods() {
		Collections.sort(modules, new ModuleComparator(ModuleComparator.Mode.IMPORTANCE));
	}
}
