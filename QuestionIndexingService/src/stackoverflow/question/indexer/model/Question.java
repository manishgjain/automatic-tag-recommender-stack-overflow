package stackoverflow.question.indexer.model;

import java.util.Set;

public class Question {
	private Integer questionId;
	private String questionTitle;
	private String questionBody;
	private Set<String> questionTags;
	
	public Integer getQuestionId() {
		return questionId;
	}
	
	public void setQuestionId(Integer qId) {
		this.questionId = qId;
	}
	
	public Set<String> getQuestionTags() {
		return questionTags;
	}
	
	public String getQuestionTagsForTestData() {
		StringBuilder tagString = new StringBuilder();
		for(String tag : questionTags) {
			tagString.append(tag + ",");
		}
		return tagString.substring(0, tagString.length()-1);
	}
	
	public void setQuestionTags(Set<String> qTags) {
		this.questionTags = qTags;
	}
	
	//Returns the Question title and body concatenated as contents.
	public String getQuestionContent() {
		return questionTitle + " " + questionBody;
	}
	
	public void setQuestionTitle(String qTitle) {
		this.questionTitle = qTitle;
	}
	
	public void setQuestionBody(String qBody) {
		this.questionBody = qBody;
	}
	
	@Override
	public String toString() {
		return this.questionId + ":" + this.questionTitle + "\n";
	}


}
