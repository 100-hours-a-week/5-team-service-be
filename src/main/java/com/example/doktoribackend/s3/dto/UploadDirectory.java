package com.example.doktoribackend.s3.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UploadDirectory {
    PROFILE("images/profiles"),
    MEETING("images/meetings");

    private final String path;
}
