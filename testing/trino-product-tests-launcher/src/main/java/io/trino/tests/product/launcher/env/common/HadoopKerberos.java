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
package io.trino.tests.product.launcher.env.common;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.google.common.collect.ImmutableList;
import io.trino.tests.product.launcher.docker.DockerFiles;
import io.trino.tests.product.launcher.env.Environment;
import io.trino.tests.product.launcher.env.EnvironmentConfig;
import io.trino.tests.product.launcher.testcontainers.PortBinder;

import javax.inject.Inject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static io.trino.tests.product.launcher.env.EnvironmentContainers.COORDINATOR;
import static io.trino.tests.product.launcher.env.EnvironmentContainers.HADOOP;
import static io.trino.tests.product.launcher.env.EnvironmentContainers.TESTS;
import static io.trino.tests.product.launcher.env.EnvironmentContainers.configureTempto;
import static io.trino.tests.product.launcher.env.common.Standard.CONTAINER_PRESTO_CONFIG_PROPERTIES;
import static java.util.Objects.requireNonNull;
import static org.testcontainers.utility.MountableFile.forHostPath;

public class HadoopKerberos
        implements EnvironmentExtender
{
    private final DockerFiles.ResourceProvider configDir;
    private final PortBinder portBinder;

    private final String hadoopBaseImage;
    private final String hadoopImagesVersion;

    private final Hadoop hadoop;

    @Inject
    public HadoopKerberos(
            DockerFiles dockerFiles,
            PortBinder portBinder,
            EnvironmentConfig environmentConfig,
            Hadoop hadoop)
    {
        this.configDir = dockerFiles.getDockerFilesHostDirectory("common/hadoop-kerberos/");
        this.portBinder = requireNonNull(portBinder, "portBinder is null");
        hadoopBaseImage = requireNonNull(environmentConfig, "environmentConfig is null").getHadoopBaseImage();
        hadoopImagesVersion = requireNonNull(environmentConfig, "environmentConfig is null").getHadoopImagesVersion();
        this.hadoop = requireNonNull(hadoop, "hadoop is null");
    }

    @Override
    public void extendEnvironment(Environment.Builder builder)
    {
        String dockerImageName = hadoopBaseImage + "-kerberized:" + hadoopImagesVersion;
        builder.configureContainer(HADOOP, container -> {
            container.setDockerImageName(dockerImageName);
            portBinder.exposePort(container, 88);

            container.withCreateContainerCmdModifier(cmd -> {
                // Add previously exposed ports and UDP port
                List<ExposedPort> exposedPorts = new ArrayList<>(List.of(ExposedPort.udp(88)));
                if (cmd.getExposedPorts() != null) {
                    exposedPorts.addAll(Arrays.asList(cmd.getExposedPorts()));
                }
                cmd.withExposedPorts(exposedPorts);

                HostConfig hostConfig = new HostConfig();
                if (cmd.getHostConfig() != null) {
                    hostConfig = cmd.getHostConfig();
                }
                Ports ports = new Ports();
                if (hostConfig.getPortBindings() != null) {
                    ports = hostConfig.getPortBindings();
                }
                ports.add(PortBinding.parse("88:88/udp"));
                hostConfig.withPortBindings(ports);
                cmd.withHostConfig(hostConfig);
            });
        });
        builder.configureContainer(COORDINATOR, container -> {
            container.setDockerImageName(dockerImageName);
            portBinder.exposePort(container, 7778);
            container
                    .withCreateContainerCmdModifier(createContainerCmd -> createContainerCmd.withDomainName("docker.cluster"))
                    .withCopyFileToContainer(forHostPath(configDir.getPath("config.properties")), CONTAINER_PRESTO_CONFIG_PROPERTIES);
        });
        builder.configureContainer(TESTS, container -> {
            container.setDockerImageName(dockerImageName);
        });
        configureTempto(builder, configDir);
    }

    @Override
    public List<EnvironmentExtender> getDependencies()
    {
        return ImmutableList.of(hadoop);
    }
}
