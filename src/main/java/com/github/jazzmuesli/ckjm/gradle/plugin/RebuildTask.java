package com.github.jazzmuesli.ckjm.gradle.plugin;

import gr.spinellis.ckjm.MetricsFilter;
import org.apache.bcel.util.ClassPath;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.OutputFiles;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class RebuildTask extends DefaultTask {

	private Project project;

	public void setProject(Project project) {
		this.project = project;
	}
	
	public Logger getLog() {
		return super.getLogger();
	}


	public static List<String> findFiles(String dir, Predicate<File> predicate) throws IOException {
		Path path = new File(dir).toPath();
		List<String> files = java.nio.file.Files.walk(path).filter(p -> predicate.test(p.toFile()))
				.map(f -> f.toFile().getAbsolutePath()).collect(Collectors.toList());
		return files;
	}

	@TaskAction
	void processDirectories() {
		try {
			String dirName = project.getProjectDir().getAbsolutePath();
			List<String> files = findFiles(dirName, p -> p.length() > 20 && p.getName().contains("ckjm-cp.txt"));
			List<String> dirs = files.stream().map(x -> new File(x).getParentFile().getAbsolutePath()).collect(Collectors.toList());
			for (String dir : dirs) {
				processDir(dir);
			}

		} catch (Exception e) {
			getLog().error(e.getMessage(), e);
		}
	}

	private boolean processDir(String dirName) throws IOException {
		getLog().info("dirName: " + dirName);
		String cpFilename = dirName + "/ckjm-cp.txt";
		File cpFile = new File(cpFilename);
		if (!cpFile.exists()) {
			getLog().info("File " + cpFilename + " doesn't exist");
			return true;
		}
		List<String> cpLines = Files.readAllLines(cpFile.toPath());
		ClassPath cp = new ClassPath(String.join(File.pathSeparator, cpLines));
		String classesFilename = dirName+"/ckjm-classes.txt";
		List<String> classes = Files.readAllLines(new File(classesFilename).toPath());
		String[] classNames = classes.toArray(new String[0]);

		CSVCkjmOutputHandler outputHandler = new CSVCkjmOutputHandler(dirName + "/ckjm.csv");
		for (String className: classNames) {
			getLog().info("Processing " + className);
			MetricsFilter.runMetrics(cp, new String[] {className}, outputHandler, true);
		}
		getLog().info("Recorded " + outputHandler.getRecords() + " out of " + classNames.length + " classes from " + dirName + " in ckjm");
		return false;
	}

}
