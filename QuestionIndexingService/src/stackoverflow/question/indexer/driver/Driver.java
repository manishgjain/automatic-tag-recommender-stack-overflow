package stackoverflow.question.indexer.driver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;

import stackoverflow.question.indexer.model.Question;
import stackoverflow.question.indexer.service.QuestionIndexer;
import stackoverflow.question.indexer.utils.QuestionXmlParser;
import stackoverflow.question.segmenter.QuestionSearcher;

public class Driver { 

	public static void main(String[] args) { 
		
		if(args.length < 1) {
			System.out.println("Usage: -index <path_to_the_files_to_be_indexed> or -segment <path_of_segmented_data>");
			return;
		}
		if(args.length == 2 && args[0].equals("-index")) {
			try {
				indexQuestionFiles(new File(args[1]));
			} catch (FileNotFoundException e) {
				System.out.println("Error: files not found");
				e.printStackTrace();
			}
			return;
		}
		if(args.length == 2 && args[0].equals("-perClusterTags")) {
			String rootDir = args[1];
			try {
				createTagSetPerCluster(rootDir);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}
		if(args.length == 2 && args[0].equals("-segmentFullData")) {
			String rootDir = args[1];
			File tagFile = new File(rootDir + "/tags_filtered.txt");
			List<String> tagList = new ArrayList<String>();
			//Read the tags from the file into List.
			try(BufferedReader reader = new BufferedReader(new FileReader(tagFile))) {
				for(String line; (line = reader.readLine()) != null;) {
					tagList.add(line.toLowerCase());
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			for(String eachTag : tagList) {
				try {
					System.out.println("SEARCHING : " + eachTag);
					searchAndWriteQuestionsBasedOnGivenTag(rootDir, eachTag);
				} catch (IOException | ParseException e) {
					e.printStackTrace();
				}
			}
			
		}
	}
	
	private static void createTagSetPerCluster(String rootDir) throws IOException {
		String fullPath = rootDir + "/generativeModel/clusters/";
		String[] fileNames = (new File(fullPath)).list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith("part-");
			}
		});
		Map<Integer, List<Integer>> clusterToDocIdMap = new HashMap<Integer, List<Integer>>();
		for(String fileName : fileNames) {
			String absolutePath = fullPath + fileName;
			System.out.println("Reading cluster file : " + fileName);
			try(BufferedReader reader = new BufferedReader(new FileReader(absolutePath))) {
				for(String line; (line = reader.readLine()) != null;) {
					String[] lineContent = line.split(",");
					Integer clusterId = Integer.parseInt(lineContent[0].substring(1));
					Integer docId = Integer.parseInt(lineContent[1].substring(0,lineContent[1].length()-1));
					if(!clusterToDocIdMap.containsKey(clusterId)) {	
						List<Integer> docList = new ArrayList<Integer>();
						clusterToDocIdMap.put(clusterId, docList);
					}
					clusterToDocIdMap.get(clusterId).add(docId);
				}
			}catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		QuestionSearcher searcher = new QuestionSearcher(rootDir + "/indexDir");
		for(Integer clusterId : clusterToDocIdMap.keySet()) {
			System.out.println("Creating Tag set for cluster : " + clusterId);
			ScoreDoc[] results = searcher.searchDocumentContentForDocIds(clusterToDocIdMap.get(clusterId));
			IndexSearcher indexSearcher = searcher.getIndexSearcher();
			Set<String> fieldsToLoad = new HashSet<String>();
			fieldsToLoad.add("qTag1");
			fieldsToLoad.add("qTag2");
			fieldsToLoad.add("qTag3");
			fieldsToLoad.add("qTag4");
			fieldsToLoad.add("qTag5");
			Set<String> tagSet = new HashSet<String>();
			for(ScoreDoc result : results) {
				Document doc = indexSearcher.doc(result.doc, fieldsToLoad);
				tagSet.add(doc.get("qTag1"));
				tagSet.add(doc.get("qTag2"));
				tagSet.add(doc.get("qTag3"));
				tagSet.add(doc.get("qTag4"));
				tagSet.add(doc.get("qTag5"));
			}
			FileWriter writer = null;
			BufferedWriter bufferedWriter = null;
			try {
				File clusterDataDir = new File(rootDir + "/clusterData");
				clusterDataDir.mkdirs();
				writer = new FileWriter(clusterDataDir.getAbsolutePath() + File.separator + "cluster_" + clusterId + ".txt");
				bufferedWriter = new BufferedWriter(writer);
				System.out.println("WRITING Cluster data: " + clusterId);
				for(String tag : tagSet) {
					bufferedWriter.write(tag + "\n");
				}
				bufferedWriter.close();
				writer.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		
		
	}

	public static void indexQuestionFiles(File inputDirectory) throws FileNotFoundException {
		
		QuestionXmlParser parser = new QuestionXmlParser();
		QuestionIndexer indexer = new QuestionIndexer(inputDirectory.getAbsolutePath() + File.separator + "indexDir");
		
		if(inputDirectory.isDirectory()) {
			int i=0;
			File dataDirectory = new File(inputDirectory.getAbsolutePath() + "/data");
			for(File inputFile : dataDirectory.listFiles()) {
				if(!inputFile.isDirectory()) {
					List<Question> questions = parser.parseXml(new FileInputStream(inputFile));
					System.out.println("PARSED : " + inputFile.getName());
					int testDataCount = (int) Math.floor(questions.size() * 0.1);
					int trainDataCount = questions.size() - testDataCount;
					
					try {
						indexer.indexQuestions(questions.subList(0, trainDataCount));
						System.out.println("INDEXED : " + inputFile.getName());	
					} catch (IOException e) {
						System.out.println("Error: could not write to index!!!");
						e.printStackTrace();
					}
					writeTestDataToFile(questions.subList(trainDataCount, questions.size()), inputDirectory, i++);
					System.out.println("Test Data : testQuestion_" + i + ".tsv");
				}
			}
		}
		indexer.close();
	}
	
	private static void writeTestDataToFile(List<Question> testQuestionList, File inputDirectory, int fileNum) {
		PrintWriter writer = null;
		try {
			File testDataDir = new File(inputDirectory.getAbsolutePath() + "/testData/");
			testDataDir.mkdirs();
			writer = new PrintWriter(testDataDir.getAbsolutePath() + File.separator + "testQuestion_" + fileNum + ".tsv");
			for(Question currentQuestion : testQuestionList) {
				String questionText = currentQuestion.getQuestionId() + "\t" + currentQuestion.getQuestionContent() + "\t" + currentQuestion.getQuestionTagsForTestData();
				writer.println(questionText);
			}
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public static void searchAndWriteQuestionsBasedOnGivenTag(String rootDir, String searchTag) throws IOException, ParseException {
		QuestionSearcher searcher = new QuestionSearcher(rootDir + "/indexDir", searchTag);
		ScoreDoc [] positiveResult = searcher.searchPositveQuestionsForGivenTag(searchTag);
		ScoreDoc [] negativeResult = searcher.searchNegativeQuestionsForGivenTag(searchTag, positiveResult.length);
		System.out.println("Total POSITIVE Hits for " + searchTag + " : " + positiveResult.length);
		System.out.println("Total NEGATIVE Hits for " + searchTag + " : " + negativeResult.length);
		IndexSearcher indexSearcher = searcher.getIndexSearcher();
		FileWriter writer = null;
		BufferedWriter bufferedWriter = null;
		try {
			File trainDataDir = new File(rootDir + "/trainData/" + searchTag);
			trainDataDir.mkdirs();
			writer = new FileWriter(trainDataDir.getAbsolutePath() + File.separator + searchTag + "_positive.tsv");
			bufferedWriter = new BufferedWriter(writer);
			System.out.println("WRITING positive: " + searchTag);
			for(ScoreDoc document : positiveResult) {
				Document currentDoc = indexSearcher.doc(document.doc);
				String documentContents = currentDoc.get("qId") + "\t" + currentDoc.get("qContent") + "\t" + searchTag;
				bufferedWriter.write(documentContents + "\n");
			}
			bufferedWriter.close();
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		try {
			File trainDataDir = new File(rootDir + "/trainData/" + searchTag);
			writer = new FileWriter(trainDataDir.getAbsolutePath() + File.separator + searchTag + "_negative.tsv");
			bufferedWriter = new BufferedWriter(writer);
			System.out.println("WRITING negative: " + searchTag);
			for(ScoreDoc document : negativeResult) {
				Document currentDoc = indexSearcher.doc(document.doc);
				String documentContents = currentDoc.get("qId") + "\t" + currentDoc.get("qContent") + "\tNOT_" + searchTag;
				bufferedWriter.write(documentContents + "\n");
			}
			bufferedWriter.close();
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
}
