package com.dao;

import com.model.User;
import java.util.List;

public interface UserDAO {
    void ajouter(User user);
    User login(String email, String password);
    List<User> getAll();
    void supprimer(User user);
}
