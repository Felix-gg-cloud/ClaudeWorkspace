-- V2__add_avatar.sql — 添加用户头像字段
ALTER TABLE users ADD COLUMN avatar VARCHAR(20) DEFAULT '⚔️';
