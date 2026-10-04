package com.pulse.pass.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.pulse.pass.domain.User;
import com.pulse.pass.dto.response.UserResponse;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "firstName", source = "profile.firstName")
    @Mapping(target = "lastName", source = "profile.lastName")
    @Mapping(target = "phone", source = "profile.phone")
    @Mapping(target = "city", source = "profile.city")
    @Mapping(target = "birthDate", source = "profile.birthDate")
    UserResponse toResponse(User user);
}
