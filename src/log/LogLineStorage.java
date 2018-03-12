package log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import log.Log.LogLevel;

public class LogLineStorage implements Comparable<LogLineStorage> {
		private String toLog;
		private LogLevel lvl;
		private long time;
		private String who;
		private String threadName;

		public LogLineStorage(String toLog, LogLevel lvl, long time, String who, String threadName) {
			this.toLog = toLog;
			this.lvl = lvl;
			this.time = time;
			this.who = who;
			this.threadName = threadName;
		}

		public LogLineStorage(String exceptionAsString, LogLevel stacktrace, long time) {
			this.toLog = exceptionAsString;
			this.lvl = stacktrace;
			this.time = time;
		}
		
		public static LogLineStorage create(String toLog, LogLevel lvl, String context) {
			return new LogLineStorage(toLog, lvl, System.currentTimeMillis(), context, Thread.currentThread().getName());
		}

		public String toWrite() {
			DateFormat df = new SimpleDateFormat("y-M-d HH:mm:ss.SSS");
			String timeOutput = df.format(time);
			return "[" + timeOutput + "] " + lvl + "\t" + who + "@" + threadName +" :: " + toLog;
		}

		@Override
		public int compareTo(LogLineStorage lls) {
			long timeDiff = this.time = lls.time;
			if (timeDiff == 0)
				return 0;
			if (timeDiff > 0)
				return 1;
			return -1;
		}
	}