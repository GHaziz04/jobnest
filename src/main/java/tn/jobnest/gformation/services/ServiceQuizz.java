package tn.jobnest.gformation.services;

// Importation basée sur ta structure réelle : package repository
import tn.jobnest.gformation.repository.DataSource;
import tn.jobnest.gformation.model.QuizQuestion;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class ServiceQuizz {

    // Récupération de la connexion via ton Singleton DataSource
    private final Connection conn = DataSource.getInstance().getConnection();

    /**
     * Crée l'entrée parente dans la table 'quizz' et récupère l'ID généré.
     * @param idRessource l'ID de la ressource associée
     * @param titre le titre du quiz
     * @return l'ID du quiz créé ou -1 en cas d'échec
     */
    public int creerQuizz(int idRessource, String titre) throws SQLException {
        if (conn == null) throw new SQLException("La connexion à la base de données est nulle.");

        String sql = "INSERT INTO quizz (id_ressource, titre, score_reussite) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, idRessource);
            ps.setString(2, titre);
            ps.setInt(3, 70); // Score de réussite par défaut
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return -1;
    }

    /**
     * Insère une question liée à un quiz spécifique.
     */
    public void ajouterQuestion(int idQuizz, String texte, String o1, String o2, String o3, int correct) {
        String sql = "INSERT INTO question (id_quizz, texte_question, option1, option2, option3, reponse_correcte) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idQuizz);
            ps.setString(2, texte);
            ps.setString(3, o1);
            ps.setString(4, o2);
            ps.setString(5, o3);
            ps.setInt(6, correct);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur SQL lors de l'ajout d'une question : " + e.getMessage());
        }
    }

    /**
     * Récupère la liste complète des questions pour un quiz donné (utile pour le Front-Office).
     */
    public List<QuizQuestion> getQuizComplet(int idRessource) {
        List<QuizQuestion> questions = new ArrayList<>();
        String sql = "SELECT q.* FROM question q " +
                "JOIN quizz qz ON q.id_quizz = qz.id_quizz " +
                "WHERE qz.id_ressource = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idRessource);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // Création de la liste d'options
                    List<String> options = new ArrayList<>();
                    options.add(rs.getString("option1"));
                    options.add(rs.getString("option2"));
                    options.add(rs.getString("option3"));

                    // Récupération du texte de la réponse correcte via l'index (1, 2 ou 3)
                    int indexCorrect = rs.getInt("reponse_correcte");
                    String correctText = (indexCorrect > 0 && indexCorrect <= options.size())
                            ? options.get(indexCorrect - 1)
                            : "";

                    questions.add(new QuizQuestion(rs.getString("texte_question"), options, correctText));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération du quiz : " + e.getMessage());
            e.printStackTrace();
        }
        return questions;
    }
}