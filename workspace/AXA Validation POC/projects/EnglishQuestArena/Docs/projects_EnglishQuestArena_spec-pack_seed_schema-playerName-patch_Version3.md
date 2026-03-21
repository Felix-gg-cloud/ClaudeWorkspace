# PlayerName 占位符补丁（必须实现）

占位符：{playerName}

适用范围：
- content_items.type=PATTERN 的 textEn / exampleEn
- tasks 的 tts.ttsTextEn、WORD_ORDER.answer.correctTokens、FLASHCARD.promptEn（如包含名字）
- DIALOGUE_REVIEW 的 line.en（如包含名字）

渲染规则：
- 前端展示前：replaceAll("{playerName}", userProfile.displayName ?? "Hero")
- TTS 播放前：同样替换

存储：
- user_profile.display_name（或 users.display_name）
- 提供接口：PUT /api/me/profile {displayName}