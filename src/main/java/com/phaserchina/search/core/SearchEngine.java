package com.phaserchina.search.core;

import java.util.List;

import com.phaserchina.search.object.Item;

public interface SearchEngine {

	public void createIndex();
	
	public List<Item> Search(String keyword);
	
}
