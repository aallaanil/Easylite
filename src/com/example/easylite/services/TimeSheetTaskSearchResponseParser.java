package com.example.easylite.services;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

public class TimeSheetTaskSearchResponseParser {
	public static Map<String, String> parse(String xml) throws IOException {
		Document doc = Jsoup.parse(xml, Constants.URL_TECHM_HR_BS, Parser.xmlParser());
		Element e = doc.getElementById("win0divSEARCHRESULT");
		String html = "<html>" + e.text() + "</html>";
		doc = Jsoup.parse(html);
		Elements elements = doc.getElementsByTag("tr");
		Map<String, String> tasks = new HashMap<String, String>();
		for (Element element: elements) {
			Elements children = element.children();
			int i = 0;
			String taskId = "";
			String taskHierarchy = "";
			boolean found = false;
			for (Element c : children) {
				e = c.tagName("td");
				String css = e.attr("class").toUpperCase();
				if (css.equals("PSSRCHRESULTSODDROW") || css.equals("PSSRCHRESULTSEVENROW")) {
					found = true;
					if (i == 0) {
						taskId = e.text();
					} else if(i == 2) {
						taskHierarchy = e.text();
					}
					++i;
				}
			}
			if (found) {
				tasks.put(taskId, taskHierarchy);
			}
		}
		return tasks;
	}
}
