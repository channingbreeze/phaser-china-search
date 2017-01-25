package com.phaserchina.search.filter;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.html.HtmlParser;
import org.apache.tika.sax.BodyContentHandler;

public class TikaFilter implements Filter {

	public String filter(String content) {
		try {
			// detecting the file type
			BodyContentHandler handler = new BodyContentHandler();
			Metadata metadata = new Metadata();
			InputStream inputStream = new ByteArrayInputStream(content.getBytes());
			ParseContext pcontext = new ParseContext();
	
			// Html parser
			HtmlParser htmlparser = new HtmlParser();
			htmlparser.parse(inputStream, handler, metadata, pcontext);
			return handler.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return content;
	}

}
