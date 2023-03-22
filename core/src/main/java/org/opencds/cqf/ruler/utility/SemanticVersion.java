package org.opencds.cqf.ruler.utility;

import java.util.List;
import java.util.Comparator;

public class SemanticVersion {
	public static Comparator<String> getVersionComparator() {
		return new VersionComparator();
	}

	public static String findHighestVersion(List<String> versions) {
		String highestVersion = null;
		Comparator<String> versionComparator = new VersionComparator();

		for (String version : versions) {
			if (highestVersion == null || versionComparator.compare(version, highestVersion) > 0) {
				highestVersion = version;
			}
		}

		return highestVersion;
	}

	public static class VersionComparator implements Comparator<String> {
		@Override
		public int compare(String v1, String v2) {
			String[] v1Parts = v1.split("\\.");
			String[] v2Parts = v2.split("\\.");

			for (int i = 0; i < Math.max(v1Parts.length, v2Parts.length); i++) {
				int num1 = i < v1Parts.length ? Integer.parseInt(v1Parts[i]) : 0;
				int num2 = i < v2Parts.length ? Integer.parseInt(v2Parts[i]) : 0;

				if (num1 != num2) {
					return num1 - num2;
				}
			}

			return 0;
		}
	}
}
