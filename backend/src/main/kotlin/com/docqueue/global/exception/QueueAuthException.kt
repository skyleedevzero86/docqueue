package com.docqueue.global.exception

sealed class QueueError(override val message: String) : RuntimeException(message) {
    object UserAlreadyRegistered : QueueError("이미 등록된 사용자입니다")
    object InvalidToken : QueueError("유효하지 않은 토큰입니다")
}