package org.mcupdater.fastpack;

import org.apache.commons.lang3.StringUtils;
import org.mcupdater.model.*;

import java.io.File;
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
		for (ConfigFile config : tempConfigs) {
			Module tempModule = null;
			int distance = 10000;
			for (Module mod : modules) {
				int newDistance = StringUtils.getLevenshteinDistance(config.getPath().substring(config.getPath().indexOf(File.separator)), mod.getId());
				if (newDistance < distance) {
					tempModule = mod;
					distance = newDistance;
				}
				System.out.println(config.getPath() + ": " + tempModule.getName() + " (" + distance +") / " + mod.getName() + " (" + newDistance + ")");
			}
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
