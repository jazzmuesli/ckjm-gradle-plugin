package com.github.jazzmuesli.ckjm.gradle.plugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

import java.io.File;
import java.util.Iterator;
import java.util.Set;

public class MetricsPlugin implements Plugin<Project> {
    public void apply(Project project) {
        project.getTasks().create("ckjm", MetricsTask.class, (task) -> {
            task.setProject(project);
        });
        project.getTasks().create("ckjmRebuild", RebuildTask.class, (task) -> {
            task.setProject(project);
        });
    }

}
