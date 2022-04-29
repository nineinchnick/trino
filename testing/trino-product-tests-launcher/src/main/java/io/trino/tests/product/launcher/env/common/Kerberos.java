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
import io.trino.tests.product.launcher.env.DockerContainer;
import io.trino.tests.product.launcher.env.Environment;
import io.trino.tests.product.launcher.env.EnvironmentConfig;
import io.trino.tests.product.launcher.testcontainers.PortBinder;
import org.testcontainers.containers.startupcheck.IsRunningStartupCheckStrategy;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;

import javax.inject.Inject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static io.trino.tests.product.launcher.docker.ContainerUtil.forSelectedPorts;
import static java.util.Objects.requireNonNull;
import static org.testcontainers.containers.wait.strategy.Wait.forLogMessage;

public class Kerberos
        implements EnvironmentExtender
{
    private static final int KERBEROS_PORT = 88;
    private static final int KERBEROS_ADMIN_PORT = 89;

    public static final String KERBEROS = "kerberos";

    public static final WaitStrategy DEFAULT_WAIT_STRATEGY = new WaitAllStrategy()
            .withStrategy(forSelectedPorts(KERBEROS_PORT, KERBEROS_ADMIN_PORT))
            .withStrategy(forLogMessage(".*krb5kdc entered RUNNING state.*", 1));

    private final PortBinder portBinder;
    private final String imagesVersion;

    @Inject
    public Kerberos(EnvironmentConfig environmentConfig, PortBinder portBinder)
    {
        this.portBinder = requireNonNull(portBinder, "portBinder is null");
        imagesVersion = requireNonNull(environmentConfig, "environmentConfig is null").getImagesVersion();
    }

    @Override
    @SuppressWarnings("resource")
    public void extendEnvironment(Environment.Builder builder)
    {
        DockerContainer container = new DockerContainer("ghcr.io/trinodb/testing/kerberos:" + imagesVersion, KERBEROS)
                .withStartupCheckStrategy(new IsRunningStartupCheckStrategy())
                .withCreateContainerCmdModifier(cmd -> {
                    // Add previously exposed ports and UDP port
                    List<ExposedPort> exposedPorts = new ArrayList<>();
                    if (cmd.getExposedPorts() != null) {
                        exposedPorts.addAll(Arrays.asList(cmd.getExposedPorts()));
                    }
                    exposedPorts.add(ExposedPort.udp(KERBEROS_PORT));
                    exposedPorts.add(ExposedPort.udp(KERBEROS_ADMIN_PORT));
                    cmd.withExposedPorts(exposedPorts);
                })
                .waitingFor(DEFAULT_WAIT_STRATEGY);

        portBinder.exposePort(container, KERBEROS_PORT);
        portBinder.exposePort(container, KERBEROS_ADMIN_PORT);

        builder.addContainer(container);
    }
}
