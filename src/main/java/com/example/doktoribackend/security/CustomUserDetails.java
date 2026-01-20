package com.example.doktoribackend.security;

import com.example.doktoribackend.user.domain.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class CustomUserDetails implements UserDetails {

    private final Long id;
    private final String email;
    private final String nickname;
    private final Collection<? extends GrantedAuthority> authorities;

    private CustomUserDetails(
            Long id,
            String email,
            String nickname,
            Collection<? extends GrantedAuthority> authorities
    ) {
        this.id = id;
        this.email = email;
        this.nickname = nickname;
        this.authorities = authorities;
    }

    public static CustomUserDetails from(User user) {
        String roleName = user.getRole().name();
        List<GrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority(roleName));

        return new CustomUserDetails(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                authorities
        );
    }

    public static CustomUserDetails fromClaims(Long id, String email, String nickname, String roleName) {
        List<GrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority(roleName));

        return new CustomUserDetails(id, email, nickname, authorities);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
