package stackoverflow.question.indexer.service;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import stackoverflow.question.indexer.model.Question;

public class QuestionIndexer {
	
	private IndexWriter questionIndexWriter;
	private Directory questionIndexDirectory;
	private IndexWriterConfig questionIndexConfig;
	private String indexDirectory;
	
	public QuestionIndexer(String indexDirectory) {
		try {
			this.indexDirectory = indexDirectory;
			questionIndexWriter = getIndexWriter();
		} catch (IOException e) {
			System.out.println("IO Exception: not able to get IndexWriter!!!");
		}
	}
	
	private IndexWriter getIndexWriter() throws IOException {
		if(questionIndexWriter == null) {
			Analyzer analyzer = new StackOverflowAnalyzer();
			questionIndexConfig = new IndexWriterConfig(Version.LUCENE_46, analyzer);
			questionIndexConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
			questionIndexDirectory = FSDirectory.open(new File(this.indexDirectory));
			questionIndexWriter = new IndexWriter(questionIndexDirectory, questionIndexConfig);
		}
		return questionIndexWriter;
	}
	
	public void indexQuestions(List<Question> questionList) throws IOException {
		Field field = null;
		for(Question question:questionList) {
			
			Document newDoc = new Document();
			//Adding question Id
			field = new IntField("qId", question.getQuestionId(), Field.Store.YES);
			newDoc.add(field);
			
			//Adding question content
			field = new TextField("qContent", question.getQuestionContent(), Field.Store.YES);
			newDoc.add(field);

			//Adding question tags
			int i = 1;
			for(String tag:question.getQuestionTags()) {
				field = new StringField("qTag"+i, tag.toLowerCase(), Field.Store.YES);
				i++;
				newDoc.add(field);
			}
			
			//Add current question into index
			questionIndexWriter.addDocument(newDoc);
		}
	}

	public void close() {
		try {
			this.questionIndexWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
