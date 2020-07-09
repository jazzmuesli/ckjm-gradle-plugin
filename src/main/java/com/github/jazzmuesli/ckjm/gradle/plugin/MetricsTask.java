package com.github.jazzmuesli.ckjm.gradle.plugin;

import org.apache.bcel.util.ClassPath;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskAction;


import gr.spinellis.ckjm.MetricsFilter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
			getLog().info("buildDir: " + project.getBuildDir());
//			Path path = Paths.get(projectDir, "sourceDirs.csv");
//			System.out.println("projectDir: " + projectDir + ", csvPath: " + path);
//			CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(path),
//					CSVFormat.DEFAULT.withHeader("sourceSetName", "dirName", "processed").withSystemRecordSeparator()
//							.withDelimiter(';'));
//
//			SourceSetContainer sourceSets = project.getConvention().getPlugin(JavaPluginConvention.class)
//					.getSourceSets();
//
//			for (Map.Entry<String, SourceSet> entry : sourceSets.getAsMap().entrySet()) {
//				SourceSet sourceSet = entry.getValue();
//				Set<File> srcDirs = sourceSet.getAllJava().getSrcDirs();
//				System.out.println("Processing sourceSet " + entry.getKey() + " with srcDirs=" + srcDirs);
//				for (File srcDir : srcDirs) {
//					boolean processed = processSourceDirectory(srcDir.getAbsolutePath());
//					printer.printRecord(entry.getKey(), srcDir.getAbsolutePath(), processed);
//					printer.flush();
//				}
//			}
//			printer.close();
		} catch (Exception e) {
			getLog().error(e.getMessage(), e);
		}
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
				
				CSVCkjmOutputHandler outputPlain = new CSVCkjmOutputHandler(dirName + "/ckjm.csv");
				MetricsFilter.runMetrics(cp, classNames.toArray(new String[0]), outputPlain, true);
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
