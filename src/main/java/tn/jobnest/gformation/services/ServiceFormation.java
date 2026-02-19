package tn.jobnest.gformation.services;

import tn.jobnest.gformation.model.Formation;
import tn.jobnest.gformation.repository.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ServiceFormation {

    // 1. RÉCUPÉRER TOUTES LES FORMATIONS (Vue globale)
    public List<Formation> recupererTout() {
        List<Formation> formations = new ArrayList<>();
        String query = "SELECT f.*, fr.nom_formateur FROM formation f " +
                "LEFT JOIN formateur fr ON f.id_user = fr.id_user";

        try (Connection conn = DataSource.getInstance().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                formations.add(mapperFormation(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur SQL (RecupererTout) : " + e.getMessage());
        }
        return formations;
    }

    // 2. AJOUTER UNE FORMATION
    public void ajouter(Formation f) {
        String query = "INSERT INTO formation (id_user, titre, description, objectifs, niveau, duree_heures, prix, nb_places, nb_places_occupees, date_debut, date_fin, statut, lieu, url_image) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        try (Connection conn = DataSource.getInstance().getConnection();
             PreparedStatement pst = conn.prepareStatement(query)) {

            pst.setInt(1, 1); // ID statique pour les tests
            pst.setString(2, f.getTitre());
            pst.setString(3, f.getDescription());
            pst.setString(4, f.getObjectifs());
            pst.setString(5, f.getNiveau());
            pst.setInt(6, f.getDuree_heures());
            pst.setDouble(7, f.getPrix());
            pst.setInt(8, f.getNb_places());
            pst.setInt(9, 0);
            pst.setDate(10, f.getDate_debut());
            pst.setDate(11, f.getDate_fin());
            pst.setString(12, f.getStatut());
            pst.setString(13, f.getLieu());
            pst.setString(14, f.getUrl_image());

            pst.executeUpdate();
            System.out.println("✅ Formation ajoutée.");
        } catch (SQLException e) {
            System.err.println("❌ Erreur SQL (Ajouter) : " + e.getMessage());
        }
    }

    // 3. MODIFIER UNE FORMATION
    public void modifier(Formation f) {
        String query = "UPDATE formation SET titre=?, description=?, objectifs=?, niveau=?, duree_heures=?, prix=?, nb_places=?, date_debut=?, date_fin=?, statut=?, lieu=?, url_image=? WHERE id_formation=?";

        try (Connection conn = DataSource.getInstance().getConnection();
             PreparedStatement pst = conn.prepareStatement(query)) {

            pst.setString(1, f.getTitre());
            pst.setString(2, f.getDescription());
            pst.setString(3, f.getObjectifs());
            pst.setString(4, f.getNiveau());
            pst.setInt(5, f.getDuree_heures());
            pst.setDouble(6, f.getPrix());
            pst.setInt(7, f.getNb_places());
            pst.setDate(8, f.getDate_debut());
            pst.setDate(9, f.getDate_fin());
            pst.setString(10, f.getStatut());
            pst.setString(11, f.getLieu());
            pst.setString(12, f.getUrl_image());
            pst.setInt(13, f.getId_formation());

            pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Erreur SQL (Modifier) : " + e.getMessage());
        }
    }

    // 4. SUPPRIMER UNE FORMATION
    public void supprimer(int id) {
        String query = "DELETE FROM formation WHERE id_formation = ?";
        try (Connection conn = DataSource.getInstance().getConnection();
             PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setInt(1, id);
            pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Erreur SQL (Supprimer) : " + e.getMessage());
        }
    }

    // --- LOGIQUE DE STATUT AUTOMATIQUE ---
    private String calculerStatutAutomatique(Formation f) {
        LocalDate aujourdhui = LocalDate.now();

        // Sécurité si les dates sont nulles
        if (f.getDate_fin() == null || f.getDate_debut() == null) return f.getStatut();

        LocalDate fin = f.getDate_fin().toLocalDate();

        // 1. Règle de temps : Si la date de fin est dépassée
        if (fin.isBefore(aujourdhui)) {
            return "Terminé";
        }

        // 2. Règle de capacité : Si c'est plein
        if (f.getNb_places_occupees() >= f.getNb_places() && f.getNb_places() > 0) {
            return "Complet";
        }

        // Sinon, on garde le statut actuel (Ouvert ou Programmée)
        return f.getStatut();
    }

    // 5. TOTAL DES FORMATIONS
    public int getTotalFormations() {
        int total = 0;
        String query = "SELECT COUNT(*) FROM formation";
        try (Connection conn = DataSource.getInstance().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            if (rs.next()) total = rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return total;
    }

    // 6. STATISTIQUES POUR LA COURBE (3 derniers mois)
    public Map<String, Integer> getStatsCreationsTroisMois() {
        Map<String, Integer> stats = new LinkedHashMap<>();
        String query = "SELECT MONTHNAME(date_debut) as mois, COUNT(*) as total " +
                "FROM formation " +
                "WHERE date_debut >= DATE_SUB(CURDATE(), INTERVAL 3 MONTH) " +
                "GROUP BY MONTH(date_debut), mois " +
                "ORDER BY date_debut ASC";

        try (Connection conn = DataSource.getInstance().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                stats.put(rs.getString("mois"), rs.getInt("total"));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur SQL (Stats Courbe) : " + e.getMessage());
        }
        return stats;
    }

    // 7. HISTORIQUE PAR ID
    public List<Formation> getHistoriqueFormateur(int idFormateur) {
        List<Formation> liste = new ArrayList<>();
        String query = "SELECT f.*, fr.nom_formateur FROM formation f " +
                "LEFT JOIN formateur fr ON f.id_user = fr.id_user " +
                "WHERE f.id_user = ?";

        try (Connection conn = DataSource.getInstance().getConnection();
             PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setInt(1, idFormateur);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                liste.add(mapperFormation(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur SQL (Historique ID) : " + e.getMessage());
        }
        return liste;
    }

    // 8. CALCULER LE TAUX D'OCCUPATION TOTAL
    public double getTauxOccupationMoyen() {
        String query = "SELECT AVG((nb_places_occupees / nb_places) * 100) FROM formation WHERE nb_places > 0";
        try (Connection conn = DataSource.getInstance().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    // MÉTHODE UTILITAIRE (Mapping ResultSet -> Objet)
    private Formation mapperFormation(ResultSet rs) throws SQLException {
        Formation f = new Formation();
        f.setId_formation(rs.getInt("id_formation"));
        f.setTitre(rs.getString("titre"));
        f.setDescription(rs.getString("description"));
        f.setPrix(rs.getDouble("prix"));
        f.setDuree_heures(rs.getInt("duree_heures"));
        f.setNb_places(rs.getInt("nb_places"));
        f.setNb_places_occupees(rs.getInt("nb_places_occupees"));
        f.setNiveau(rs.getString("niveau"));
        f.setLieu(rs.getString("lieu"));
        f.setUrl_image(rs.getString("url_image"));
        f.setObjectifs(rs.getString("objectifs"));
        f.setDate_debut(rs.getDate("date_debut"));
        f.setDate_fin(rs.getDate("date_fin"));

        // ON APPLIQUE LA LOGIQUE DE STATUT AUTOMATIQUE ICI
        f.setStatut(calculerStatutAutomatique(f));

        try {
            String nomF = rs.getString("nom_formateur");
            f.setNomFormateur(nomF != null ? nomF : "Anonyme");
        } catch (SQLException e) {
            f.setNomFormateur("Non spécifié");
        }
        return f;
    }
}