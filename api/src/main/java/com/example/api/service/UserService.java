package com.example.api.service;

import java.util.List;
import com.example.api.model.User;

public interface UserService {

    List<User> getUsers();

    User getUserById(String theId);

    User saveUser(User theUser);

    void deleteUser(String theId);
}
