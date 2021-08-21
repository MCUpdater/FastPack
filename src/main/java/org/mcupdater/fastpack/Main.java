package org.mcupdater.fastpack;

import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.BuiltinHelpFormatter;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.mcupdater.downloadlib.DownloadUtil;
import org.mcupdater.model.CurseProject;
import org.mcupdater.model.Import;
import org.mcupdater.model.Module;
import org.mcupdater.model.metadata.ProjectData;
import org.mcupdater.util.FastPack;
import org.mcupdater.util.MCUpdater;
import org.mcupdater.util.PathWalker;
import org.mcupdater.util.ServerDefinition;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.SimpleFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
	public static void main(final String[] args) {
		boolean hasForge = false;
		boolean hasFabric = false;
		boolean debug = false;
		boolean doConfigs = true;
		boolean onlyOverrides = false;
		boolean includeOptional = false;
		
		OptionParser optParser = new OptionParser();
		optParser.accepts("help", "Shows this help").forHelp();
		optParser.formatHelpWith(new BuiltinHelpFormatter(200, 3));
		ArgumentAcceptingOptionSpec<String> fileSpec = optParser.accepts("file", "Parse a single mod file (or download url) and exit").withRequiredArg().ofType(String.class);
		ArgumentAcceptingOptionSpec<String> importURLSpec = optParser.accepts("import", "Generate a pack from a supported 3rd party source (Curse)").withRequiredArg().ofType(String.class);
		ArgumentAcceptingOptionSpec<String> searchPathSpec = optParser.accepts("path", "Path to scan for mods and configs").requiredUnless("help","file","import").withRequiredArg().ofType(String.class);
		ArgumentAcceptingOptionSpec<String> baseURLSpec = optParser.accepts("baseURL", "Base URL for downloads").requiredUnless("help","file","import").withRequiredArg().ofType(String.class);
		ArgumentAcceptingOptionSpec<String> MCVersionSpec = optParser.accepts("mc", "Minecraft version").requiredUnless("help","file","import").withRequiredArg().ofType(String.class);
		ArgumentAcceptingOptionSpec<String> forgeVersionSpec = optParser.accepts("forge", "Forge version").withRequiredArg().ofType(String.class);
		ArgumentAcceptingOptionSpec<String> fabricVersionSpec = optParser.accepts("fabric","Fabric version").withRequiredArg().ofType(String.class);
		ArgumentAcceptingOptionSpec<String> yarnVersionSpec = optParser.accepts("yarn", "Yarn version").withOptionalArg().ofType(String.class).defaultsTo("latest");
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
		ArgumentAcceptingOptionSpec<String> lookupFileSpec = optParser.accepts("lookupFile", "").withRequiredArg().ofType(String.class);
		optParser.accepts("noConfigs", "Do not generate ConfigFile entries");
		optParser.accepts("configsOnly", "Generate all mods as overrides with ConfigFile entries");
		optParser.accepts("includeOptional", "Add an import statement to include the MCUpdater community optional mods pack");
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

		hasForge = options.has("forge");
		hasFabric = options.has("fabric");

		debug = options.has("debug");

		doConfigs = !options.has("noConfigs");
		onlyOverrides = options.has("configsOnly");
		includeOptional = options.has("includeOptional");

		String serverName = serverNameSpec.value(options);
		String serverId = serverIdSpec.value(options);
		String serverAddr = serverAddrSpec.value(options);
		String mainClass = mainClassSpec.value(options);
		String newsURL = newsURLSpec.value(options);
		String iconURL = iconURLSpec.value(options);
		Boolean autoConnect = autoConnectSpec.value(options);
		String stylesheet = stylesheetSpec.value(options);

		String importURL = importURLSpec.value(options);
		Path xmlPath = new File(xmlPathSpec.value(options)).toPath();

		ServerDefinition definition;
		if( importURL == null || importURL.isEmpty() ) {
			List<ProjectData> projects = null;
			Path searchPath = new File(searchPathSpec.value(options)).toPath();
			
			final String sourcePack = sourcePackURLSpec.value(options);
			final String sourceId = sourcePackIdSpec.value(options);
			final String revision = revisionSpec.value(options);
			final String baseURL = baseURLSpec.value(options);
			final String MCVersion = MCVersionSpec.value(options);

			if (options.has("lookupFile")) {
				projects = MCUpdater.loadProjectData(FileSystems.getDefault().getPath(lookupFileSpec.value(options)));
			}

			definition = FastPack.doFastPack(sourcePack, sourceId, serverName, serverId, serverAddr, mainClass, newsURL, iconURL, revision, autoConnect, MCVersion, searchPath, baseURL, debug, projects);

			if (hasForge) {
				final String forgeVersion = forgeVersionSpec.value(options);
				definition.addForge(MCVersion, forgeVersion);
			}
			if (hasFabric) {
				final String fabricVersion = fabricVersionSpec.value(options);
				final String yarnVersion = yarnVersionSpec.value(options);
				definition.addFabric(MCVersion, fabricVersion, yarnVersion);
			}
			if (includeOptional) {
				definition.addImport(new Import("https://files.mcupdater.com/optional/ServerPack.xml", "optional"));
			}
		} else {
			definition = FastPack.doImport(importURL, serverName, serverId, serverAddr, mainClass, newsURL, iconURL, autoConnect, debug);
		}

		List<Module> sortedModules = definition.sortMods();
		Map<String,String> issues = new HashMap<>();
		if (doConfigs) definition.assignConfigs(issues, debug);

		definition.writeServerPack(stylesheet, xmlPath, sortedModules, onlyOverrides);
	}

	private static void parseOneFile(String fname) {
		final ServerDefinition definition = new ServerDefinition();
		final List<Module> modList;
		
		final File f = new File(fname);
		if( f.exists() && f.isFile() ) {
			definition.addModule((Module) PathWalker.handleOneFile(definition, f, null));
			modList = definition.sortMods();
		} else {
			// detect curse file url
			final Pattern regex = Pattern.compile("curseforge.com\\/projects\\/(?<project>[a-zA-Z_0-9\\-]+)\\/files\\/(?<filenum>\\d+)");
			final Matcher match = regex.matcher(fname);
			final boolean curse = match.find();
			if( curse && !fname.endsWith("download") ) {
				fname += "/download";
			}
			
			final URL url;
			try {
				url = new URL(fname);
			} catch( MalformedURLException e ) {
				// we've already verified that it isn't a file on disk, so bad url means:
				System.out.println("!! Unable to find file '"+fname+"'");
				return;
			}

			final File tmp;
			final Path path;
			try {
				tmp = DownloadUtil.getToTemp(url, "import", ".jar");
				path = tmp.toPath();
				if( Files.size(path) == 0 ) {
					System.out.println("!! got zero bytes from "+url);
					return;
				}
			} catch (IOException e) {
				System.out.println("!! Unable to download "+url);
				return;
			}
			
			definition.addModule((Module) PathWalker.handleOneFile(definition, tmp, fname));
			modList = definition.sortMods();
			
			if( curse ) {
				Module mod = modList.get(0);
				mod.setCurseProject(new CurseProject(match.group("project"), Integer.parseInt(match.group("filenum"))));
			}
		}
		
		final BufferedWriter stdout = new BufferedWriter(new OutputStreamWriter(System.out));
		try {
			stdout.newLine();
			ServerDefinition.generateServerDetailXML(stdout, new ArrayList<>(), new ArrayList<>(), modList, false);
			stdout.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
