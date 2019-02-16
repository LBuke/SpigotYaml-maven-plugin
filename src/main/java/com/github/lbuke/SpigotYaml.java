package com.github.lbuke;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

@Mojo(name = "SpigotYaml", defaultPhase = LifecyclePhase.COMPILE, threadSafe = true)
public class SpigotYaml extends AbstractMojo {

    private static final Pattern JPLUGIN_PATTERN = Pattern.compile(".*class \\w+ extends JavaPlugin.*");
    private static final Pattern ABSTRACT_PATTERN = Pattern.compile(".*abstract class.*");

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        getLog().info("# Spigot Yaml Maven Plugin");

        this.getJavaPlugin(obj -> {
            getLog().info("main: " + obj);
            getLog().info("name: " + project.getName());
            getLog().info("version: " + project.getVersion());
            getLog().info("description: " + project.getDescription());
        });
    }

    private void getJavaPlugin(Callback<String> callable) {
        new FileScan(callable).run();
    }

    private class FileScan {

        private Callback<String> callable;

        public FileScan(Callback<String> callable) {
            this.callable = callable;
        }

        public void run() {
            for (File file : Objects.requireNonNull(project.getBasedir().listFiles())) {
                if (file == null)
                    continue;

                boolean done = scan(file);
                if (done) break;
            }
        }

        public boolean scan(File dir) {
            if (!dir.isDirectory()) {
                if (dir.getName().contains(".java"))
                    return check(dir);
                return false;
            }

            for (File file : Objects.requireNonNull(dir.listFiles())) {
                if (file == null)
                    continue;

                if (file.isDirectory()) {
                    scan(file);
                    continue;
                }

                if (!file.getName().contains(".java"))
                    continue;

                boolean b = check(file);
                if (b) {
                    return true;
                }
            }

            return false;
        }

        public boolean check(File file) {
            try {
                Scanner scanner = new Scanner(file);

                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    if(JPLUGIN_PATTERN.matcher(line).find() && !ABSTRACT_PATTERN.matcher(line).find()) {
                        String main = file.getAbsolutePath();
                        main = main.replaceAll("\\u005C", ".");
                        main = main.replaceAll("/", ".");

                        if (main.contains("src.main.java."))
                            main = main.split("src.main.java.")[1];

                        if (main.contains("src.java."))
                            main = main.split("src.java.")[1];

                        if (main.contains("src.main."))
                            main = main.split("src.main.")[1];

                        if (main.contains("src."))
                            main = main.split("src.")[1];

                        main = main.replaceAll("\\.java", "");

                        callable.call(main);
                        return true;
                    }
                }
            } catch(FileNotFoundException e) {
                //handle this
            }

            return false;
        }
    }
}
