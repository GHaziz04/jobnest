package tn.jobnest.gentretien.dao;

import tn.jobnest.gentretien.utils.MyDatabase;
import tn.jobnest.gentretien.model.OffreEmploi;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OffreEmploiDAO {

    private Connection getConn() {
        return MyDatabase.getInstance().getConn();
    }

    // ─────────────────────────────────────────────
    //  AJOUTER + retourner l'id généré
    // ─────────────────────────────────────────────
    public int ajouterEtRetournerId(OffreEmploi o) throws SQLException {
        String sql = "INSERT INTO offre_emploi " +
                "(id_recruteur, titre, description, entreprise, type_contrat, " +
                " salaire_min, salaire_max, niveau_experience, nb_postes, " +
                " date_publication, date_expiration, statut) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";

        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt   (1,  o.getIdRecruteur());
            ps.setString(2,  o.getTitre());
            ps.setString(3,  o.getDescription());
            ps.setString(4,  o.getEntreprise());
            ps.setString(5,  o.getTypeContrat());

            if (o.getSalaireMin() > 0) ps.setDouble(6, o.getSalaireMin());
            else                        ps.setNull  (6, Types.DECIMAL);

            if (o.getSalaireMax() > 0) ps.setDouble(7, o.getSalaireMax());
            else                        ps.setNull  (7, Types.DECIMAL);

            ps.setString(8, o.getNiveauExperience());

            if (o.getNbPostes() > 0) ps.setInt (9, o.getNbPostes());
            else                      ps.setNull(9, Types.INTEGER);

            ps.setDate  (10, o.getDatePublication());
            ps.setDate  (11, o.getDateExpiration());
            ps.setString(12, o.getStatut() != null ? o.getStatut() : "publiee");
            // ✅ matching_score supprimé

            int rows = ps.executeUpdate();
            if (rows == 0) throw new SQLException("Échec insertion offre");

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
            throw new SQLException("ID non généré");
        }
    }

    // ─────────────────────────────────────────────
    //  LIRE TOUTES LES OFFRES
    // ─────────────────────────────────────────────
    public List<OffreEmploi> afficher() throws SQLException {
        List<OffreEmploi> list = new ArrayList<>();
        String sql = "SELECT * FROM offre_emploi ORDER BY id_offre DESC";
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    // ─────────────────────────────────────────────
    //  MODIFIER
    // ─────────────────────────────────────────────
    public void modifier(OffreEmploi o) throws SQLException {
        String sql = "UPDATE offre_emploi " +
                "SET titre=?, description=?, entreprise=?, type_contrat=?, " +
                "    salaire_min=?, salaire_max=?, niveau_experience=?, nb_postes=?, " +
                "    date_publication=?, date_expiration=?, statut=? " +
                "WHERE id_offre=?";

        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1,  o.getTitre());
            ps.setString(2,  o.getDescription());
            ps.setString(3,  o.getEntreprise());
            ps.setString(4,  o.getTypeContrat());

            if (o.getSalaireMin() > 0) ps.setDouble(5, o.getSalaireMin());
            else                        ps.setNull  (5, Types.DECIMAL);

            if (o.getSalaireMax() > 0) ps.setDouble(6, o.getSalaireMax());
            else                        ps.setNull  (6, Types.DECIMAL);

            ps.setString(7, o.getNiveauExperience());

            if (o.getNbPostes() > 0) ps.setInt (8, o.getNbPostes());
            else                      ps.setNull(8, Types.INTEGER);

            ps.setDate  (9,  o.getDatePublication());
            ps.setDate  (10, o.getDateExpiration());
            ps.setString(11, o.getStatut() != null ? o.getStatut() : "publiee");
            ps.setInt   (12, o.getIdOffre());
            // ✅ matching_score supprimé

            if (ps.executeUpdate() == 0)
                throw new SQLException("Modification échouée : offre introuvable");
        }
    }

    // ─────────────────────────────────────────────
    //  SUPPRIMER
    // ─────────────────────────────────────────────
    public void supprimer(int id) throws SQLException {
        try (PreparedStatement ps = getConn().prepareStatement(
                "DELETE FROM offre_emploi WHERE id_offre=?")) {
            ps.setInt(1, id);
            if (ps.executeUpdate() == 0)
                throw new SQLException("Suppression échouée : offre introuvable");
        }
    }

    // ─────────────────────────────────────────────
    //  MAPPING ResultSet → OffreEmploi
    // ─────────────────────────────────────────────
    private OffreEmploi mapRow(ResultSet rs) throws SQLException {
        OffreEmploi o = new OffreEmploi();
        o.setIdOffre         (rs.getInt   ("id_offre"));
        o.setIdRecruteur     (rs.getInt   ("id_recruteur"));
        o.setTitre           (rs.getString("titre"));
        o.setDescription     (rs.getString("description"));
        o.setEntreprise      (rs.getString("entreprise"));
        o.setTypeContrat     (rs.getString("type_contrat"));
        o.setSalaireMin      (rs.getDouble("salaire_min"));
        o.setSalaireMax      (rs.getDouble("salaire_max"));
        o.setNiveauExperience(rs.getString("niveau_experience"));
        o.setNbPostes        (rs.getInt   ("nb_postes"));
        o.setDatePublication (rs.getDate  ("date_publication"));
        o.setDateExpiration  (rs.getDate  ("date_expiration"));
        o.setStatut          (rs.getString("statut"));
        o.setNbVues          (rs.getInt   ("nb_vues"));
        o.setNbCandidatures  (rs.getInt   ("nb_candidatures"));
        // ✅ matching_score supprimé
        return o;
    }
}