import com.github.jazzmuesli.ckjm.gradle.plugin.CSVCkjmOutputHandler;
import gr.spinellis.ckjm.MetricsFilter;
import org.apache.bcel.util.ClassPath;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class MyTest {


    @Ignore
    @Test
    public void test() throws IOException {

        String dirName = "/Users/preich/Documents/github/poi/build/scratchpad/build/classes/java/main";

        String cpFilename = dirName + "/ckjm-cp.txt";
        List<String> cpLines = Files.readAllLines(new File(cpFilename).toPath());
        String[] cpFiles= cpLines.toArray(new String[0]);
        ClassPath cp = new ClassPath(String.join(File.pathSeparator, cpLines));
        String classesFilename = dirName+"/ckjm-classes.txt";
        List<String> classes = Files.readAllLines(new File(classesFilename).toPath());
        String[] classNames = classes.toArray(new String[0]);

        CSVCkjmOutputHandler outputHandler = new CSVCkjmOutputHandler(dirName + "/ckjm.csv");
        for (String className: classNames) {
            System.out.println("Processing " + className);
            MetricsFilter.runMetrics(cp, new String[] {className}, outputHandler, true);
        }
        System.out.println("Recorded " + outputHandler.getRecords() + " out of " + classNames.length + " classes from " + dirName + " in ckjm");

    }
}
