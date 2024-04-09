package de.dlr.ivf.urmo.router.spring.io.implementation;

import com.lmax.disruptor.EventHandler;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;

public class FileWritingEventHandler implements EventHandler<StringEvent> {

    private final String outputFolder;
    private final String prefix;
    private PrintWriter printWriter;
    private LocalDate localDate;

    public FileWritingEventHandler(String outputFolder, String prefix) throws IOException {
        this.localDate = LocalDate.now();
        this.outputFolder = outputFolder;
        this.prefix = prefix;

        Path outputPath = Paths.get(outputFolder);
        if(!Files.isDirectory(outputPath)){
            Files.createDirectory(outputPath);
        }

        Path outputFilePath = generateFilename(prefix, localDate);

        this.printWriter = newPrintWriter(outputFilePath);
    }
    @Override
    public void onEvent(StringEvent s, long l, boolean b) throws Exception {
        LocalDate now = LocalDate.now();
        if(now.isAfter(localDate)){
            this.printWriter = newPrintWriter(generateFilename(prefix, now));
            this.localDate = now;
        }
        this.printWriter.println(s.getValue());
    }

    private PrintWriter newPrintWriter(Path outputFilePath) throws IOException {

        if(this.printWriter != null){
            this.printWriter.flush();
            this.printWriter.close();
        }
        if(!Files.exists(outputFilePath)){
            Files.createFile(outputFilePath);

        }
        return new PrintWriter(new FileWriter(outputFilePath.toFile(),true),true);
    }

    private Path generateFilename(String prefix, LocalDate localDate){
        return Paths.get(outputFolder +"/"+localDate+"_"+prefix+".txt");
    }

    @Override
    public void onShutdown() {
        printWriter.flush();
        printWriter.close();
    }
}
