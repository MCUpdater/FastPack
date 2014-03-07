package org.mcupdater.fastpack;

import org.mcupdater.model.ConfigFile;
import org.mcupdater.model.Import;
import org.mcupdater.model.Module;
import org.mcupdater.model.ServerList;

import java.util.ArrayList;
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
		this.modules.get(0).setConfigs(tempConfigs);
	}

	public List<Module> getModules() {
		return modules;
	}
}
