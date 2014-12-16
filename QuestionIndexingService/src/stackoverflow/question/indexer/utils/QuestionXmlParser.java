package stackoverflow.question.indexer.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import stackoverflow.question.indexer.model.Question;

public class QuestionXmlParser {
	
	public List<Question> parseXml(InputStream in) {
		
		List<Question> currentQuestionList = new ArrayList<Question>();
		try {
			QuestionsSAXHandler SAXHandler = new QuestionsSAXHandler();

			XMLReader parser = XMLReaderFactory.createXMLReader();
			
			parser.setContentHandler(SAXHandler);

			InputSource source = new InputSource(in);
			
			parser.parse(source);
			
			currentQuestionList = SAXHandler.getQuestions();

		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {

		}
		
		return currentQuestionList;
	}
}
