package com.example.doktoribackend.security;

import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.exception.UserNotFoundException;
import com.example.doktoribackend.user.domain.User;
import com.example.doktoribackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        long userId;
        try {
            userId = Long.parseLong(username);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.INVALID_USER_ID);
        }

        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(UserNotFoundException::new);

        return CustomUserDetails.from(user);
    }
}

