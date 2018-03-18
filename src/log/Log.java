package log;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;

public class Log {

	private LogWriter lw;
	private String context;

	private Log(String context) {
		this.context = context;
		lw = LogWriter.getInstance();
	}
	
	public static void startLog(File logFile, boolean append, boolean write) {
		LogWriter.setLogFile(logFile);
		LogWriter.setAppend(append);
		LogWriter.setWrite(write);
		LogWriter lw = LogWriter.getInstance();
		if (lw != null)
			new Thread(lw).start();
	}
	
	public static Log getLogger(String who) {
		return new Log(who);
	}

	public void debug(String toLog) {
		LogLineStorage lls = LogLineStorage.create(toLog, LogLevel.DEBUG, context);
		addToLog(lls);
	}

	public void error(String toLog) {
		LogLineStorage lls = LogLineStorage.create(toLog, LogLevel.ERROR, context);
		addToLog(lls);
	}

	public void info(String toLog) {
		LogLineStorage lls = LogLineStorage.create(toLog, LogLevel.INFO, context);
		addToLog(lls);
	}

	public void trace(String toLog) {
		LogLineStorage lls = LogLineStorage.create(toLog, LogLevel.TRACE, context);
		addToLog(lls);
	}

	public void exception(Exception e) {
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		String exceptionAsString = sw.toString();
//		LogLineStorage lls = new LogLineStorage(exceptionAsString, LogLevel.EXCEPTION, System.currentTimeMillis());
		LogLineStorage lls = LogLineStorage.create(exceptionAsString, LogLevel.EXCEPTION, context);
		addToLog(lls);
	}

	private void addToLog(LogLineStorage lls) {
		if (lw != null)
			lw.add(lls);
	}
	
	public void stop() {
		lw.stop();
	}

	// private void writeToLog(String toLog,LogLevel lvl, long
	// currentTime,String who){
	// if(logFile == null)
	// return;
	//
	// Date time = new Date(currentTime);
	//
	// String output = "["+time+"] "+who+" "+lvl +" # "+toLog;
	//
	// try {
	// FileUtil.writeToFile(output, logFile);
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	// }

	// Log helper classes

	protected enum LogLevel {
		// Severity lowest down to highest
		ERROR, INFO, DEBUG, EXCEPTION, TRACE
	}

	

}
