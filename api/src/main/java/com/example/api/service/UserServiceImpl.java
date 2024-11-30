package com.example.api.service;

import java.util.List;
import com.example.api.exception.UserAlreadyExistException;
import com.example.api.exception.UserNotFound;
import com.example.api.model.User;
import com.example.api.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public List<User> getUsers() {
        return userRepository.findAll();
    }

    @Override
    public User getUserById(long theId) {
        return userRepository.findById(theId).orElseThrow(() -> new UserNotFound("Did not find user id - " + theId));
    }

    @Override
    public User saveUser(User theUser) {
        User existingUser = userRepository.findByEmail(theUser.getEmail());
        if (existingUser != null) {
            throw new UserAlreadyExistException("User with email " + theUser.getEmail() + " already exists");
        }
        return userRepository.save(theUser);
    }

    @Override
    public void deleteUser(long theId) {
        userRepository.deleteById(theId);
    }
}
