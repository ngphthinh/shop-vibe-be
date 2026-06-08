package org.ngphthinh.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.ngphthinh.dto.request.user.UserCreateRequest;
import org.ngphthinh.dto.request.user.UserUpdateRequest;
import org.ngphthinh.dto.response.user.UserResponse;
import org.ngphthinh.entity.Role;
import org.ngphthinh.entity.User;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = org.mapstruct.NullValuePropertyMappingStrategy.IGNORE)

public interface UserMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "address", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "isLocked", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "cart", ignore = true)
    @Mapping(target = "orders", ignore = true)
    @Mapping(target = "reviews", ignore = true)
    User toUser(UserCreateRequest userCreateRequest);

    @Mapping(target = "roles", source = "roles", qualifiedByName = "mapRoles")
    UserResponse toUserResponse(User user);

    // dùng để map cho các trường roles trong UserResponse, lấy id của Role để trả về
    @Named("mapRoles")
    default List<String> mapRoles(Set<Role> roles) {
        if (roles == null) {
            return null;
        }
        // role id == role name
        return roles.stream()
                .map(Role::getId)
                .collect(Collectors.toList());
    }


    void updateUserFromUserResponse(UserUpdateRequest request, @MappingTarget User user);

}
