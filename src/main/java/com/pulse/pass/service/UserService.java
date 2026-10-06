package com.pulse.pass.service;

import com.pulse.pass.dto.request.RegisterUserRequest;
import com.pulse.pass.dto.response.UserResponse;

public interface UserService {

    UserResponse register(RegisterUserRequest request);

    UserResponse findByEmail(String email);

    UserResponse findByUsername(String username);
}
