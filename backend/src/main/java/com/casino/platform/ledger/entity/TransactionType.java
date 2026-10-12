package com.casino.platform.ledger.entity;

public enum TransactionType {
    DEPOSIT,             // 입금 (가상 머니 지급)
    WITHDRAW,            // 출금
    GAME_BET,            // 게임 베팅 (차감)
    GAME_WIN,            // 게임 승리 (지급)
    GAME_BET_ROLLBACK,   // 베팅 롤백 (환급)
    GAME_WIN_ROLLBACK,   // 승리 롤백 (차감)
    ADJUSTMENT           // 조정
}
