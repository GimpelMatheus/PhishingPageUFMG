package br.ufmg.app;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {
	private static final Logger LOGGER = LogManager.getLogger();

	public static void main(String[] args) {
		
		if (args.length > 1) {
			LOGGER.error("The only required parameter is the configuration filepath.");
			System.exit(-1);
		} else if (args.length < 1) {
			LOGGER.error("Required execution argument: path of the json configuration file.");
			System.exit(-1);
		}

		Configuration config = new Configuration(args[0]);
		LOGGER.info(config);

		App app = new App(config);

		try {
			LOGGER.info("Starting running the application.\n");
			app.run();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		System.exit(0);
	}
}
