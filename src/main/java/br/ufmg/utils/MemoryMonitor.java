package br.ufmg.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class MemoryMonitor implements Runnable {

	private AtomicBoolean processesRestart;

	public MemoryMonitor(AtomicBoolean rp) {
		processesRestart = rp;
	}

	public void run() {
		int numberOfRestarts = 0;
		while (true) {
			try {
				TimeUnit.SECONDS.sleep(1);
			} catch (InterruptedException e1) {
				return;
			}

			java.lang.Process p;
			try {
				//rodar dentro de uma vm vamos ter a memória reservada
				p = Runtime.getRuntime().exec("free -t -m");
			} catch (IOException e) {
				e.printStackTrace();
				continue;
			}

			try {
				p.waitFor();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				continue;
			}

			BufferedReader buf = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line = "";
			String output = "";
			String tokens = "";

			try {
				while ((line = buf.readLine()) != null) {
					output += line + "\n";
				}
				tokens = output.split("\n")[1];
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				continue;
			}
			String[] outputList = tokens.split("\\s+");
			double memoryPercent = ((Double.parseDouble(outputList[1]) - Double.parseDouble(outputList[6]))
					/ Double.parseDouble(outputList[1])) * 100;
			if (memoryPercent > 70.0) {
				numberOfRestarts++;
				System.out.println("[INFO] Restarting process " + numberOfRestarts + "...");
				processesRestart.set(true);
				try {
					p = Runtime.getRuntime().exec("pkill -9 firefox");
					p.waitFor();
					p = Runtime.getRuntime().exec("pkill -9 geckodriver");
					p.waitFor();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}
