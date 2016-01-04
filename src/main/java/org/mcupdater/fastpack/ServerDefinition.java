package org.mcupdater.fastpack;

import org.apache.commons.codec.language.Soundex;
import org.apache.commons.lang3.StringUtils;
import org.mcupdater.model.*;

import java.util.*;

public class ServerDefinition
{
	private ServerList entry;
	private List<Import> imports;
	private Map<String, Module> modules;
	private List<ConfigFile> tempConfigs;

	public ServerDefinition() {
		this.entry = new ServerList();
		this.imports = new ArrayList<>();
		this.modules = new HashMap<>();
		this.tempConfigs = new ArrayList<>();
	}

	public void addImport(Import newImport) {
		this.imports.add(newImport);
	}

	public void addConfig(ConfigFile newConfig) {
		tempConfigs.add(newConfig);
	}

	public void addModule(Module newMod) {
        if (modules.containsKey(newMod.getId())) {
            System.out.println("Warning: ModID: " + newMod.getId() + " belonging to " + newMod.getName() + " already exists in the list, and is being overwritten.");
        }
		modules.put(newMod.getId(), newMod);
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
			for (Module mod : modules.values()) {
				try {
					int newDistance = StringUtils.getLevenshteinDistance(configName, mod.getId());
					for (Map.Entry<String,String> exception : FastPack.configExceptions.entrySet()) {
						if (config.getPath().contains(exception.getKey()) && mod.getId().matches(exception.getValue())) {
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
            if (tempModule != null) {
                System.out.println(config.getPath() + ": " + tempModule.getName() + " (" + distance + ")\n");
	            if (tempModule.getSide().equals(ModSide.CLIENT)) {
		            config.setNoOverwrite(true);
	            }
                modules.get(tempModule.getId()).getConfigs().add(config);
            } else {
                System.out.println(config.getPath() + " could not be assigned to a module!");
            }
		}
	}

	public Map<String, Module> getModules() {
		return modules;
	}

	public List<Module> sortMods() {
        List<Module> sorted = new ArrayList<>(modules.values());
		Collections.sort(sorted, new ModuleComparator(ModuleComparator.Mode.IMPORTANCE));
        return sorted;
	}
}
