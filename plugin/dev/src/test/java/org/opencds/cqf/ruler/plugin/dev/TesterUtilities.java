package org.opencds.cqf.ruler.plugin.dev;

import org.opencds.cqf.ruler.plugin.utility.ClientUtilities;
import org.opencds.cqf.ruler.plugin.utility.IdUtilities;

public interface TesterUtilities extends IdUtilities, ClientUtilities, IServerSupport {
    public static final String separator = System.getProperty("file.separator");
}
