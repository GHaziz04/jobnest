package tn.jobnest.gentretien.service;
import java.sql.SQLException;
import java.util.List;
public interface Icrud<T> {
    void ajouter(T t) throws SQLException;
    void update(T t) throws SQLException;
    void delete(int id) throws SQLException;
    List<T> afficher() throws SQLException;
}
