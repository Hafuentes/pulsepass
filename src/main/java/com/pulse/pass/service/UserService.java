package com.pulse.pass.service;

import org.springframework.security.access.prepost.PreAuthorize;

import com.pulse.pass.dto.request.RegisterUserRequest;
import com.pulse.pass.dto.response.UserResponse;

public interface UserService {

    UserResponse register(RegisterUserRequest request);

    @PreAuthorize("@authz.canAccessUserByEmail(#email)")
    UserResponse findByEmail(String email);

    @PreAuthorize("@authz.canAccessUserByUsername(#username)")
    UserResponse findByUsername(String username);
}
