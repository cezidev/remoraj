package lt.slabs.com.jkoolcloud.remora;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class JustATest2 {
	public JustATest2() {
		System.out.println("################## CONSTRUCTOR");
	}

	public void instrumentedMethod(String uri) throws IOException {
		HttpURLConnection urlConnection = (HttpURLConnection) new URL(uri).openConnection();
		System.out.println(urlConnection.getResponseCode());
	}
}
