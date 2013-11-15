package org.gluster.fs;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;

import org.gluster.io.glfs_javaJNI;


public class GlusterFile {

	private String path;
	private long handle;

	protected GlusterFile(String path, long handle) {
		this.handle = handle;
		this.path = path;
	}

	protected GlusterFile(GlusterFile parent, String path) {
		handle = parent.handle;
	}

	/*
	 * convenience method to return any file on the same file system given its
	 * path
	 */
	public GlusterFile newInstance(String path) {
		return new GlusterFile(path, this.handle);

	}

	private static String getName(String fullPath){
		int index = fullPath.lastIndexOf(GlusterClient.PATH_SEPARATOR);
		return fullPath.substring(index + 1);
	}
	
	public String getName() {
		return getName(this.path);
	}

	public String getPath(){
		return this.path;
	}
	
    public String getParent() {
        int index = path.lastIndexOf(GlusterClient.PATH_SEPARATOR);
        return path.substring(0, index);
   }
    
    public GlusterFile getAbsoluteFile() {
        return this;
    }
    
    private static String slashify(String path, boolean isDirectory) {
        String p = path;
        if (GlusterClient.PATH_SEPARATOR != '/')
            p = p.replace(GlusterClient.PATH_SEPARATOR, '/');
        if (!p.startsWith("/"))
            p = "/" + p;
        if (!p.endsWith("/") && isDirectory)
            p = p + "/";
        return p;
    }
    
    public URI toURI() {
        try {
        	String sp = slashify(getPath(), isDirectory());
            if (sp.startsWith("//"))
                sp = "//" + sp;
            return new URI("glusterfs", null, sp, null, null);
        } catch (Exception e) {
			// impossible.
		}
        return null;
    }

	public boolean mkdirs() {
		String[] pieces = (path.indexOf(GlusterClient.PATH_SEPARATOR) == 0 ? path.substring(1) : path).split(String.valueOf(GlusterClient.PATH_SEPARATOR));
		String p = "";
		int i;
		GlusterFile f = null;

		for (i = 0; i < pieces.length; i++) {
			p = p.concat(String.valueOf(GlusterClient.PATH_SEPARATOR)).concat(
					pieces[i]);
			f = newInstance(p);
			if (!f.exists()) {
				if (!f.mkdir())
					return false;
			} else if (!f.isDirectory()) {
				return false;
			}
		}

		return (f != null && f.isDirectory());
	}

	public OutputStream outputStream() throws IOException {
		return new GlusterOutputStream(this.path, this.handle);
	}

	public InputStream inputStream() throws IOException {
		return new GlusterInputStream(this.path, this.handle);
	}

	public GlusterFile[] listFiles() {
		String[] ss = list();
		if (ss == null)
			return null;
		int n = ss.length;
		GlusterFile[] fs = new GlusterFile[n];
		for (int i = 0; i < n; i++) {
			fs[i] = newInstance(ss[i]);
		}
		return fs;
	}
	
	public GlusterFile[] listFiles(FilenameFilter filter) {
        String ss[] = list();
        if (ss == null) return null;
        ArrayList<GlusterFile> files = new ArrayList<GlusterFile>();
        for (String s : ss)
            if ((filter == null) || filter.accept(new File(s),s))
                files.add(newInstance(s));
        return files.toArray(new GlusterFile[files.size()]);
    }
	
	public String[] list() {
		String list[] = glfs_javaJNI.glfs_java_list_dir(handle, this.path);
		
		for(int i=0;i<list.length;i++){
			list[i] = getName(list[i]);
		}
		return list;
	}
	
	public String[] list(FilenameFilter filter) {
        String names[] = list();
        if ((names == null) || (filter == null)) {
            return names;
        }
        ArrayList v = new ArrayList();
        for (int i = 0 ; i < names.length ; i++) {
            if (filter.accept(new File(names[i]),names[i])) {
                v.add(getName(names[i]));
            }
        }
        return (String[])(v.toArray(new String[v.size()]));
    }
	
	public String toString() {
		return path;
	}
	
	public boolean renameTo(String dstpath) {
		return glfs_javaJNI.glfs_java_file_renameTo(handle, path, dstpath);
	}

	public long length() {
		return glfs_javaJNI.glfs_java_file_length(handle, path);
	}

	public boolean exists() {
		return glfs_javaJNI.glfs_java_file_exists(handle, path);
	}

	public boolean createNewFile() {
		return glfs_javaJNI.glfs_java_file_createNewFile(handle, path);
	}

	public boolean mkdir() {
		return glfs_javaJNI.glfs_java_file_mkdir(handle, path);
	}

	public boolean isDirectory() {
		return glfs_javaJNI.glfs_java_file_isDirectory(handle, path);
	}

	public boolean isFile() {
		return glfs_javaJNI.glfs_java_file_isFile(handle, path);
	}
	
	public boolean delete() {
		return glfs_javaJNI.glfs_java_file_delete(handle, path);
	}

	public boolean renameTo(GlusterFile dst) {
		if (dst.handle != handle)
			return false;

		return glfs_javaJNI.glfs_java_file_renameTo(handle, path, dst.path);
	}
}
