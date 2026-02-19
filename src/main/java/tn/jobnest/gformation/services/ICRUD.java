package tn.jobnest.gformation.services;

import java.util.List;

public interface ICRUD<T> {
    void ajouter(T t);
    List<T> recupererTout();
    void modifier(T t, int id);
    void supprimer(int id);
}