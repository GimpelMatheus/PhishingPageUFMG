package br.ufmg.app;

import java.nio.charset.Charset;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;

import java.util.concurrent.*;
import java.util.function.Predicate;

import br.ufmg.utils.*;

import java.util.concurrent.atomic.AtomicBoolean;

public class App {

	private Configuration config;
	private LogsWriter logsWriter;
	private URLList whiteList;
	private URLList blackList;
	private File[] urlFiles;
	private BlockingQueue<String> urlsList;
	private AtomicBoolean restartProcesses;
	private AtomicBoolean killProcesses;

	/* Inicialização de variáveis. */
	public App(Configuration config) { // int instancias, int timeout, int limite_requisicoes, Path repository, Path
										// whiteList, Path blackList, Path logsDir) {
		try {
			this.whiteList = new URLList(config.getWhiteListPath());
			this.blackList = new URLList(config.getBlackListPath());
		} catch (Exception e) {
			e.printStackTrace();
		}
		this.config = config;
		urlsList = new LinkedBlockingDeque<String>();
		restartProcesses = new AtomicBoolean(false);
		killProcesses = new AtomicBoolean(false);
	}

	public void run() throws FileNotFoundException, UnsupportedEncodingException, IOException {
		this.startLogFiles();
		this.singletonSetup();
		this.getFiles();
		this.getURLs();
		this.manageProcesses();
	}

	private void startLogFiles() throws FileNotFoundException, UnsupportedEncodingException, IOException {
		this.logsWriter = new LogsWriter(config.getLogsDirPath(), config.getConcurrentBrowserInstancesNumber());
		this.logsWriter.createFiles();
	}

	private void singletonSetup() {
		Singleton.getInstance().setParameters(this.config.getWindowTimeout(), this.logsWriter);
	}

	/* Função que realiza a leitura de arquivos. */
	private void getFiles() {
		File repo = this.config.getRepositoryPath().toFile();
		if (repo.isDirectory()) {
			urlFiles = repo.listFiles();
			Arrays.sort(urlFiles, Comparator.comparingLong(File::lastModified));
		} else {
			System.err.println("[ERROR] Inexistent URLs recip " + this.config.getRepositoryPath().toString());
			System.exit(-1);
		}
		if (urlFiles.length == 0) {
			System.exit(0);
		}
	}

	/* Função que realiza a leitura de URLs. */
	private void getURLs() {
		Charset charset = Charset.forName("UTF-8");
		for (File file : urlFiles) {
			try {
				List<String> lines = Files.readAllLines(file.toPath(), charset);
				for (String line : lines) {
					urlsList.add(line);
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (NullPointerException e) {
				e.printStackTrace();
			}
		}
	}

	/*
	 * Função que determina se a aplicação deve parar, realizando
	 * a leitura de um arquivo na pasta shellscripts/sys/operante.
	 */
	public boolean isAppRunning() {
		File runtimeFile = config.getRuntimeControllersPath().resolve("running").toFile();
		if (!runtimeFile.exists()) {
			System.err.println("[ERROR] The runtime file " + runtimeFile.toString() + " does not exists.");
			System.exit(-1);
		}
		try {
			BufferedReader br = new BufferedReader(new FileReader(runtimeFile.toString()));
			char isRunningBoolean = (char) br.read();
			br.close();
			return (isRunningBoolean != '0');
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (IOException e) {
			System.err.println("[WARNING] Something has gone wrong with" + runtimeFile.toString()
					+ ". So, the app keeps running.");
		}
		return true;
	}

	/* Função principal. Administa o multithreading */
	private void manageProcesses() {
		SimpleDateFormat startDate = new SimpleDateFormat("dd-M-yyyy hh:mm:ss");

		Date date = new Date();
		String formatedDate = startDate.format(date);
		String startDateStr = "Started at " + formatedDate + "\n";
		try {
			Files.write(Paths.get(this.config.getLogsDirPath().toString(),
					this.logsWriter.getStandardFileNameFromSuffix("inicio")), startDateStr.getBytes());
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		MemoryMonitor memoryMonitor = new MemoryMonitor(restartProcesses);
		Thread monitor = new Thread(memoryMonitor);
		monitor.start();

		List<Thread> threadsList = new LinkedList<Thread>();

		Predicate<Thread> isDead = t -> !t.isAlive();

		long startTime = System.nanoTime();
		int index = 0;

		while (isAppRunning()) {

			if (killProcesses.get()) {
				break;
			}

			if (restartProcesses.get()) {
				System.out.println("[INFO] Waiting...");
				for (Thread thread : threadsList) {
					try {
						thread.join(600000);
					} catch (InterruptedException e) {
						continue;
					}
				}
				try {
					Runtime.getRuntime().exec("pkill -9 firefox");
					Runtime.getRuntime().exec("pkill -9 geckodriver");
				} catch (IOException e) {
					e.printStackTrace();
				}
				restartProcesses.set(false);
			}
			threadsList.removeIf(isDead);
			if (threadsList.size() >= this.config.getConcurrentBrowserInstancesNumber()) {
				try {
					TimeUnit.SECONDS.sleep(1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} else {
				br.ufmg.utils.Process r = new br.ufmg.utils.Process(
						urlsList, killProcesses, restartProcesses,
						index, this.logsWriter, whiteList, blackList,
						this.config.getPageTimeout(),
						this.config.getMaxRequestNumber(),
						this.config.getGeckodriverBinPath().toString());
				Thread t = new Thread(r);
				threadsList.add(t);
				t.start();
				System.out.println("[INFO] Starting thread " + Integer.toString(index));
				index += 1;
			}
		}

		for (Thread thread : threadsList) {
			try {
				thread.join(1000);
				// thread.join(600000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		monitor.interrupt();
		writeRemainingURLs();
		System.gc();

		long finalTime = System.nanoTime();
		long spentTime = finalTime - startTime;
		String timeString = Long.toString(spentTime) + '\n';

		try {
			Files.write(this.config.getLogsDirPath().resolve(this.logsWriter.getStandardFileNameFromSuffix("time")),
					timeString.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void writeRemainingURLs() {
		try {
			FileWriter remaining = new FileWriter(this.config.getLogsDirPath()
					.resolve(this.logsWriter.getStandardFileNameFromSuffix("remaining_urls")), false);

			while (urlsList.isEmpty() == false) {
				try {
					String url = urlsList.take();
					remaining.write(url + "\n");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			remaining.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
