package com.phaserchina.search.filter;

public class WhiteSpaceFilter implements Filter {

	public String filter(String content) {
		StringBuffer sb = new StringBuffer();
		String[] lines = content.split("\n");
		System.out.println(lines.length);
		for(int i=0; i<lines.length; i++) {
			sb.append(lines[i].trim() + " ");
		}
		return sb.toString();
	}

}
