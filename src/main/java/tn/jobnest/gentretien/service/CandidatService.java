package tn.jobnest.gentretien.service;

import tn.jobnest.gentretien.model.Candidat;
import java.sql.*;

public class CandidatService {
    private Connection cnx;

    public CandidatService() {
        try {
            cnx = DriverManager.getConnection("jdbc:mysql://localhost:3306/jobnest", "root", "");
        } catch (SQLException e) {
            System.err.println("Erreur Connexion BDD : " + e.getMessage());
        }
    }

    public Candidat getCandidatById(int idCandidat) {
        // On sélectionne les colonnes de 'c' (candidat) en filtrant par l'ID reçu
        String query = "SELECT c.* FROM candidat c " +
                "JOIN candidature ca ON c.id_user = ca.id_candidat " +
                "WHERE c.id_user = ?";

        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setInt(1, idCandidat);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                return new Candidat(
                        rs.getInt("id_user"),
                        rs.getString("nom"),
                        rs.getString("prenom"),
                        rs.getString("email"),
                        rs.getString("telephone"),
                        rs.getString("ville"),
                        rs.getString("Titre_pro"),
                        rs.getString("bio"),
                        rs.getString("image"),
                        rs.getString("DateN")
                );
            }
        } catch (SQLException e) {
            System.err.println("Erreur SQL lors du JOIN : " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}