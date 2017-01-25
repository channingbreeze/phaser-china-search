package com.phaserchina.search.core;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleFragmenter;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;
import org.wltea.analyzer.lucene.IKAnalyzer;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.phaserchina.search.config.Config;
import com.phaserchina.search.filter.Filter;
import com.phaserchina.search.filter.NodeFilter;
import com.phaserchina.search.filter.TikaFilter;
import com.phaserchina.search.filter.WhiteSpaceFilter;
import com.phaserchina.search.object.Item;

public class ItemSearchEngine implements SearchEngine {

	private Analyzer analyzer = new IKAnalyzer(true);
	
	private File indexDir = new File(Config.articleIndexDir);

	public void createIndex() {

		Directory directory = null;
		IndexWriter indexWriter = null;
		try {
			// 创建哪个版本的IndexWriterConfig，根据参数可知lucene是向下兼容的，选择对应的版本就好
			IndexWriterConfig indexWriterConfig = new IndexWriterConfig(
					Version.LUCENE_36, analyzer);
			// 创建磁盘目录对象
			directory = new SimpleFSDirectory(indexDir);
			indexWriter = new IndexWriter(directory, indexWriterConfig);

			// 文档所在目录
			File articleDir = new File(Config.articlesDir);
			List<Item> itemList = new ArrayList<Item>();
			File[] articles = articleDir.listFiles();
			for (File article : articles) {
				if(!article.getName().startsWith("Phaser.")) {
					continue;
				}
				// 这里开始处理content, NodeFilter需要一个文件路径
				String content = new NodeFilter().filter(Config.articlesDir + "\\" + article.getName());
				JSONArray itemJsonArr = JSONArray.parseArray(content);
				for(int i=0; i<itemJsonArr.size(); i++) {
					JSONObject itemJsonObj = itemJsonArr.getJSONObject(i);
					Item item = new Item();
					if(itemJsonObj.containsKey("title")) {
						item.setTitle(itemJsonObj.getString("title"));
					}
					if(itemJsonObj.containsKey("name")) {
						item.setName(itemJsonObj.getString("name"));
					}
					if(itemJsonObj.containsKey("type")) {
						item.setType(itemJsonObj.getString("type"));
					}
					if(itemJsonObj.containsKey("description")) {
						item.setDescription(itemJsonObj.getString("description"));
					}
					if(itemJsonObj.containsKey("anchor")) {
						item.setAnchor(itemJsonObj.getString("anchor"));
					}
					item.setPath(article.getName());
					itemList.add(item);
				}
			}

			// 为了避免重复插入数据，每次测试前 先删除之前的索引
			indexWriter.deleteAll();
			// 获取实体对象
			for (int i = 0; i < itemList.size(); i++) {
				Item itemObject = itemList.get(i);
				// indexWriter添加索引
				Document doc = new Document();
				doc.add(new Field("path", itemObject.getPath().toString(),
						Field.Store.YES, Field.Index.NOT_ANALYZED));
				doc.add(new Field("name", itemObject.getName().toString(),
						Field.Store.YES, Field.Index.NOT_ANALYZED));
				doc.add(new Field("description", itemObject.getDescription().toString(),
						Field.Store.YES, Field.Index.NOT_ANALYZED));
				doc.add(new Field("anchor", itemObject.getAnchor().toString(),
						Field.Store.YES, Field.Index.NOT_ANALYZED));
				doc.add(new Field("type", itemObject.getType().toString(),
						Field.Store.YES, Field.Index.NOT_ANALYZED));
				doc.add(new Field("title", itemObject.getTitle().toString(),
						Field.Store.YES, Field.Index.ANALYZED));
				// 添加到索引中去
				indexWriter.addDocument(doc);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (indexWriter != null) {
				try {
					indexWriter.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (directory != null) {
				try {
					directory.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
	}

	public List<Item> Search(String keyword) {
		IndexReader indexReader = null;
		IndexSearcher indexSearcher = null;
		List<Item> itemList = new ArrayList<Item>();
		try {
			indexReader = IndexReader.open(FSDirectory.open(indexDir));
			// 创建一个排序对象，其中SortField构造方法中，第一个是排序的字段，第二个是指定字段的类型，第三个是是否升序排列，true：降序，false：升序。
			Sort sort = new Sort(new SortField[] {
					new SortField("type", SortField.STRING, false) });
			// 创建搜索类
			indexSearcher = new IndexSearcher(indexReader);
			// 下面是创建QueryParser 查询解析器
			// QueryParser支持单个字段的查询，但是MultiFieldQueryParser可以支持多个字段查询，建议用后者这样可以实现全文检索的功能。
			QueryParser queryParser = new MultiFieldQueryParser(
					Version.LUCENE_36, new String[] { "title" },
					analyzer);
			// 利用queryParser解析传递过来的检索关键字，完成Query对象的封装
			Query query = queryParser.parse(keyword);
			// 执行检索操作，查询top100记录
			TopDocs topDocs = indexSearcher.search(query, 100, sort);
			ScoreDoc[] scoreDoc = topDocs.scoreDocs;
			// 高亮功能
			SimpleHTMLFormatter simpleHtmlFormatter = new SimpleHTMLFormatter(
					"【", "】");
			Highlighter highlighter = new Highlighter(simpleHtmlFormatter,
					new QueryScorer(query));

			for (int i = 0; i < scoreDoc.length; i++) {
				// 内部编号 ,和数据库表中的唯一标识列一样
				int doc = scoreDoc[i].doc;
				// 根据文档id找到文档
				Document mydoc = indexSearcher.doc(doc);

				String path = mydoc.get("path");
				String name = mydoc.get("name");
				String description = mydoc.get("description");
				String anchor = mydoc.get("anchor");
				
				// 需要注意的是 如果使用了高亮显示的操作，查询的字段中没有需要高亮显示的内容 highlighter会返回一个null回来。
				itemList.add(new Item(path, null, name, description, anchor, null));
			}
		} catch (CorruptIndexException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		} finally {
			if (indexSearcher != null) {
				try {
					indexSearcher.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
			if (indexReader != null) {
				try {
					indexReader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return itemList;
	}

}
