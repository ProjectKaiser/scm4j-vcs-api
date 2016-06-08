package com.projectkaiser.scm.vcs.api;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.UUID;

import org.apache.commons.io.FileUtils;

public class VCSWorkspace {

	public static final String LOCK_FILE_PREFIX = "lock_";

	private Boolean corrupt = false;
	private File folder;
	private FileOutputStream lockedStream;
	private File lockFile;
	private FileLock fileLock;
	private VCSWorkspaceState state = VCSWorkspaceState.NOT_INITIALIZED;
	
	public VCSWorkspaceState getState() {
		return state;
	}
	
	public File getLockFile() {
		return lockFile;
	}

	public void setLockFile(File lockFile) {
		this.lockFile = lockFile;
	}

	public File getFolder() {
		return folder;
	}

	public void setFolder(File folder) {
		this.folder = folder;
	}

	public void setCorrupt(Boolean isCorrupt) {
		this.corrupt = isCorrupt;
	}

	private VCSWorkspace(File folder, FileOutputStream lockedStream, File lockFile, FileLock fileLock) {
		this.folder = folder;
		this.lockedStream = lockedStream;
		this.lockFile = lockFile;
		this.fileLock = fileLock;
		state = VCSWorkspaceState.LOCKED;
	}

	public Boolean getCorrupt() {
		return corrupt;
	}

	public void unlock() {
		if (state != VCSWorkspaceState.LOCKED) {
			return;
		}
		
		try {
			fileLock.close();
			lockedStream.close();
			state = VCSWorkspaceState.OBSOLETE;
			if (corrupt) {
				FileUtils.deleteDirectory(folder);
				lockFile.delete();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static VCSWorkspace getLockedWorkspace(String workspaceBasePath) {
		File workspaceBaseFolder = new File(workspaceBasePath);
		if (!workspaceBaseFolder.exists()) {
			workspaceBaseFolder.mkdirs();
		}
		
		File[] files = workspaceBaseFolder.listFiles();
		for (File file : files) {
			if (file.isDirectory()) {
				File lockFile = new File(workspaceBaseFolder, LOCK_FILE_PREFIX + file.getName());
				try {
					if (!lockFile.exists()) {
						lockFile.createNewFile();
					}

					try {
						FileOutputStream s = new FileOutputStream(lockFile, false);
						try {
							FileLock fileLock = s.getChannel().lock();
							return new VCSWorkspace(file, s, lockFile, fileLock);
						} catch (OverlappingFileLockException e) {
							s.close();
							continue;
						}
					} catch (SecurityException e) {
						continue;
					}
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}

		String guid = UUID.randomUUID().toString();
		File newFolder = new File(workspaceBaseFolder, guid);
		newFolder.mkdirs();
		FileOutputStream s;
		File lockFile = new File(workspaceBaseFolder, LOCK_FILE_PREFIX + newFolder.getName());

		try {
			s = new FileOutputStream(lockFile, false);
			try {
				FileLock fileLock = s.getChannel().lock();
				return new VCSWorkspace(newFolder, s, lockFile, fileLock);
			} catch (Exception e) {
				s.close();
				throw new RuntimeException(e);
			}

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String toString() {
		return "VCSWorkspace [corrupt=" + corrupt + ", state=" + state.toString() + ", folder=" + folder.toString() + "]";
	}
}
