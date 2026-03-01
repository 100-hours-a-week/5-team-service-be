ALTER TABLE chatting_room_members
    ADD COLUMN nickname          VARCHAR(20)  NULL COMMENT '참여 시점 닉네임' AFTER user_id,
    ADD COLUMN profile_image_url VARCHAR(512) NULL COMMENT '참여 시점 프로필 이미지 URL' AFTER nickname;
