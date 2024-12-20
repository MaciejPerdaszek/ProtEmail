package com.example.api.service;

import java.util.List;
import java.util.Optional;
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
    public User getUserById(String theId) {
        return userRepository.findById(theId).orElseThrow(() -> new UserNotFound("Did not find user id - " + theId));
    }

    @Override
    public User saveUser(User theUser) {
        Optional<User> existingUser = userRepository.findById(theUser.getId());
        if (existingUser.isPresent()) {
            throw new UserAlreadyExistException("User with uid" + theUser.getId() + " already exists");
        }
        return userRepository.save(theUser);
    }

    @Override
    public void deleteUser(String theId) {
        userRepository.deleteById(theId);
    }
}
