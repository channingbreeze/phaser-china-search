package com.phaserchina.search.object;

public class Item {

	/**
	 * 该项对应的文件路径
	 */
	private String path;
	
	/**
	 * 该项的标题，也就是做反向索引的关键词
	 */
	private String title;
	
	/**
	 * 该项的名字，不进行反向索引
	 */
	private String name;
	
	/**
	 * 该项的具体描述，不进行反向索引
	 */
	private String description;
	
	/**
	 * 该项的锚点，用于跳转时的定位
	 */
	private String anchor;
	
	/**
	 * 该项的类型，是constructor还是member还是method
	 */
	private String type;
	
	public Item() {
		
	}

	public Item(String path, String title, String name, String description, String anchor,
			String type) {
		super();
		this.path = path;
		this.title = title;
		this.name = name;
		this.description = description;
		this.anchor = anchor;
		this.type = type;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getName() { 
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getAnchor() {
		return anchor;
	}

	public void setAnchor(String anchor) {
		this.anchor = anchor;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return "Article [path=" + path + ", title=" + title + ", name=" + name + ", description="
				+ description + ", anchor=" + anchor + ", type=" + type + "]";
	}
	
}
