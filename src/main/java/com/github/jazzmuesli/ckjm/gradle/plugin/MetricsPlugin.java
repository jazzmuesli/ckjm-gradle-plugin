package com.github.jazzmuesli.ckjm.gradle.plugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class MetricsPlugin implements Plugin<Project> {
    public void apply(Project project) {
        project.getTasks().create("metrics", MetricsTask.class, (task) -> { 
            task.setProject(project);
        });
    }
}
