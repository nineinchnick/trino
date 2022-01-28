/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.trino.server;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import io.airlift.log.Logger;
import io.trino.spi.Plugin;
import io.trino.spi.classloader.ThreadContextClassLoader;
import io.trino.spi.connector.ConnectorFactory;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.AbstractMap.SimpleEntry;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Arrays.asList;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

public class PluginReader
{
    private static final Logger log = Logger.get(PluginReader.class);

    private PluginReader()
    {
    }

    public static void main(String[] args)
    {
        Map<String, String> modulesToPlugins = mapModulesToPlugins();
        Map<String, List<String>> pluginsToConnectors = mapPluginsToConnectors(new File(args[0]));
        Optional<List<String>> impactedModules = readImpactedModules(new File(args[1]));
        if (!impactedModules.isPresent()) {
            System.exit(1);
        }
    }

    private static Map<String, String> mapModulesToPlugins()
    {
        Optional<List<String>> modules = readTrinoPlugins(new File("pom.xml"));
        if (modules.isEmpty()) {
            return null;
        }
        return modules.get().stream()
                .map(module -> {
                    try {
                        Optional<Path> jarFile = java.nio.file.Files.find(Path.of(module, "/target"),
                                0,
                                (path, basicFileAttributes) -> path.toFile().getName().matches(".*-services.jar")
                        ).findFirst();
                        if (jarFile.isPresent()) {
                            return new SimpleEntry<>(module, readPluginClassName(jarFile.get().toFile()));
                        }
                    }
                    catch (IOException e) {
                       log.warn(e, "Couldn't find services jar for module %s", module);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static Optional<List<String>> readTrinoPlugins(File rootPom)
    {
        try (FileReader fileReader = new FileReader(rootPom)) {
            MavenXpp3Reader reader = new MavenXpp3Reader();
            Model model = reader.read(fileReader);
            return Optional.of(
                    model.getModules().stream()
                            .filter(PluginReader::isTrinoPlugin)
                            .collect(toImmutableList()));
        }
        catch (IOException e) {
            log.warn(e, "Couldn't read file %s", rootPom);
            return Optional.empty();
        }
        catch (XmlPullParserException e) {
            log.warn(e, "Couldn't parse file %s", rootPom);
            return Optional.empty();
        }
    }

    private static boolean isTrinoPlugin(String module)
    {
        String modulePom = module + "/pom.xml";
        try (FileReader fileReader = new FileReader(modulePom)) {
            MavenXpp3Reader reader = new MavenXpp3Reader();
            Model model = reader.read(fileReader);
            return model.getPackaging().equals("trino-plugin");
        }
        catch (IOException e) {
            log.warn(e, "Couldn't read file %s", modulePom);
            return false;
        }
        catch (XmlPullParserException e) {
            log.warn(e, "Couldn't parse file %s", modulePom);
            return false;
        }
    }

    private static String readPluginClassName(File serviceJar)
    {
        try {
            ZipFile zipFile = new ZipFile(serviceJar);
            Enumeration<? extends ZipEntry> e = zipFile.entries();

            while (e.hasMoreElements()) {
                ZipEntry entry = e.nextElement();
                // if the entry is not directory and matches relative file then extract it
                if (entry.isDirectory() || !entry.getName().equals("META-INF/services/io.trino.spi.Plugin")) {
                    continue;
                }
                BufferedInputStream bis = new BufferedInputStream(zipFile.getInputStream(entry));
                String fileContentsStr = new String(ByteStreams.toByteArray(bis), Charsets.UTF_8);
                bis.close();
                return fileContentsStr;
            }
        } catch (IOException e) {
            log.error("IOError :" + e);
            e.printStackTrace();
        }
        return null;
    }

    private static Optional<List<String>> readImpactedModules(File gibImpactedModules)
    {
        try {
            return Optional.of(Files.asCharSource(gibImpactedModules, StandardCharsets.UTF_8).readLines());
        }
        catch (IOException e) {
            log.warn(e, "Couldn't read file %s", gibImpactedModules);
            return Optional.empty();
        }
    }

    private static Map<String, List<String>> mapPluginsToConnectors(File path)
    {
        ServerPluginsProviderConfig config = new ServerPluginsProviderConfig();
        config.setInstalledPluginsDir(path);
        ServerPluginsProvider pluginsProvider = new ServerPluginsProvider(config, directExecutor());
        HashMap<String, List<String>> connectors = new HashMap<>();
        pluginsProvider.loadPlugins((plugin, createClassLoader) -> loadPlugin(createClassLoader, connectors), PluginManager::createClassLoader);
        return connectors;
    }

    private static void loadPlugin(Supplier<PluginClassLoader> createClassLoader, Map<String, List<String>> connectors)
    {
        PluginClassLoader pluginClassLoader = createClassLoader.get();
        try (ThreadContextClassLoader ignored = new ThreadContextClassLoader(pluginClassLoader)) {
            loadServicePlugin(pluginClassLoader, connectors);
        }
    }

    private static void loadServicePlugin(PluginClassLoader pluginClassLoader, Map<String, List<String>> connectors)
    {
        ServiceLoader<Plugin> serviceLoader = ServiceLoader.load(Plugin.class, pluginClassLoader);
        List<Plugin> plugins = ImmutableList.copyOf(serviceLoader);
        checkState(!plugins.isEmpty(), "No service providers of type %s in the classpath: %s", Plugin.class.getName(), asList(pluginClassLoader.getURLs()));

        for (Plugin plugin : plugins) {
            connectors.put(plugin.getClass().getName(), getPluginConnectors(plugin));
        }
    }

    private static List<String> getPluginConnectors(Plugin plugin)
    {
        ImmutableList.Builder<String> connectorNames = ImmutableList.builder();

        for (ConnectorFactory connectorFactory : plugin.getConnectorFactories()) {
            connectorNames.add(connectorFactory.getName());
        }

        return connectorNames.build();
    }
}
