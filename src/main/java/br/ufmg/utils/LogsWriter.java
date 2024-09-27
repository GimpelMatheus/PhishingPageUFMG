package br.ufmg.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class LogsWriter {

    private final ArrayList<String> fileNameSuffixes = new ArrayList<String>(Arrays.asList(
            "access_log", "cadeia_urls", "firefox_exception",
            "http", "http_exception", "source_page",
            "tcp", "time_urls"));

    private Path logDirPath;
    private int numberOfThreads;
    private String currentDateForFilenamePrefix;
    private String localhostNameSuffix;
    private Date currentDate;

    // One log file per thread
    private ArrayList<Path> accessLogFilePaths;
    private ArrayList<FileWriter> accessLogFileWriter;

    private ArrayList<Path> cadeiaURLsFilePaths;
    private ArrayList<FileWriter> cadeiaURLsFileWriter;

    private ArrayList<Path> firefoxExceptionFilePaths;
    private ArrayList<FileWriter> firefoxExceptionFileWriter;

    private ArrayList<Path> httpFilePaths;
    private ArrayList<FileWriter> httpFileWriter;

    private ArrayList<Path> httpExceptionFilePaths;
    private ArrayList<FileWriter> httpExceptionFileWriter;

    private ArrayList<Path> sourcePageFilePaths;
    private ArrayList<FileWriter> sourcePageFileWriter;

    private ArrayList<Path> tcpFilePaths;
    private ArrayList<FileWriter> tcpFileWriter;

    private ArrayList<Path> timeURLsFilePaths;
    private ArrayList<FileWriter> timeURLsFileWriter;

    public LogsWriter(Path logsDirPath, int numberOfThreads) {
        this.logDirPath = logsDirPath;
        this.numberOfThreads = numberOfThreads;
        this.currentDate = new Date();
        this.startFilenamePrefix();
        this.startFilenameSuffix();
        this.startLogFilePathsForMultipleFiles();
    }

    public Path getLogDirPath() {
        return this.logDirPath;
    }

    public String getFormatedDate() {
        return this.currentDateForFilenamePrefix;
    }

    private void startFilenamePrefix() {
        SimpleDateFormat timestamp = new SimpleDateFormat("yyyyMMddHHmmss");
        this.currentDateForFilenamePrefix = timestamp.format(this.currentDate);
    }

    private void startFilenameSuffix() {
        try {
            InetAddress ip = InetAddress.getLocalHost();
            this.localhostNameSuffix = ip.getHostName();
        } catch (UnknownHostException exc) {
            this.localhostNameSuffix = "";
        }
    }

    public String getStandardFileNameFromSuffix(String fileNameSuffix) {
        return this.currentDateForFilenamePrefix + "." + fileNameSuffix + "." + this.localhostNameSuffix;
    }

    private ArrayList<Path> generateListOfPaths(String fileNameSuffix) {
        ArrayList<Path> result = new ArrayList<Path>();
        for (int i = 0; i < this.numberOfThreads; i++) {
            result.add(
                    this.logDirPath
                            .resolve(this.getStandardFileNameFromSuffix(fileNameSuffix) +
                                    "_" + Integer.toString(i))
                            .toAbsolutePath());
        }
        return result;
    }

    private void startLogFilePathsForMultipleFiles() {
        this.accessLogFilePaths = generateListOfPaths(this.fileNameSuffixes.get(0));
        this.cadeiaURLsFilePaths = generateListOfPaths(this.fileNameSuffixes.get(1));
        this.firefoxExceptionFilePaths = generateListOfPaths(this.fileNameSuffixes.get(2));
        this.httpFilePaths = generateListOfPaths(this.fileNameSuffixes.get(3));
        this.httpExceptionFilePaths = generateListOfPaths(this.fileNameSuffixes.get(4));
        this.sourcePageFilePaths = generateListOfPaths(this.fileNameSuffixes.get(5));
        this.tcpFilePaths = generateListOfPaths(this.fileNameSuffixes.get(6));
        this.timeURLsFilePaths = generateListOfPaths(this.fileNameSuffixes.get(7));
    }

    private ArrayList<FileWriter> createListOfFileWriters(ArrayList<Path> listOfPaths, boolean autoFlush)
            throws FileNotFoundException, UnsupportedEncodingException {
        ArrayList<FileWriter> result = new ArrayList<FileWriter>();
        for (int i = 0; i < this.numberOfThreads; i++) {
            result.add(new FileWriter(listOfPaths.get(i), autoFlush));
        }
        return result;
    }

    public void createFiles() throws FileNotFoundException, UnsupportedEncodingException, IOException {
        this.logDirPath.toFile().mkdirs();

        // Write the start time in a file.
        SimpleDateFormat dataInicio = new SimpleDateFormat("dd-M-yyyy hh:mm:ss");
        String dataFormatada = dataInicio.format(this.currentDate);
        String inicio = "Inicio em " + dataFormatada + "\n";
        File startTimeFile = this.logDirPath.resolve(this.getStandardFileNameFromSuffix("inicio")).toFile();
        try {
            if(!startTimeFile.exists()) {
                startTimeFile.createNewFile();
            }
            Files.write(startTimeFile.toPath(), inicio.getBytes());
        } catch (IOException e) {
            System.out.println(e.getMessage());
            System.out.println(startTimeFile.getPath());
            System.exit(-1);
        }

        this.accessLogFileWriter = this.createListOfFileWriters(this.accessLogFilePaths, true);
        this.cadeiaURLsFileWriter = this.createListOfFileWriters(this.cadeiaURLsFilePaths, false);
        this.firefoxExceptionFileWriter = this.createListOfFileWriters(this.firefoxExceptionFilePaths, false);
        this.httpFileWriter = this.createListOfFileWriters(this.httpFilePaths, false);
        this.httpExceptionFileWriter = this.createListOfFileWriters(this.httpExceptionFilePaths, false);
        this.sourcePageFileWriter = this.createListOfFileWriters(this.sourcePageFilePaths, false);
        this.tcpFileWriter = this.createListOfFileWriters(this.tcpFilePaths, false);
        this.timeURLsFileWriter = this.createListOfFileWriters(this.timeURLsFilePaths, false);
    }

    private void closeListOfFiles(ArrayList<FileWriter> listOfFileWriters) throws IOException {
        for (FileWriter file : listOfFileWriters) {
            file.close();
        }
    }

    public void closeFiles() throws IOException {
        this.closeListOfFiles(this.accessLogFileWriter);
        this.closeListOfFiles(this.cadeiaURLsFileWriter);
        this.closeListOfFiles(this.firefoxExceptionFileWriter);
        this.closeListOfFiles(this.httpFileWriter);
        this.closeListOfFiles(this.httpExceptionFileWriter);
        this.closeListOfFiles(this.sourcePageFileWriter);
        this.closeListOfFiles(this.tcpFileWriter);
        this.closeListOfFiles(this.timeURLsFileWriter);
    }

    public void writeAccessLog(int threadIndex, String text) throws IOException {
        if (threadIndex >= this.numberOfThreads) {
            throw new IOException(
                    "Thread ID " + Integer.toString(threadIndex) + " does not exist, so the file also doesn't");
        }
        this.accessLogFileWriter.get(threadIndex).write(text);
    }

    public void writeCadeiaURLs(int threadIndex, String text) throws IOException {
        if (threadIndex >= this.numberOfThreads) {
            throw new IOException(
                    "Thread ID " + Integer.toString(threadIndex) + " does not exist, so the file also doesn't");
        }
        this.cadeiaURLsFileWriter.get(threadIndex).write(text);
    }

    public void writeFirefoxException(int threadIndex, String text) throws IOException {
        if (threadIndex >= this.numberOfThreads) {
            throw new IOException(
                    "Thread ID " + Integer.toString(threadIndex) + " does not exist, so the file also doesn't");
        }
        this.firefoxExceptionFileWriter.get(threadIndex).write(text);
    }

    public void writeHttp(int threadIndex, String text) throws IOException {
        if (threadIndex >= this.numberOfThreads) {
            throw new IOException(
                    "Thread ID " + Integer.toString(threadIndex) + " does not exist, so the file also doesn't");
        }
        this.httpFileWriter.get(threadIndex).write(text);
    }

    public void writeHttpException(int threadIndex, String text) throws IOException {
        if (threadIndex >= this.numberOfThreads) {
            throw new IOException(
                    "Thread ID " + Integer.toString(threadIndex) + " does not exist, so the file also doesn't");
        }
        this.httpExceptionFileWriter.get(threadIndex).write(text);
    }

    public void writeSourcePage(int threadIndex, String text) throws IOException {
        if (threadIndex >= this.numberOfThreads) {
            throw new IOException(
                    "Thread ID " + Integer.toString(threadIndex) + " does not exist, so the file also doesn't");
        }
        this.sourcePageFileWriter.get(threadIndex).write(text);
    }

    public void writeTcp(int threadIndex, String text) throws IOException {
        if (threadIndex >= this.numberOfThreads) {
            throw new IOException(
                    "Thread ID " + Integer.toString(threadIndex) + " does not exist, so the file also doesn't");
        }
        this.tcpFileWriter.get(threadIndex).write(text);
    }

    public void writeTimeURLs(int threadIndex, String text) throws IOException {
        if (threadIndex >= this.numberOfThreads) {
            throw new IOException(
                    "Thread ID " + Integer.toString(threadIndex) + " does not exist, so the file also doesn't");
        }
        this.timeURLsFileWriter.get(threadIndex).write(text);
    }

}