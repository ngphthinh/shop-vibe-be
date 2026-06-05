package org.ngphthinh.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ngphthinh.entity.Role;
import org.ngphthinh.enums.RoleName;
import org.ngphthinh.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class InitData implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) throws Exception {
        List<Role> roles = List.of(Role.builder()
                        .id(RoleName.ROLE_ADMIN.name())
                        .name(RoleName.ROLE_ADMIN)
                        .build(),
                Role.builder()
                        .id(RoleName.ROLE_USER.name())
                        .name(RoleName.ROLE_USER)
                        .build()
        );


        if (roleRepository.count() == 0) {
            roleRepository.saveAll(roles);
            log.info("Initialized roles: {}", roles);
        }

    }
}
