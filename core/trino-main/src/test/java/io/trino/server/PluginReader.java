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

import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import io.airlift.log.Logger;
import io.trino.spi.Plugin;
import io.trino.spi.classloader.ThreadContextClassLoader;
import io.trino.spi.connector.ConnectorFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkState;
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
        // TODO choose a fancy name for this program
        // TODO move this to the PTL module
        Map<String, List<String>> pluginsToConnectors = mapPluginsToConnectors(new File(args[0]));
        // TODO read Maven modules and filter plugin modules
        // TODO map modules through plugins to connectors
        Optional<List<String>> impactedModules = readImpactedModules(new File(args[1]));
        // TODO print a list of connectors from impacted modules
    }

    private Optional<List<String>> readMavenModules(File rootPom)
    {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = reader.read(new FileReader(rootPom));

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
