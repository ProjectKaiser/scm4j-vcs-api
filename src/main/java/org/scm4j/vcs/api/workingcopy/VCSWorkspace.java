package org.scm4j.vcs.api.workingcopy;

import java.io.File;

/**
 * The Class VCSWorkspace.
 */
public class VCSWorkspace implements IVCSWorkspace {

	public static final String DEFAULT_WORKSPACE_DIR = new File(System.getProperty("java.io.tmpdir"), "scm4j-vcs-workspaces").getPath();

	private final File folder;

	@Override
	public IVCSRepositoryWorkspace getVCSRepositoryWorkspace(String repoUrl) {
		return new VCSRepositoryWorkspace(repoUrl, this);
	}

	@Override
	public File getHomeFolder() {
		return folder;
	}

	public VCSWorkspace(String workspacePath) {
		folder = new File(workspacePath);
		folder.mkdirs();
	}
	
	public VCSWorkspace() {
		this(DEFAULT_WORKSPACE_DIR);
	}

	@Override
	public String toString() {
		return folder.toString();
	}
}
