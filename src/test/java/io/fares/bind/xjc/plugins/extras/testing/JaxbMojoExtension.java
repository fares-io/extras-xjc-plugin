/*
 * Copyright 2019 Niels Bertram
 *
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

package io.fares.bind.xjc.plugins.extras.testing;

import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.jvnet.jaxb.maven.XJCMojo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.function.Consumer;

public class JaxbMojoExtension implements BeforeAllCallback {

  private static final Logger log = LoggerFactory.getLogger(JaxbMojoExtension.class);

  private final Path resourcePath;

  private final Path generatePath;

  private final XJCMojo mojo;

  public JaxbMojoExtension(Path resourcePath, Path generatePath, XJCMojo mojo) {
    this.resourcePath = resourcePath;
    this.generatePath = generatePath;
    this.mojo = mojo;
  }

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {

    // construct schema and generate paths

    if (context.getTestClass().isPresent()) {

      String testName = context.getTestClass().get().getSimpleName();

      // configure the schema location
      Path testSchemaPath = Paths.get("schemas", testName);
      Path schemaPath = resourcePath.resolve(testSchemaPath);
      mojo.setSchemaDirectory(schemaPath.toFile());

      // configure the generated output location
      Path targetPath = generatePath.resolve(testName);
      mojo.setGenerateDirectory(targetPath.toFile());

    } else {

      Path schemaPath = resourcePath.resolve("schemas");
      mojo.setSchemaDirectory(schemaPath.toFile());

      Path targetPath = generatePath.resolve("xjc");
      mojo.setGenerateDirectory(targetPath.toFile());

    }

    // region clean output target

    File generateDirectory = mojo.getGenerateDirectory();

    if (generateDirectory.exists()) {

      Files.walkFileTree(generateDirectory.toPath(), new DirectoryCleaner());

      if (log.isDebugEnabled()) {
        log.debug("cleaned {}", generateDirectory.getAbsolutePath());
      }

    }

    // endregion

    mojo.execute();

  }

  public Path getGeneratedPath() {
    return mojo.getGenerateDirectory().toPath();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private boolean enableExtension = true;

    private boolean verbose = false;

    private boolean debug = false;

    private String resourcesLocation = "src/test/resources";

    private String generatedLocation = "target/generated-test-sources";

    private LinkedList<String> args = new LinkedList<>();

    public Builder verbose() {
      this.verbose = true;
      return this;
    }

    public Builder debug() {
      this.debug = true;
      return this;
    }

    public Builder disableExtension() {
      this.enableExtension = false;
      return this;
    }

    /**
     * Override where the resources will be placed in relative to {@link #baseDir()}.
     *
     * @param generatedLocation the location path
     * @return this builder
     */
    public Builder generatedLocation(String generatedLocation) {
      this.generatedLocation = generatedLocation;
      return this;
    }

    /**
     * Add XJC compiler arguments
     *
     * @param args the XJC compiler arguments to add
     * @return this builder
     */
    public Builder arg(String... args) {
      if (args != null) {
        this.args.addAll(Arrays.asList(args));
      }
      return this;
    }

    public JaxbMojoExtension build() {
      return build(null);
    }

    public JaxbMojoExtension build(Consumer<XJCMojo> mojoConfigurer) {

      XJCMojo mojo = new XJCMojo();

      mojo.setProject(new MavenProject());

      // enable extension unless disabled
      mojo.setExtension(enableExtension);

      // configure the arg switches
      args.remove("-extension");

      mojo.setArgs(args);

      // set verbose and debug levels
      mojo.setVerbose(verbose);
      mojo.setDebug(debug);

      if (mojoConfigurer != null) {
        mojoConfigurer.accept(mojo);
      }

      // region base directories for mojo execution

      Path basedir = baseDir();

      if (log.isDebugEnabled()) {
        log.debug("executing jaxb mojo in working directory {}", basedir.toAbsolutePath());
      }

      Path resourcePath = basedir.resolve(resourcesLocation);

      Path generatePath = basedir.resolve(generatedLocation);

      // endregion

      return new JaxbMojoExtension(resourcePath, generatePath, mojo);

    }

    Path baseDir() {
      return Paths.get("").toAbsolutePath();
    }

  }

  private static final class DirectoryCleaner extends SimpleFileVisitor<Path> {

    @Override
    public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
      Files.delete(path);
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
      if (exc != null) {
        throw exc;
      }
      Files.delete(dir);
      return FileVisitResult.CONTINUE;
    }

  }

}
