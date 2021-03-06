[![Release](https://jitpack.io/v/scm4j/scm4j-vcs-api.svg)](https://jitpack.io/#scm4j/scm4j-vcs-api)
[![Build Status](https://travis-ci.org/scm4j/scm4j-vcs-api.svg?branch=master)](https://travis-ci.org/scm4j/scm4j-vcs-api)
[![Coverage Status](https://coveralls.io/repos/github/scm4j/scm4j-vcs-api/badge.svg?branch=master)](https://coveralls.io/github/scm4j/scm4j-vcs-api?branch=master) 

# Overview
scm4j-vcs-api is set of base classes and interfaces to build VCS support (Git, SVN, etc) libraries which exposes basic vcs-related operations: merge, branch create etc.
scm4j-vcs-api provides:
- A simple interface to implement basic VCS-related operations
- Working copies management for operations which must be executed on a local file system

# Terms
- `IVCS`
	- Basic exposed interface which contains vcs-related methods
- Workspace Home
	- Home folder of all vcs-related operations which are require to use local file system.
	- Defined by IVCS-user side
- Repository Workspace
	- A separate folder where Working Copies of one certain Repository will be located. Need to group few Working Copies used by one Repository into one folder. E.g. if there are Git and SVN version control systems then need to know which VCS type each Working Copy belongs to.
    - Created and named automatically as repository url replacing all special characters with "_"
- Locked Working Copy, LWC
	- A separate folder used to execute VCS-related operations which are need to be executed on a local file system. E.g. in Git it is need to make checkout somewhere on local file system before making a merge.
	- Named automatically as uuid, located within Repository Workspace folder
	- Can be reused for another vcs-related operation automatically. I.e. checked out once, then switches between branches.
	- Deletes automatically if last VCS-related operation left the Working Copy in corrupted state, i.e. can not be reverted, re-checked out and so on
- Lock File
	- A special empty file which is used to show if according LWC locked or free. If a Lock File has exclusive file system lock then the according LWC folder is considered as locked, otherwise as free
	- Lock way: `new FileOutputStream(lockFile, false).getChannel.lock()`
	- named as "lock_" + <LWC folder name>
- Abstract Test
	- Base functional tests of VCS-related functions which are exposed by IVCS. To implement functional test for a certain IVCS implementation (Git, SVN, etc) just implement VCSAbstractTest subclass
	- Implemented as [scm4j-vcs-test](https://github.com/scm4j/scm4j-vcs-test) separate project
- `VCSMergeResult`, Merge Result
	- Result of vcs merge operation. Could be successful or failed. Provides list of conflicting files if failed.
- `VCSDiffEntry`, Diff Entry
	- Result of VCS branches diff operation. Contains Diff type (added, modified, deleted) and unified diff string for a certain file which differs between branches 
- Head, Head Commit, Branch Head
	- The latest commit or state of a branch
- Master Branch
	- "Master" for Git, "Trunk" for SVN etc
- `VCSTag`, Tag
    - Contains tag name, tag log message, tag author and `VCSCommit` instance which represents the tagged commit

# Using VCS interface
IVCS interface consists of few basic vcs functions.
Note: null passed as a branch name is considered as Master Branch. Any non-null branch name is considered as user-created branch within conventional place for branches: any branch except "master" for Git, any branch within "Branches" branch for SVN etc. For SVN do not use "Branches\my-branch" as branch name, use "my-branch" instead.
- `void createBranch(String srcBranchName, String dstBranchName, String commitMessage)`
	- Creates a new branch named `dstBranchName` from the Head of `srcBranchName`.
	- commitMessage is a log message which will be attached to branch create operation if it possible (e.g. Git does not posts branch create operation as a separate commit)
- `VCSMergeResult merge(String srcBranchName, String dstBranchName, String commitMessage);`
	- Merge all commits from `srcBranchName` to `dstBranchName` with `commitMessage` attached
	- `VCSMergeResult.getSuccess() == true`
		- merge is successful
	- `VCSMergeResult.getSuccess() == false`
		- Automatic merge can not be completed due of conflicting files
		- `VCSMergeResult.getConflictingFiles()` contains paths to conflicting files
	- Heads of branches `srcBranchName` and `dstBranchName` are used
- `void deleteBranch(String branchName, String commitMessage)`
	- Deletes branch with path `branchName` and attaches `commitMessage` to branch delete operation if possible (e.g. Git does not posts branch delete operation as a separate commit)
-  `void setCredentials(String user, String password)`
	- Applies credentials to existing IVCS implementation. I.e. first a IVCS implementation should be created, then credentials should be applied when necessary
- `void setProxy(String host, int port, String proxyUser, String proxyPassword)`
	- Sets proxy parameters if necessary
- `String getRepoUrl()`
	- Returns string url of current vcs repository
- `String getFileContent(String branchName, String fileRelativePath, String revision)`
	- Returns file content as a string using UTF-8 encoding.
	- `fileRelativePath` is a path to file within `branchName` branch 
	- File state at `revision` revision is used. If `revision` is null then Head state is used
- `VCSCommit setFileContent(String branchName, String filePath, String content, String commitMessage)`
	- Rewrites a file with path `filePath` within branch `branchName` with content `content` and applies `commitMessage` message to commit
	- Creates the file and its parent folders if doesn't exists
- `VCSCommit setFileContent(String branchName, List<VCSChangeListNode> vcsChangeList)`
	- For each `vcsChangeListNode`: rewrites file with path `vcsChangeListNode.getFilePath()` within branch `branchName` with content `vcsChangeListNode.getContent()`
	- Single commit, commit message is all `vcsChangeListNode.getCommitMessage()` strings joined with ", "
	- Creates files and its parent folders if aren't exist
	- Returns null if `vcsChangeList` is empty
- `List<VCSDiffEntry> getBranchesDiff(String srcBranchName, String destBranchName)`
	- Returns list of `VCSDiffEntry` showing what was made within branch `srcBranchName` relative to branch `destBranchName`
	- Note: result could be considered as a commit which would be made on merging the branch `srcBranchName` into `destBranchName`
- `Set<String> getBranches(String path)`
	- Returns list of names of all branches which are started from `path`. Branches here are considered as user-created branches and Master Branch. I.e. any branch for Git, "Trunk" and any branch within "Branches" branch (not "Tags" branches) for SVN etc
    - `path` processing
        - Git
            - prefix of branch names to browse
            - Assume we have following branches:
                - branch_1
                - branch_2
                - new-branch
            - then `getBranches("br")` will return [branch_1, branch_2]
        - SVN
            - Assume we have following folders structure:
                - branches/branch_1/folder
                - branches/branch_2/folder
                - branches/release/v1/folder
                - branches/release/v2/folder
                - branches/release/a2/folder
                - branches/new-branch/folder
                - branch_3/folder
                - tags/
                - trunk/
                - new-branch
            - `path` is empty - result is all first-level subdirst within `branches/` folder
                - `getBranches("")` -> [branch_1, branch_2, release, new-branch]
            - `path` ends with "/" or empty - result is all first-level subdirs within `branches/path` dir
                - `getBranches("release/")` -> [v1, v2, a2]
            - `path` does not ends with "/" - result is first-level subdirs within `branches/path` dir up to the last slash which names starts with `path` dir from the last slash till end substring
                 - `getBranches("new-")` -> [new-branch]
                 - `getBranches("release/v")` -> [v1, v2]
- `List<VCSCommit> log(Sting branchName, Integer limit)`
	- Returns list of commits of branch `branchName` limited by `limit` in descending order
- `String getVCSTypeString`
	- Returns short name of current IVCS implementation: "git", "svn" etc
- `VCSCommit removeFile(String branchName, String filePath, String commitMessage)`
	- Removes the file with path `filePath` within branch `branchName`. Operation is executed as separate commit with `commitMessage` message attached. Note: filePath = "folder\file.txt" -> file.txt is removed, folder is kept. Returns resulting commit.
- `List<VCSCommit> getCommitsRange(String branchName, String startRevision, String endRevision)`
	- Returns ordered list of all commits located between commits specified by `startRevision` and `endRevision` inclusively within branch `branchName`
	- If `startRevision` is null then all commits up to commit specified by `endRevision` inclusively are fetched
	- If `endRevision` is null then all commits starting from commit specified by `startRevision` are fetched
	- If `startRevision` and `endRevision` are null then all commits are fetched
- `List<VCSCommit> getCommitsRange(String branchName, String startRevision, WalkDirection direction, int limit)`
    - Returns ordered list of `limit` commits (0 is unlimited) starting from commit specified by `startRevision` in direction specified by `direction`
    - If `startRevision` is null then all commits are fetched
- `VCSCommit getHeadCommit(String branchName)`
    - Returns `VCSCommit` instance pointing to the head (last) commit of the branch `branchName` or `null` if the requested branch does not exists  
- `Boolean fileExists(String branchName, String filePath)`
    - Returns true if the file with path `filePath` exists in repository in branch `branchName`, false otherwise
- `VCSTag createTag(String branchName, String tagName, String tagMessage) throws EVCSTagExists`
    - Creates a tag named `tagName` with log message `tagMessage` on a Head of branch `branchName`
- `List<VCSTag> getTags()`
    - Returns list of all tags
- `void removeTag(String tagName)`
    - Removes tag with name `tagName`
- `void checkout(String branchName, String targetPath, String revision)`
    - Checks out a branch `branchName` on a revision `revision` into a local folder `targetPath`
- `List<VCSTag> getTagsOnRevision(String revision)`
    - returns list of all tags which are related to commit specified by `revision`    
    
# Using Locked Working Copy
Let's assume we developing a multiuser server which has ability to merge branches of user's repositories. So few users could request to merge theirs branches of different repositories simultaneously. For example, Git merge operation consists of few underlying operations (check in\out, merge itself, push) which must be executed on a local file system in a certain folder. So we have following requirements:
- The simple way to allocate place for vcs operations execution
- Make this place "transactional", protecting this place of interfere from other vcs operations
- Reusing ability for the same Repository to prevent of checkout operation executions each time

Locked Working Copy is a solution which solves these requirements by providing a certain folder and guarantees that this folder will not be assigned to another LWC instance until its `close()` method will be called

LWC usage scenario:
- Create Workspace Home instance providing path to any folder as Workspace Home folder path. This folder will contain repositories folders (if different vcs or repositories are used)
```java
	public static final String WORKSPACE_DIR = System.getProperty("java.io.tmpdir") + "scm4j-vcs-workspaces";
	...
	IVCSWorkspace workspace = new VCSWorkspace(WORKSPACE_DIR);
	...
```
- Obtain Repository Workspace from Workspace Home providing a certain Repository's url. The obtained Repository Workspace will represent a folder within Workspace Home dir which will contain all Working Copies relating to the provided VCS Repository  
```java
	String repoUrl = "https://github.com/scm4j/scm4j-vcs-api";
	IVCSRepositoryWorkspace repoWorkspace = workspace.getVCSRepositoryWorkspace(repoUrl);
```
- Obtain Locked Working Copy from Repository Workspace when necessary. The obtained LWC will represent a locked folder within Workspace Repository. The folder is protected from simultaneously execute different vcs-related operations by another thread or even process. Use try-with-resources or try...finally to release Working Copy after vcs-related operations will be completed
```java
	try (IVCSLockedWorkingCopy wc = repoWorkspace.getVCSLockedWorkingCopy()) {
	...
	}
```
- Use `IVCSLockedWorkingCopy.getFolder()` as folder for vcs-related operations
- Do not use `IVCSLockedWorkingCopy` instance after calling `IVCSLockedWorkingCopy.close()` method because after closing `IVCSLockedWorkingCopy` instance does not guarantees that according folder is not in use
- Consider `IVCSLockedWorkingCopy.getState()` values:
	- LOCKED
		- current `IVCSLockedWorkingCopy` represents a locked folder, i.e. a folder which is not used by other `IVCSLockedWorkingCopy` instances. 
	- OBSOLETE
		- `IVCSLockedWorkingCopy.close()` method has been called. Corresponding folder is unlocked and could be used by other `IVCSLockedWorkingCopy` instances. `IVCSLockedWorkingCopy` instance with this state should not be used anymore.
- If a Working copy can not be reused due of VCS system data damage (e.g. .git, .svn folders) or due of vcs Working Copy can not be cleaned, reverted, switched, checked out etc, execute `IVCSLockedWorkingCopy.setCorrupted(true)`. LWC folder will be deleted on close.
- Code snippet
```java
public static final String WORKSPACE_DIR = System.getProperty("java.io.tmpdir") + "scm4j-vcs-workspaces";
public static void main(String[] args) {
    IVCSWorkspace workspace = new VCSWorkspace(WORKSPACE_DIR);
    String repoUrl = "https://github.com/scm4j/scm4j-vcs-api";
    IVCSRepositoryWorkspace repoWorkspace = workspace.getVCSRepositoryWorkspace(repoUrl);
    try (IVCSLockedWorkingCopy wc = repoWorkspace.getVCSLockedWorkingCopy()) {
        // wc.getFolder() is locked folder
    }
}
```

# Folder structure
- Workspace Home folder (e.g. c:\temp\scm4j-vcs-workspces\)
	- Repository Workspace 2 (e.g. <Workspace Home>\https_github_com_scm4j\
		- Working Copy 1 
			- Branch1 is checked out, merging executes
		- Working Copy 2
			- branch creating executes
		- ...
	-  Repository Workspace 2 (e.g. <Workspace Home>\c_svn_file_repo\)
		- Working Copy 1
		- ...
	- ...

# Working Copy locking way
A special Lock File is created on `IVCSLockedWorkingCopy` instance creation and placed beside the LWC folder which been locking. This file is keeping opened with exclusive lock so any other process (from local or remote PC) can not open it again. So to check if a LWC folder is free it is need to try to lock according Lock File. If success then the according folder was free and can be assigned to current `IVCSLockedWorkingCopy` instance. Otherwise folder is locked and we need to check other folders. If there are no folders left then new folder is created and locked.
So actually a Lock File is locked, not the LWC folder itself. 
Lock way: `new FileOutputStream(lockFile, false).getChannel.lock()`

# Developing IVCS implementation
- Add github-hosted scm4j-vcs-api and scm4j-vcs-test projects as maven artifacts using [jitpack.io](https://jitpack.io/). As an example, add following to gradle.build file:
	```gradle
	allprojects {
		repositories {
			maven { url "https://jitpack.io" }
		}
	}
	
	dependencies {
		// versioning: master-SNAPSHOT (lastest build, unstable), + (lastest release, stable) or certain version (e.g. 1.0)
		compile 'com.github.scm4j:scm4j-vcs-api:+'
		testCompile 'com.github.scm4j:scm4j-vcs-test:+'
	}
	```
	This will include VCS API (IVCS, LWC) and Abstract Test to your project.
	Also you can download release jars from https://github.com/scm4j/scm4j-vcs-api/releases, https://github.com/scm4j/scm4j-vcs-test/releases
- Implement IVCS interface
	- IVCS implementation should be separate object which normally holds all VCS-related data within
	- Normally IVCSRepositoryWorkspace instance is passed to constructor and stored within IVCS implementation. 
	- All VCS-related operations must be executed within a folder associated with IVCSLockedWorkingCopy in LOCKED state. That guarantees that the folder will not be used by another VCS operations simultaneously. Call `IVCSRepositoryWorkspace.getLockedWorkingCopy()` to obtain LWC when necessary
	- Use `IVCSLockedWorkingCopy.getFolder()` to get a folder for vcs-related operations
	- Note: if `IVCSRepositoryWorkspace.getLockedWorkingCopy()` was called then IVCSLockedWorkingCopy.close() must be called. LWC `close()` call is checked by Abstract Test 
	```java
	public class GitVCS implements IVCS {
	
		IVCSRepositoryWorkspace repo;
		
		public GitVCS(IVCSRepositoryWorkspace repo) {
			this.repo = repo;
		}
		
		@Override
		public void createBranch(String srcBranchName, String newBranchName, String commitMessage) {
		try {
			try (IVCSLockedWorkingCopy wc = repo.getVCSLockedWorkingCopy()) {
				// execute branch create
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		// ...
	} 
	```	
	- Throw exceptions from scm4j.vcs.api.exceptions package. Abstract Test checks throwning of these exceptions.
- Implement functional tests
	- Create VCSAbstractTest subclass within test package, implement all abstract methods
	- Normally test class should not include any test, just @After and @Before methods. All necessary functional testing is implemented within VCSAbstractTest
	- See [scm4j-vcs-test](https://github.com/scm4j/scm4j-vcs-test) for details
- Example of gradle usage to export IVCS implementation, its sources and javadoc as separate single JARs:
```gradle
task sourcesJar(type: Jar, dependsOn: classes) {
	classifier = 'sources'
	from sourceSets.main.allSource
}
	
task javadocJar(type: Jar, dependsOn: javadoc) {
	classifier = 'javadoc'
	from javadoc.destinationDir
}

artifacts {
	archives sourcesJar
	archives javadocJar
}
```
After that the `gralde build` command will produce 3 JARs.

# See also
- [scm4j-vcs-test](https://github.com/scm4j/scm4j-vcs-test)
- [scm4j-vcs-git](https://github.com/scm4j/scm4j-vcs-git)
- [scm4j-vcs-svn](https://github.com/scm4j/scm4j-vcs-svn)
