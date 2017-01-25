package com.phaserchina.search;

import com.phaserchina.search.core.ItemSearchEngine;
import com.phaserchina.search.core.SearchEngine;

public class CreateIndex {

	public static void main(String[] args) {
		SearchEngine searchEngine = new ItemSearchEngine();
		searchEngine.createIndex();
		System.out.println("create index over");
	}
	
}
