package tn.jobnest.gformation.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class QuizQuestion {

    // Ajout des IDs pour la persistance en Base de Données
    private int id_question;
    private int id_ressource;

    private String question;
    private List<String> options;

    @JsonProperty("reponseCorrecte")
    private String reponseCorrecte;

    // Constructeur vide (Indispensable pour Jackson)
    public QuizQuestion() {}

    public QuizQuestion(String question, List<String> options, String reponseCorrecte) {
        this.question = question;
        this.options = options;
        this.reponseCorrecte = reponseCorrecte;
    }

    // --- Getters et Setters ---

    public int getId_question() { return id_question; }
    public void setId_question(int id_question) { this.id_question = id_question; }

    public int getId_ressource() { return id_ressource; }
    public void setId_ressource(int id_ressource) { this.id_ressource = id_ressource; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public List<String> getOptions() { return options; }
    public void setOptions(List<String> options) { this.options = options; }

    public String getReponseCorrecte() { return reponseCorrecte; }

    @JsonProperty("reponseCorrecte")
    public void setReponseCorrecte(String reponseCorrecte) {
        this.reponseCorrecte = reponseCorrecte;
    }

    @JsonProperty("correctAnswer")
    public void setCorrectAnswer(String correctAnswer) {
        this.reponseCorrecte = correctAnswer;
    }
}