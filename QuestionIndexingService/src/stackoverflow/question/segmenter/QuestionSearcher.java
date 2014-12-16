package stackoverflow.question.segmenter;

import java.io.File; 
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;



public class QuestionSearcher {
	
	private IndexReader indexReader;
	private IndexSearcher indexSearcher;
	private MultiFieldQueryParser multiFieldQueryParser;
	private QueryParser queryParser;
	private Query query;
	private Analyzer analyzer;
	
	public QuestionSearcher(String indexDir, String searchTag) throws IOException {
		indexReader = DirectoryReader.open(FSDirectory.open(new File(indexDir)));
		indexSearcher = new IndexSearcher(indexReader);
		analyzer = new WhitespaceAnalyzer(Version.LUCENE_46);
		String[] tagFields = {"qTag1","qTag2","qTag3","qTag4","qTag5"};
		multiFieldQueryParser = new MultiFieldQueryParser(Version.LUCENE_46, tagFields, analyzer);
	}
	
	public QuestionSearcher(String indexDir) throws IOException {
		indexReader = DirectoryReader.open(FSDirectory.open(new File(indexDir)));
		indexSearcher = new IndexSearcher(indexReader);
		analyzer = new WhitespaceAnalyzer(Version.LUCENE_46);
		String searchField = "qId";
		queryParser = new QueryParser(Version.LUCENE_46, searchField, analyzer);
	}
	
	public ScoreDoc[] searchPositveQuestionsForGivenTag(String searchTag) throws IOException {
		try {
			query = multiFieldQueryParser.parse(QueryParser.escape(searchTag));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Query : " + query.toString());
		return indexSearcher.search(query, 10000000).scoreDocs;
	}
	
	public ScoreDoc[] searchNegativeQuestionsForGivenTag(String searchTag, int count) throws IOException {
		BooleanQuery negationQuery = null;
		try {
			MatchAllDocsQuery everyDocsCluase = new MatchAllDocsQuery();
			query = multiFieldQueryParser.parse(QueryParser.escape(searchTag));
			negationQuery = new BooleanQuery();
			negationQuery.add(everyDocsCluase, Occur.MUST);
			negationQuery.add(query, Occur.MUST_NOT);
			System.out.println(negationQuery.toString());
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return indexSearcher.search(negationQuery, count).scoreDocs;
	}
	
	public IndexSearcher getIndexSearcher() {
		return this.indexSearcher;
	}
	
	
	public void close() throws IOException {
		this.indexReader.close();
	}
	
	public ScoreDoc[] searchDocumentContentForDocIds(List<Integer> docIdList) throws IOException {
		List<ScoreDoc> docList = new ArrayList<ScoreDoc>();
		for(Integer docId : docIdList) {
			query = NumericRangeQuery.newIntRange ("qId", docId, docId, true, true);
			ScoreDoc[] result = (indexSearcher.search(query, 1).scoreDocs); 
			docList.add(result[0]);
		}
		return (ScoreDoc[]) docList.toArray();
	}	

	
}
