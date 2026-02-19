package com.service;

import com.dao.UserDAO;
import com.dao.UserDAOImpl;
import com.model.User;
import java.util.List;

public class UserService {

    private UserDAO userDAO = new UserDAOImpl();

    public void inscrire(User user) {
        userDAO.ajouter(user);
    }

    public User login(String email, String password) {
        return userDAO.login(email, password);
    }

    public List<User> afficherTous() {
        return userDAO.getAll();
    }

    public void supprimer(User user) {
        userDAO.supprimer(user);
    }
}
