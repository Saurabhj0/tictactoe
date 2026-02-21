package com.example.tictactoe_global.controller;

import com.example.tictactoe_global.model.User;
import com.example.tictactoe_global.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @PostMapping("/register")
    public User register(@RequestBody String displayName) {
        // Clean up quotes if sent as raw string
        displayName = displayName.replace("\"", "");
        String finalDisplayName = displayName;
        return userRepository.findByDisplayName(displayName)
                .orElseGet(() -> userRepository.save(new User(finalDisplayName)));
    }

    @GetMapping("/{displayName}")
    public User getUser(@PathVariable String displayName) {
        return userRepository.findByDisplayName(displayName).orElse(null);
    }
}
