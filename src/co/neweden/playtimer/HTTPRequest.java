package co.neweden.playtimer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

public class HTTPRequest {
	
	public static boolean forumUserExists(String userName) {
		
		try {
			URL url = new URL("http://neweden.co/api/doesUserExist.php?username=" + userName);
			BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
			String strTemp = "";
			while (null != (strTemp = br.readLine())) {
				if (strTemp.equals("TRUE")) {
					return true;
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return false;
	}

}
