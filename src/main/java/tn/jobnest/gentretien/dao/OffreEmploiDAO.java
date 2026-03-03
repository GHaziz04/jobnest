package tn.jobnest.gentretien.dao;

import tn.jobnest.gentretien.utils.MyDatabase;
import tn.jobnest.gentretien.model.OffreEmploi;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OffreEmploiDAO {

    private final Connection connection = MyDatabase.getInstance().getConn();

    public int ajouterEtRetournerId(OffreEmploi o) throws SQLException {

        String sql = "INSERT INTO offre_emploi " +
                "(id_recruteur, titre, description, entreprise, type_contrat, " +
                " salaire_min, salaire_max, niveau_experience, nb_postes, " +
                " date_publication, date_expiration, statut, matching_score) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";

        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, o.getIdRecruteur());
            ps.setString(2, o.getTitre());
            ps.setString(3, o.getDescription());
            ps.setString(4, o.getEntreprise());
            ps.setString(5, o.getTypeContrat());
            ps.setDouble(6, o.getSalaireMin());
            ps.setDouble(7, o.getSalaireMax());
            ps.setString(8, o.getNiveauExperience());
            ps.setInt(9, o.getNbPostes());
            ps.setDate(10, o.getDatePublication());
            ps.setDate(11, o.getDateExpiration());
            ps.setString(12, o.getStatut());

            if (o.getMatchingScore() != null)
                ps.setDouble(13, o.getMatchingScore());
            else
                ps.setNull(13, Types.DOUBLE);

            int rows = ps.executeUpdate();
            if (rows == 0) throw new SQLException("Échec insertion offre");

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
            throw new SQLException("ID non généré");
        }
    }

    public List<OffreEmploi> afficher() throws SQLException {

        List<OffreEmploi> list = new ArrayList<>();
        String sql = "SELECT * FROM offre_emploi";

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public void modifier(OffreEmploi o) throws SQLException {

        String sql = "UPDATE offre_emploi " +
                "SET titre=?, description=?, entreprise=?, type_contrat=?, " +
                "    salaire_min=?, salaire_max=?, niveau_experience=?, nb_postes=?, " +
                "    date_publication=?, date_expiration=?, statut=?, matching_score=? " +
                "WHERE id_offre=?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, o.getTitre());
            ps.setString(2, o.getDescription());
            ps.setString(3, o.getEntreprise());
            ps.setString(4, o.getTypeContrat());
            ps.setDouble(5, o.getSalaireMin());
            ps.setDouble(6, o.getSalaireMax());
            ps.setString(7, o.getNiveauExperience());
            ps.setInt(8, o.getNbPostes());
            ps.setDate(9, o.getDatePublication());
            ps.setDate(10, o.getDateExpiration());
            ps.setString(11, o.getStatut());

            if (o.getMatchingScore() != null)
                ps.setDouble(12, o.getMatchingScore());
            else
                ps.setNull(12, Types.DOUBLE);

            ps.setInt(13, o.getIdOffre());

            int rows = ps.executeUpdate();
            if (rows == 0) throw new SQLException("Modification échouée : offre introuvable");
        }
    }

    public void supprimer(int id) throws SQLException {

        String sql = "DELETE FROM offre_emploi WHERE id_offre=?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            int rows = ps.executeUpdate();
            if (rows == 0) throw new SQLException("Suppression échouée : offre introuvable");
        }
    }

    public OffreEmploi getById(int id) throws SQLException {

        String sql = "SELECT * FROM offre_emploi WHERE id_offre=?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    private OffreEmploi mapRow(ResultSet rs) throws SQLException {

        OffreEmploi o = new OffreEmploi();
        o.setIdOffre(rs.getInt("id_offre"));
        o.setIdRecruteur(rs.getInt("id_recruteur"));
        o.setTitre(rs.getString("titre"));
        o.setDescription(rs.getString("description"));
        o.setEntreprise(rs.getString("entreprise"));
        o.setTypeContrat(rs.getString("type_contrat"));
        o.setSalaireMin(rs.getDouble("salaire_min"));
        o.setSalaireMax(rs.getDouble("salaire_max"));
        o.setNiveauExperience(rs.getString("niveau_experience"));
        o.setNbPostes(rs.getInt("nb_postes"));
        o.setDatePublication(rs.getDate("date_publication"));
        o.setDateExpiration(rs.getDate("date_expiration"));
        o.setStatut(rs.getString("statut"));
        o.setNbVues(rs.getInt("nb_vues"));
        o.setNbCandidatures(rs.getInt("nb_candidatures"));

        double score = rs.getDouble("matching_score");
        if (!rs.wasNull()) o.setMatchingScore(score);

        return o;
    }
}