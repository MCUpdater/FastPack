package org.mcupdater.fastpack;

import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.BuiltinHelpFormatter;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.mcupdater.model.*;
import org.mcupdater.util.ServerDefinition;
import org.mcupdater.util.ServerPackParser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FastPack {
	public static boolean hasForge = false;
	private static boolean debug = false;
	private static boolean doConfigs = true;
	private static boolean onlyOverrides = false;

	public static void main(final String[] args) {
		OptionParser optParser = new OptionParser();
		optParser.accepts("help", "Shows this help").isForHelp();
		optParser.formatHelpWith(new BuiltinHelpFormatter(200, 3));
		ArgumentAcceptingOptionSpec<String> searchPathSpec = optParser.accepts("path", "Path to scan for mods and configs").requiredUnless("help").withRequiredArg().ofType(String.class);
		ArgumentAcceptingOptionSpec<String> baseURLSpec = optParser.accepts("baseURL", "Base URL for downloads").requiredUnless("help").withRequiredArg().ofType(String.class);
		ArgumentAcceptingOptionSpec<String> MCVersionSpec = optParser.accepts("mc", "Minecraft version").requiredUnless("help").withRequiredArg().ofType(String.class);
		ArgumentAcceptingOptionSpec<String> forgeVersionSpec = optParser.accepts("forge", "Forge version").withRequiredArg().ofType(String.class);
		ArgumentAcceptingOptionSpec<String> xmlPathSpec = optParser.accepts("out", "XML file to write").requiredUnless("help").withRequiredArg().ofType(String.class);
		ArgumentAcceptingOptionSpec<String> serverAddrSpec = optParser.accepts("mcserver", "Server address").withRequiredArg().ofType(String.class).defaultsTo("");
		ArgumentAcceptingOptionSpec<String> serverNameSpec = optParser.accepts("name", "Server name").withRequiredArg().ofType(String.class).defaultsTo("FastPack Instance");
		ArgumentAcceptingOptionSpec<String> serverIdSpec = optParser.accepts("id", "Server ID").withRequiredArg().ofType(String.class).defaultsTo("fastpack");
		ArgumentAcceptingOptionSpec<String> mainClassSpec = optParser.accepts("mainClass", "Main class for launching Minecraft").withRequiredArg().ofType(String.class).defaultsTo("net.minecraft.launchwrapper.Launch");
		ArgumentAcceptingOptionSpec<String> newsURLSpec = optParser.accepts("newsURL", "URL to display in the News tab").withRequiredArg().ofType(String.class).defaultsTo("about:blank");
		ArgumentAcceptingOptionSpec<String> iconURLSpec = optParser.accepts("iconURL", "URL of icon to display in instance list").withRequiredArg().ofType(String.class).defaultsTo("");
		ArgumentAcceptingOptionSpec<String> revisionSpec = optParser.accepts("revision", "Revision string to display").withRequiredArg().ofType(String.class).defaultsTo("1");
		ArgumentAcceptingOptionSpec<Boolean> autoConnectSpec = optParser.accepts("autoConnect", "Auto-connect to server on launch").withRequiredArg().ofType(Boolean.class).defaultsTo(Boolean.TRUE);
		ArgumentAcceptingOptionSpec<String> stylesheetSpec = optParser.accepts("xslt", "Path of XSLT file").withRequiredArg().ofType(String.class).defaultsTo("");
		ArgumentAcceptingOptionSpec<String> sourcePackURLSpec = optParser.accepts("sourcePackURL", "URL of pack to load - useful with configsOnly").withRequiredArg().ofType(String.class).defaultsTo("");
		ArgumentAcceptingOptionSpec<String> sourcePackIdSpec = optParser.accepts("sourcePackId", "Server ID of source pack").requiredIf("sourcePackURL").withRequiredArg().ofType(String.class);
		optParser.accepts("noConfigs", "Do not generate ConfigFile entries");
		optParser.accepts("configsOnly", "Generate all mods as overrides with ConfigFile entries");
		optParser.accepts("debug", "Output full config matching data");
		final OptionSet options = optParser.parse(args);

		if (options.has("help")) {
			try {
				optParser.printHelpOn(System.out);
				return;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		if (options.has("forge")) {
			hasForge = true;
		}

		if (options.has("debug")) {
			debug = true;
		}

		if (options.has("noConfigs")) {
			doConfigs = false;
		}

		if (options.has("configsOnly")) {
			onlyOverrides = true;
		}

		ServerDefinition definition = new ServerDefinition();
		ServerList entry;
		String sourcePack = sourcePackURLSpec.value(options);
		if (sourcePack.isEmpty()) {
			entry = new ServerList();
		} else {
			entry = ServerPackParser.loadFromURL(sourcePack, sourcePackIdSpec.value(options));
		}
		entry.setName(serverNameSpec.value(options));
		entry.setServerId(serverIdSpec.value(options));
		entry.setAddress(serverAddrSpec.value(options));
		entry.setMainClass(mainClassSpec.value(options));
		entry.setNewsUrl(newsURLSpec.value(options));
		entry.setIconUrl(iconURLSpec.value(options));
		entry.setRevision(revisionSpec.value(options));
		entry.setAutoConnect(autoConnectSpec.value(options));
		entry.setVersion(MCVersionSpec.value(options));
		definition.setServerEntry(entry);
		if (hasForge) {
			definition.addImport(new Import("http://files.mcupdater.com/example/forge.php?mc=" + MCVersionSpec.value(options) + "&forge=" + forgeVersionSpec.value(options), "forge"));
		}
		String stylesheet = stylesheetSpec.value(options);

		Path searchPath = new File(searchPathSpec.value(options)).toPath();
		Path xmlPath = new File(xmlPathSpec.value(options)).toPath();

		PathWalker pathWalk = new PathWalker(definition, searchPath, baseURLSpec.value(options));
		try {
			Files.walkFileTree(searchPath, pathWalk);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (debug) {
			for (Module modEntry : definition.getModules().values()) {
				System.out.println(modEntry.toString());
			}
		}
		if (hasForge) {
			definition.addModule(new Module("Minecraft Forge", "forge-" + forgeVersionSpec.value(options), new ArrayList<PrioritizedURL>(), "", true, ModType.Override, 0, false, false, true, "", new ArrayList<ConfigFile>(), "BOTH", "", new HashMap<String, String>(), "", "", new ArrayList<GenericModule>(), ""));
		}
        List<Module> sortedModules = definition.sortMods();
		if (doConfigs) definition.assignConfigs(debug);

		definition.writeServerPack(stylesheet, xmlPath, sortedModules, onlyOverrides);
	}

}
