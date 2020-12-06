package us.ajg0702.leaderboards;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class Downloader {
	static Downloader instance;
	public static Downloader getInstance() {
		return instance;
	}
	public static Downloader getInstance(Main pl) {
		if(instance == null) {
			instance = new Downloader(pl);
		}
		return instance;
	}
	
	Main pl;
	private Downloader(Main pl) {
		this.pl = pl;
	}
	
	
	public void downloadAndLoad() {
		try {
			File f = new File(pl.getDataFolder().getAbsolutePath()+File.separator+"lib"+File.separator);
			if(f.exists()) {
				loadClasses();
				return;
			} else {
				f.mkdirs();
			}
			URL website = new URL("https://ajg0702.us/pl/ajLeaderboards/libs/sqlite.jar");
			//URL website = new URL("https://api.spiget.org/v2/resources/60909/versions/latest/download");
			HttpURLConnection con = (HttpURLConnection) website.openConnection();
			con.addRequestProperty("User-Agent", "ajLeaderboards/"+pl.getDescription().getVersion());
			con.setInstanceFollowRedirects(true);
			HttpURLConnection.setFollowRedirects(true);
			
			
			boolean redirect = false;
			int status = con.getResponseCode();
			if (status != HttpURLConnection.HTTP_OK) {
				if (status == HttpURLConnection.HTTP_MOVED_TEMP
					|| status == HttpURLConnection.HTTP_MOVED_PERM
						|| status == HttpURLConnection.HTTP_SEE_OTHER)
				redirect = true;
			}
			
			if (redirect) {

				// get redirect url from "location" header field
				String newUrl = con.getHeaderField("Location");

				// get the cookie if need, for login
				String cookies = con.getHeaderField("Set-Cookie");

				// open the new connnection again
				con = (HttpURLConnection) new URL(newUrl).openConnection();
				con.setRequestProperty("Cookie", cookies);
				con.addRequestProperty("User-Agent", "ajLeaderboards/"+pl.getDescription().getVersion());
										
				System.out.println("Redirect to URL : " + newUrl);

			}
			
			
			redirect = false;
			status = con.getResponseCode();
			if (status != HttpURLConnection.HTTP_OK) {
				if (status == HttpURLConnection.HTTP_MOVED_TEMP
					|| status == HttpURLConnection.HTTP_MOVED_PERM
						|| status == HttpURLConnection.HTTP_SEE_OTHER)
				redirect = true;
			}
			
			if (redirect) {

				// get redirect url from "location" header field
				String newUrl = con.getHeaderField("Location");
				
				// get the cookie if need, for login
				String cookies = con.getHeaderField("Set-Cookie");

				// open the new connnection again
				con = (HttpURLConnection) new URL(newUrl).openConnection();
				con.setRequestProperty("Cookie", cookies);
				con.setRequestProperty("Connection", "Connection: close");
				con.addRequestProperty("User-Agent", "ajLeaderboards/"+pl.getDescription().getVersion());
				//con.addRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.88 Safari/537.36");
										
				System.out.println("Redirect to URL : " + newUrl);

			}
			
			ReadableByteChannel rbc = Channels.newChannel(con.getInputStream());
			FileOutputStream fos = new FileOutputStream(pl.getDataFolder().getAbsolutePath()+File.separator+"lib"+File.separator+"sqlite.jar");
			fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
			fos.close();
			loadClasses();

		} catch(Exception e) {
			pl.getLogger().warning("An error occured while trying to download lib:");
			e.printStackTrace();
			return;
		}
	}
	
	@SuppressWarnings("rawtypes")
	List<Class> classes = new ArrayList<>();
	private void loadClasses() throws IOException {
		String pathToJar = pl.getDataFolder().getAbsolutePath()+File.separator+"lib"+File.separator+"sqlite.jar";
		JarFile jarFile = new JarFile(pathToJar);
		Enumeration<JarEntry> e = jarFile.entries();

		URL[] urls = { new URL("jar:file:" + pathToJar+"!/") };
		URLClassLoader cl = URLClassLoader.newInstance(urls);

		while (e.hasMoreElements()) {
		    JarEntry je = e.nextElement();
		    
		    if(je.isDirectory() || !je.getName().endsWith(".class")){
		    	pl.getLogger().info("[s] "+je.getName());
		        continue;
		    }
		    pl.getLogger().info("[l] "+je.getName());
		    // -6 because of .class
		    String className = je.getName().substring(0,je.getName().length()-6);
		    className = className.replace('/', '.');
		    try {
				classes.add(cl.loadClass(className));
			} catch (ClassNotFoundException e1) {
				e1.printStackTrace();
			}

		}
		jarFile.close();
	}
}
