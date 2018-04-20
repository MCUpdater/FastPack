package org.mcupdater.fastpack;

import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.BuiltinHelpFormatter;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.apache.commons.compress.utils.Lists;
import org.mcupdater.model.*;
import org.mcupdater.util.FastPack;
import org.mcupdater.util.MCUpdater;
import org.mcupdater.util.PathWalker;
import org.mcupdater.util.ServerDefinition;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.SimpleFormatter;

public class Main {
	public static void main(final String[] args) {
		boolean hasForge = false;
		boolean debug = false;
		boolean doConfigs = true;
		boolean onlyOverrides = false;

		OptionParser optParser = new OptionParser();
		optParser.accepts("help", "Shows this help").isForHelp();
		optParser.formatHelpWith(new BuiltinHelpFormatter(200, 3));
		ArgumentAcceptingOptionSpec<String> fileSpec = optParser.accepts("file", "Parse a single mod file and exit").withRequiredArg().ofType(String.class);
		ArgumentAcceptingOptionSpec<String> searchPathSpec = optParser.accepts("path", "Path to scan for mods and configs").requiredUnless("help","file").withRequiredArg().ofType(String.class);
		ArgumentAcceptingOptionSpec<String> baseURLSpec = optParser.accepts("baseURL", "Base URL for downloads").requiredUnless("help","file").withRequiredArg().ofType(String.class);
		ArgumentAcceptingOptionSpec<String> MCVersionSpec = optParser.accepts("mc", "Minecraft version").requiredUnless("help","file").withRequiredArg().ofType(String.class);
		ArgumentAcceptingOptionSpec<String> forgeVersionSpec = optParser.accepts("forge", "Forge version").withRequiredArg().ofType(String.class);
		ArgumentAcceptingOptionSpec<String> xmlPathSpec = optParser.accepts("out", "XML file to write").requiredUnless("help","file").withRequiredArg().ofType(String.class);
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
		
		final OptionSet options;
		try {
			options = optParser.parse(args);
		} catch( Exception ex ) {
			System.err.println( ex.getMessage() );
			return;
		}
		
		if( options.has("file")) {
			parseOneFile(fileSpec.value(options));
			return;
		}

		if (options.has("help")) {
			try {
				optParser.printHelpOn(System.out);
				return;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		MCUpdater.getInstance();
		ConsoleHandler handler = new ConsoleHandler();
		handler.setFormatter(new SimpleFormatter());
		MCUpdater.apiLogger.addHandler(handler);		

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
		String sourcePack = sourcePackURLSpec.value(options);
		String sourceId = sourcePackIdSpec.value(options);
		String serverName = serverNameSpec.value(options);
		String serverId = serverIdSpec.value(options);
		String serverAddr = serverAddrSpec.value(options);
		String mainClass = mainClassSpec.value(options);
		String newsURL = newsURLSpec.value(options);
		String iconURL = iconURLSpec.value(options);
		String revision = revisionSpec.value(options);
		Boolean autoConnect = autoConnectSpec.value(options);
		String MCVersion = MCVersionSpec.value(options);
		String forgeVersion = forgeVersionSpec.value(options);
		String stylesheet = stylesheetSpec.value(options);
		Path searchPath = new File(searchPathSpec.value(options)).toPath();
		Path xmlPath = new File(xmlPathSpec.value(options)).toPath();
		String baseURL = baseURLSpec.value(options);

		ServerDefinition definition = FastPack.doFastPack(sourcePack, sourceId, serverName, serverId, serverAddr, mainClass, newsURL, iconURL, revision, autoConnect, MCVersion, searchPath, baseURL, debug);
		if (hasForge) {
			definition.addForge(MCVersion, forgeVersion);
		}
		List<Module> sortedModules = definition.sortMods();
		Map<String,String> issues = new HashMap<>();
		if (doConfigs) definition.assignConfigs(issues, debug);

		definition.writeServerPack(stylesheet, xmlPath, sortedModules, onlyOverrides);
	}

	private static void parseOneFile(String fname) {
		File f = new File(fname);
		if ( f == null || !f.exists() || !f.isFile() ) {
			System.out.println("!! Unable to find '"+fname+"'");
			return;
		}

		final Path searchPath;
		if( f.getParent() == null ) {
			searchPath = new File(".").toPath();
		} else {
			searchPath = f.getParentFile().toPath();	
		}
		
		final ServerDefinition definition = new ServerDefinition();
		final PathWalker walker = new PathWalker(definition,searchPath,"[PATH]");
		
		try {
			walker.visitFile(f.toPath(), null);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		final BufferedWriter stdout = new BufferedWriter(new OutputStreamWriter(System.out));
		try {
			stdout.newLine();
			ServerDefinition.generateServerDetailXML(stdout, new ArrayList<Import>(), definition.sortMods(), false);
			stdout.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
