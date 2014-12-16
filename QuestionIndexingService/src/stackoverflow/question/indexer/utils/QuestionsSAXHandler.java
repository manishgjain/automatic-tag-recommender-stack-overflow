package stackoverflow.question.indexer.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import stackoverflow.question.indexer.model.Question;

public class QuestionsSAXHandler extends DefaultHandler {
	
	private List<Question> questionList = new ArrayList<Question>();
	private Stack<String> elementStack = new Stack<String>();
	private Stack<Question> questionStack = new Stack<Question>();
	
	public void startDocument() throws SAXException {
//		System.out.println("Question : Started.");
	}
	
	public void endDocument() throws SAXException {
//		System.out.println("Question : Ended.");
	}
	
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
    	//Push it in element stack
        this.elementStack.push(qName);

        //If this is start of 'user' element then prepare a new User instance and push it in object stack
        if ("question".equals(qName))
        {
        	Question newQuestion = new Question();
            
            this.questionStack.push(newQuestion);
        }
    }
	
	public void endElement(String uri, String localName, String qName) throws SAXException {	

		this.elementStack.pop();
        if ("question".equals(qName))
        {
            Question question = this.questionStack.pop();
            this.questionList.add(question);
        }
    }
	
    public void characters(char[] charArray, int start, int length) throws SAXException {
        
    	String value = new String(charArray, start, length).trim();

        if (value.length() == 0) {
        	// ignore white space
            return;
        }
        
        //handle the value based on to which element it belongs
        if ("Id".equals(currentElement())) {
            Question currentQuestion = this.questionStack.peek();
            currentQuestion.setQuestionId(Integer.parseInt(value));
        }
        else if ("Title".equals(currentElement())) {
            Question currentQuestion = this.questionStack.peek();
            currentQuestion.setQuestionTitle(value);
        }
        else if ("Body".equals(currentElement())) {
            Question currentQuestion = this.questionStack.peek();
            currentQuestion.setQuestionBody(value);
        }
        else if ("Tags".equals(currentElement())) {
            Question currentQuestion = this.questionStack.peek();
            Set<String> qTags = new HashSet<String>();
            String[] tagsArray = value.split(",");
            for(String currentTag : tagsArray) {
            	if(!currentTag.isEmpty())
            		qTags.add(currentTag);
            }
            currentQuestion.setQuestionTags(qTags);
        }
    }
    
    private String currentElement() {
        return this.elementStack.peek();
    }
    
    public List<Question> getQuestions()
    {
    	return questionList;
    }


	
	
}
