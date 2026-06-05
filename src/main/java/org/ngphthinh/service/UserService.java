package org.ngphthinh.service;

import lombok.RequiredArgsConstructor;
import org.ngphthinh.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class UserService {
    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void lockUser(String email) {
        userRepository.updateIsLockedByEmail(true, false, email);
    }

}
