package com.phaserchina.search.filter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.Properties;

import com.ctc.wstx.util.StringUtil;

public class NodeFilter implements Filter {

	public static String exeCmd(String commandStr) {
		BufferedReader br = null;
		try {
			Process p = Runtime.getRuntime().exec(commandStr);
			br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line = null;
			StringBuilder sb = new StringBuilder();
			while ((line = br.readLine()) != null) {
				sb.append(line + "\n");
			}
			return sb.toString();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	public String filter(String filePath) {
		String res = null;
		try {
			String nodePath = NodeFilter.class.getClassLoader().getResource("nodefilter").getPath();
			nodePath = nodePath.substring(1, nodePath.length());
			nodePath = URLDecoder.decode(nodePath, "UTF-8");
			nodePath = nodePath + "\\index.js";
			Properties prop = System.getProperties();
			String os = prop.getProperty("os.name");
			
			// windows系统
			if(os.startsWith("win") || os.startsWith("Win")) {
				res = exeCmd("cmd.exe /c node " + nodePath + " " + filePath);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return res;
	}

}
