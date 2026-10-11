package com.pulse.pass.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.pulse.pass.dto.request.RegisterUserRequest;
import com.pulse.pass.dto.response.UserResponse;
import com.pulse.pass.exception.DuplicateResourceException;
import com.pulse.pass.exception.ResourceNotFoundException;
import com.pulse.pass.service.UserService;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    private UserResponse user() {
        return new UserResponse(1L, "andrea", "andrea@mail.com", true, "Andrea", "Lopez",
                "3001112233", "Santa Marta", LocalDate.of(1995, 5, 20));
    }

    private static final String VALID_BODY = """
            {
              "username": "andrea",
              "email": "andrea@mail.com",
              "firstName": "Andrea",
              "lastName": "Lopez",
              "phone": "3001112233",
              "city": "Santa Marta",
              "birthDate": "1995-05-20"
            }
            """;

    @Test
    void TEST_CTRL_USR_001_register_valid_returns201() throws Exception {
        when(userService.register(any(RegisterUserRequest.class))).thenReturn(user());

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username").value("andrea"))
                .andExpect(jsonPath("$.email").value("andrea@mail.com"))
                .andExpect(jsonPath("$.active").value(true));

        verify(userService).register(any(RegisterUserRequest.class));
    }

    @Test
    void TEST_CTRL_USR_002_register_invalidEmail_returns400() throws Exception {
        String invalid = VALID_BODY.replace("andrea@mail.com", "not-an-email");

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details.email").exists());

        verify(userService, never()).register(any());
    }

    @Test
    void register_missingFields_returns400() throws Exception {
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.username").exists())
                .andExpect(jsonPath("$.details.firstName").exists())
                .andExpect(jsonPath("$.details.lastName").exists())
                .andExpect(jsonPath("$.details.birthDate").exists());

        verify(userService, never()).register(any());
    }

    @Test
    void TEST_CTRL_USR_003_register_duplicate_returns409() throws Exception {
        when(userService.register(any(RegisterUserRequest.class)))
                .thenThrow(new DuplicateResourceException("Username already exists."));

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Username already exists."));
    }

    @Test
    void TEST_CTRL_USR_004_findByEmail_returns200() throws Exception {
        when(userService.findByEmail("andrea@mail.com")).thenReturn(user());

        mockMvc.perform(get("/api/users/by-email").param("email", "andrea@mail.com"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.email").value("andrea@mail.com"));

        verify(userService).findByEmail("andrea@mail.com");
    }

    @Test
    void findByEmail_missing_returns404() throws Exception {
        when(userService.findByEmail("x@mail.com"))
                .thenThrow(new ResourceNotFoundException("User not found: x@mail.com"));

        mockMvc.perform(get("/api/users/by-email").param("email", "x@mail.com"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void TEST_CTRL_USR_005_findByUsername_returns200() throws Exception {
        when(userService.findByUsername("andrea")).thenReturn(user());

        mockMvc.perform(get("/api/users/by-username").param("username", "andrea"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("andrea"));

        verify(userService).findByUsername("andrea");
    }

    @Test
    void findByUsername_missing_returns404() throws Exception {
        when(userService.findByUsername("ghost"))
                .thenThrow(new ResourceNotFoundException("User not found: ghost"));

        mockMvc.perform(get("/api/users/by-username").param("username", "ghost"))
                .andExpect(status().isNotFound());
    }
}
