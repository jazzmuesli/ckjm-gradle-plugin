package com.github.jazzmuesli.ckjm.gradle.plugin;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.bcel.util.ClassPath;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.FileCollection;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.compile.JavaCompile;

import gr.spinellis.ckjm.MetricsFilter;

public class MetricsTask extends DefaultTask {

	private Project project;

	public void setProject(Project project) {
		this.project = project;
	}
	
	public Logger getLog() {
		return super.getLogger();
	}

	@TaskAction
	void processDirectories() {
		try {
			String projectDir = project.getProjectDir().getAbsolutePath();
			System.out.println("projectDir: " + projectDir);
			System.out.println("buildDir: " + project.getBuildDir());
			ClassPath cp = new ClassPath();
			System.out.println("classpath: " + cp);

			List<String> skipConfigurations = Arrays.asList("apiElements", "implementation", "runtimeElements", "runtimeOnly", "testImplementation", "testRuntimeOnly");
			Set<String> depElements = new LinkedHashSet<>();
			for (Configuration element : project.getConfigurations()) {
				if (skipConfigurations.contains(element.getName())) {
					getLog().info("Skipping " + element);
					continue;
				}
				try {
					Set<File> filesSet = element.resolve();
					for (File file : filesSet) {
						System.out.println(file.getName());
						depElements.add(file.getAbsolutePath());
					}
				} catch (IllegalStateException e) {
					getLog().warn("Can't resolve " + element + " for projectDir=" + projectDir + " due to " + e.getMessage() + ", it's fine.");
				}
			}
			getLog().info("Found " + depElements.size() + " dependencies");

			for (String depFname : depElements) {
				cp = new ClassPath(cp, depFname);
			}

			Set<String> mainOutputs = getOutput("compileJava");
			for (String f : mainOutputs) {
				cp = new ClassPath(cp, f);
			}
			Set<String> testOutputs = getOutput("compileTestJava");
			for (String f : testOutputs) {
				cp = new ClassPath(cp, f);
			}
			
	        final ClassPath fcp = cp;
	        mainOutputs.forEach(dir -> {
	        	System.out.println("MainDir: " + dir);
	        	processSourceDirectory(fcp, dir);
	        });
			
	        testOutputs.forEach(dir -> {
	        	System.out.println("testDir: " + dir);
	        	processSourceDirectory(fcp, dir);
	        });
			
		} catch (Exception e) {
			getLog().error(e.getMessage(), e);
		}
	}

	protected Set<String> getOutput(String task) {
		Set<String> outputs = getProject().getTasks().getByName(task).getOutputs().getFiles().getFiles()
				.stream().filter(File::exists).map(File::getAbsolutePath).collect(Collectors.toSet());
		return outputs;
	}

	protected void processSourceDirectory(ClassPath cp, String dirName) {
		try {
			getLog().info("Processing " + dirName);
			if (new File(dirName).exists()) {
				List<String> files = Files.walk(Paths.get(dirName)).filter(p -> p.toFile().getName().endsWith(".class"))
						.map(s -> s.toFile().getAbsolutePath()).collect(Collectors.toList());
				List<String> classNames = files.stream().map(f-> getClassNameFromFile(dirName, f)).collect(Collectors.toList());
				getLog().info("Found " + files.size() + " files in " + dirName);
				File ckjmFilesFile = new File(dirName).toPath().resolve("ckjm-files.txt").toFile();
				getLog().info("Writing " + files.size() + " files to " + ckjmFilesFile);
				Files.write(ckjmFilesFile.toPath(), files, StandardOpenOption.CREATE);
				
				File ckjmClassesFile = new File(dirName).toPath().resolve("ckjm-classes.txt").toFile();
				getLog().info("Writing " + classNames.size() + " classes to " + ckjmClassesFile);
				Files.write(ckjmClassesFile.toPath(), classNames, StandardOpenOption.CREATE);

				File ckjmCpFile = new File(dirName).toPath().resolve("ckjm-cp.txt").toFile();
				getLog().info("Writing " + cp.toString()+ " CP to " + ckjmCpFile);
				Files.write(ckjmCpFile.toPath(), Arrays.asList(cp.toString().split(File.pathSeparator)), StandardOpenOption.CREATE);

				CSVCkjmOutputHandler outputHandler = new CSVCkjmOutputHandler(dirName + "/ckjm.csv");
				for (String className: classNames) {
					getLog().info("Processing " + className);
					MetricsFilter.runMetrics(cp, new String[] {className}, outputHandler, true);
				}
				getLog().info("Recorded " + outputHandler.getRecords() + " out of " + classNames.size() + " classes from " + dirName + " in ckjm");

			}
		} catch (Exception e) {
			getLog().error(e.getMessage(), e);
		}
	}

	/**
	 * convert
	 * /private/tmp/jfreechart/target/test-classes/org/jfree/chart/renderer/category/LevelRendererTest.class
	 * to
	 * org.jfree.chart.renderer.category.LevelRendererTest
	 * @param dirName /private/tmp/jfreechart/target/test-classes
	 * @param f /private/tmp/jfreechart/target/test-classes/org/jfree/chart/renderer/category/LevelRendererTest.class
	 * @return org.jfree.chart.renderer.category.LevelRendererTest
	 */
	protected String getClassNameFromFile(String dirName, String f) {
		return new File(f).getAbsolutePath().replaceAll(dirName, "").replace("/", ".").replaceAll(".class$", "").replaceAll("^\\.", "");
	}
}
