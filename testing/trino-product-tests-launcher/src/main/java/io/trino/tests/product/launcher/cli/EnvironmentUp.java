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
package io.trino.tests.product.launcher.cli;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Module;
import io.airlift.log.Logger;
import io.trino.tests.product.launcher.Extensions;
import io.trino.tests.product.launcher.LauncherModule;
import io.trino.tests.product.launcher.docker.ContainerUtil;
import io.trino.tests.product.launcher.env.DockerContainer;
import io.trino.tests.product.launcher.env.Environment;
import io.trino.tests.product.launcher.env.EnvironmentConfig;
import io.trino.tests.product.launcher.env.EnvironmentFactory;
import io.trino.tests.product.launcher.env.EnvironmentModule;
import io.trino.tests.product.launcher.env.EnvironmentOptions;
import org.testcontainers.DockerClientFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;

import javax.inject.Inject;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static io.trino.tests.product.launcher.cli.Commands.runCommand;
import static io.trino.tests.product.launcher.env.EnvironmentContainers.TEMPTO_CONFIG_FILES_ENV;
import static io.trino.tests.product.launcher.env.EnvironmentContainers.TESTS;
import static io.trino.tests.product.launcher.env.EnvironmentContainers.isPrestoContainer;
import static io.trino.tests.product.launcher.env.EnvironmentListener.getStandardListeners;
import static io.trino.tests.product.launcher.env.common.Standard.CONTAINER_TEMPTO_PROFILE_CONFIG;
import static java.util.Objects.requireNonNull;
import static picocli.CommandLine.Mixin;
import static picocli.CommandLine.Option;

@Command(
        name = "up",
        description = "Start an environment",
        usageHelpAutoWidth = true)
public final class EnvironmentUp
        implements Callable<Integer>
{
    private static final Logger log = Logger.get(EnvironmentUp.class);

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit")
    public boolean usageHelpRequested;

    @Mixin
    public EnvironmentOptions environmentOptions = new EnvironmentOptions();

    @Mixin
    public EnvironmentUpOptions environmentUpOptions = new EnvironmentUpOptions();

    private final Module additionalEnvironments;

    public EnvironmentUp(Extensions extensions)
    {
        this.additionalEnvironments = requireNonNull(extensions, "extensions is null").getAdditionalEnvironments();
    }

    @Override
    public Integer call()
    {
        return runCommand(
                ImmutableList.<Module>builder()
                        .add(new LauncherModule())
                        .add(new EnvironmentModule(environmentOptions, additionalEnvironments))
                        .add(environmentUpOptions.toModule())
                        .build(),
                EnvironmentUp.Execution.class);
    }

    public static class EnvironmentUpOptions
    {
        private static final String DEFAULT_VALUE = "(default: ${DEFAULT-VALUE})";

        @Option(names = "--background", description = "Keep containers running in the background once they are started " + DEFAULT_VALUE)
        public boolean background;

        @Option(names = "--environment", paramLabel = "<environment>", description = "Name of the environment to start", required = true)
        public String environment;

        @Option(names = "--option", paramLabel = "<option>", description = "Extra options to provide to environment (property can be used multiple times; format is key=value)")
        public Map<String, String> extraOptions = new HashMap<>();

        @Option(names = "--logs-dir", paramLabel = "<dir>", description = "Location of the exported logs directory " + DEFAULT_VALUE)
        public Optional<Path> logsDirBase;

        @Option(names = "--tempto-resources", paramLabel = "<path>", description = "Path where Tempto configuration file and other resources will be written to " + DEFAULT_VALUE)
        public Optional<Path> temptoResources;

        public Module toModule()
        {
            return binder -> binder.bind(EnvironmentUpOptions.class).toInstance(this);
        }
    }

    public static class Execution
            implements Callable<Integer>
    {
        private final EnvironmentFactory environmentFactory;
        private final boolean withoutPrestoMaster;
        private final boolean background;
        private final String environment;
        private final EnvironmentConfig environmentConfig;
        private final Optional<Path> logsDirBase;
        private final DockerContainer.OutputMode outputMode;
        private final Map<String, String> extraOptions;
        private final Optional<Path> temptoResources;

        @Inject
        public Execution(EnvironmentFactory environmentFactory, EnvironmentConfig environmentConfig, EnvironmentOptions options, EnvironmentUpOptions environmentUpOptions)
        {
            this.environmentFactory = requireNonNull(environmentFactory, "environmentFactory is null");
            this.environmentConfig = requireNonNull(environmentConfig, "environmentConfig is null");
            this.withoutPrestoMaster = options.withoutPrestoMaster;
            this.background = environmentUpOptions.background;
            this.environment = environmentUpOptions.environment;
            this.outputMode = requireNonNull(options.output, "options.output is null");
            this.logsDirBase = requireNonNull(environmentUpOptions.logsDirBase, "environmentUpOptions.logsDirBase is null");
            this.extraOptions = ImmutableMap.copyOf(requireNonNull(environmentUpOptions.extraOptions, "environmentUpOptions.extraOptions is null"));
            this.temptoResources = environmentUpOptions.temptoResources;
        }

        @Override
        public Integer call()
        {
            Optional<Path> environmentLogPath = logsDirBase.map(dir -> dir.resolve(environment));
            Environment.Builder builder = environmentFactory.get(environment, environmentConfig, extraOptions)
                    .setContainerOutputMode(outputMode)
                    .setLogsBaseDir(environmentLogPath);

            temptoResources.ifPresent(path -> builder.configureContainer(TESTS, container -> {
                String[] defaultNames = new String[]{
                        "/docker/presto-product-tests/conf/tempto/tempto-configuration-for-docker-default.yaml",
                        CONTAINER_TEMPTO_PROFILE_CONFIG,
                        environmentConfig.getTemptoEnvironmentConfigFile()};
                String[] envNames = container.getEnvMap().get(TEMPTO_CONFIG_FILES_ENV).split(",");
                String[] names = Arrays.copyOf(defaultNames, defaultNames.length + envNames.length);
                System.arraycopy(envNames, 0, names, defaultNames.length, envNames.length);


                container.setCommand("sleep 100000");
                container.reset();
                container.start();

                String combined = getCombinedTemptoConfiguration(Arrays.asList(names), container, path);
                try {
                    Files.writeString(path.resolve("tempto-configuration.yaml"), combined);
                }
                catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                combined.lines()
                        .filter(line -> line.contains("${tests.paths_prefix}"))
                        .map(line -> line.substring(line.indexOf("${tests.paths_prefix}") + "${tests.paths_prefix}".length()))
                        .forEach(containerPath -> {
                            Path relativePath = path.resolve(containerPath.replaceAll("^/+", ""));
                            relativePath.toFile().getParentFile().mkdirs();
                            container.copyFileFromContainer(containerPath, relativePath.toString());
                        });
                container.stop();
            }));

            builder.removeContainer(TESTS);

            if (withoutPrestoMaster) {
                builder.removeContainers(container -> isPrestoContainer(container.getLogicalName()));
            }

            log.info("Creating environment '%s' with configuration %s and options %s", environment, environmentConfig, extraOptions);
            Environment environment = builder.build(getStandardListeners(environmentLogPath));
            environment.start();

            if (background) {
                killContainersReaperContainer();
                return ExitCode.OK;
            }

            environment.awaitContainersStopped();
            environment.stop();

            return ExitCode.OK;
        }

        private String getCombinedTemptoConfiguration(List<String> names, DockerContainer container, Path path)
        {
            Path tempDir;
            try {
                tempDir = Files.createTempDirectory("tempto-configs");
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return names.stream()
                    .filter(containerPath -> !containerPath.equals("/dev/null"))
                    .map(containerPath -> {
                        Path localPath = tempDir.resolve("file.yaml");
                        container.copyFileFromContainer(containerPath, localPath.toString());
                        try {
                            return "---\n" + Files.readString(localPath);
                        }
                        catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    })
                    .collect(Collectors.joining("\n"));
        }

        private static void killContainersReaperContainer()
        {
            log.info("Killing the testcontainers reaper container (Ryuk) so that environment can stay alive");
            ContainerUtil.killContainersReaperContainer(DockerClientFactory.lazyClient());
        }
    }
}
