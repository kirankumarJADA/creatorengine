package com.creatorengine.aifaq.entity;

/**
 * A single Question/Answer pair the creator has curated. Checked first
 * (closest match wins) before falling back to the free-text knowledge base.
 */
public class QaPair {

    private String question;
    private String answer;

    public QaPair() {}

    public QaPair(String question, String answer) {
        this.question = question;
        this.answer = answer;
    }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
}
