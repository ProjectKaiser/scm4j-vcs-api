package com.projectkaiser.scm.vcs.api;

public interface IVCS {
	void createBranch(String srcBranchPath, String dstBranchPath, String commitMessage);

	PKVCSMergeResult merge(String srcBranchPath, String dstBranchPath, String commitMessage);

	void deleteBranch(String branchPath, String commitMessage);

	void setCredentials(String user, String password);

	void setProxy(String host, int port, String proxyUser, String proxyPassword);

	String getBaseUrl();

	String getFileContent(String branchName, String filePath, String encoding);

	void setFileContent(String branchName, String filePath, String content, String commitMessage);
}
