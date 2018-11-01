package com.model;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.text.DateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JobLogger {
	private static boolean logToFile;
	private static boolean logToConsole;
	private static boolean logMessage;
	private static boolean logWarning;
	private static boolean logError;
	private static boolean logToDatabase;
	private static Map<Integer, String> dbParams;
	private static Logger logger;

	public JobLogger(boolean logToFileParam, boolean logToConsoleParam, boolean logToDatabaseParam,
			boolean logMessageParam, boolean logWarningParam, boolean logErrorParam, Map<Integer, String> dbParamsMap) {

	}

	public static void logMessage(String messageText, boolean message, boolean warning, boolean error)
			throws Exception {
		messageText = messageText.trim();
		if (messageText == null || messageText.length() == 0) {
			return;
		}
		if (!logToConsole && !logToFile && !logToDatabase) {
			throw new IllegalArgumentException("Invalid configuration");
		}
		if ((!logError && !logMessage && !logWarning) || (!message && !warning && !error)) {
			throw new IllegalArgumentException("Error or Warning or Message must be specified");
		}

		Properties connectionProps = new Properties();
		connectionProps.put("user", dbParams.get("userName"));
		connectionProps.put("password", dbParams.get("password"));

		try (Connection connection = DriverManager.getConnection("jdbc:" + dbParams.get("dbms") + "://"
				+ dbParams.get("serverName") + ":" + dbParams.get("portNumber") + "/", connectionProps)) {

			int t = 0;
			if (message && logMessage) {
				t = 1;
			} else if (error && logError) {
				t = 2;
			} else if (warning && logWarning) {
				t = 3;
			}

			String l = null;
			File logFile = new File(dbParams.get("logFileFolder") + "/logFile.txt");
			if (!logFile.exists()) {
				try {
					logFile.createNewFile();
				} catch (IOException e) {
					throw new IOException("Error while creating new file");
				}
			}

			FileHandler fh = new FileHandler(dbParams.get("logFileFolder") + "/logFile.txt");
			ConsoleHandler ch = new ConsoleHandler();

			if (error && logError) {
				l = l + "error " + DateFormat.getDateInstance(DateFormat.LONG).format(new Date()) + messageText;
			} else if (warning && logWarning) {
				l = l + "warning " + DateFormat.getDateInstance(DateFormat.LONG).format(new Date()) + messageText;
			} else if (message && logMessage) {
				l = l + "message " + DateFormat.getDateInstance(DateFormat.LONG).format(new Date()) + messageText;
			} else if (logToFile) {
				logger.addHandler(fh);
				logger.log(Level.INFO, messageText);
			} else if (logToConsole) {
				logger.addHandler(ch);
				logger.log(Level.INFO, messageText);
			} else if (logToDatabase) {

				String sql = "insert into Log_Values values(?,?)";

				try (PreparedStatement stmt = connection.prepareStatement(sql)) {
					stmt.setBoolean(1, message);
					stmt.setInt(2, t);

					stmt.execute();
				}
			}

		}
	}
}
