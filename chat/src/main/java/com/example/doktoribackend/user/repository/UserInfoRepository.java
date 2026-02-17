package com.example.doktoribackend.user.repository;

import com.example.doktoribackend.user.domain.UserInfo;
import org.springframework.data.repository.Repository;

import java.util.Optional;

public interface UserInfoRepository extends Repository<UserInfo, Long> {

    Optional<UserInfo> findById(Long id);
}
