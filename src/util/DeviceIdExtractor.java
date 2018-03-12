package util;

import java.io.File;
import java.io.IOException;

public class DeviceIdExtractor {
	/**
	 * Extracts from Folder
	 * 
	 * @param parent
	 *            - the parent folder
	 * @param folder
	 *            - the folder from which to extract
	 * @return the deviceId
	 * @throws IOException
	 */
	public static String extractFromFolder(File parent, File folder) throws IOException {
		String fileSep = "\\" + File.separator;
		String[] parentPath = parent.getAbsolutePath().split(fileSep);
		String[] folderPath = folder.getAbsolutePath().split(fileSep);
		String output = folderPath[parentPath.length];

		return output;
	}
}
