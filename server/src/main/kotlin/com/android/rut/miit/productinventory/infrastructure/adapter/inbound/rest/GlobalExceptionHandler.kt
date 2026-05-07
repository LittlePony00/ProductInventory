package com.android.rut.miit.productinventory.infrastructure.adapter.inbound.rest

import com.android.rut.miit.productinventory.application.dto.response.ErrorResponse
import com.android.rut.miit.productinventory.domain.exception.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException::class)
    fun handleNotFound(ex: EntityNotFoundException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse(message = ex.message ?: "Not found", code = "NOT_FOUND"))

    @ExceptionHandler(DuplicateEntityException::class)
    fun handleDuplicate(ex: DuplicateEntityException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErrorResponse(message = ex.message ?: "Already exists", code = "CONFLICT"))

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(ex: AccessDeniedException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ErrorResponse(message = ex.message ?: "Access denied", code = "FORBIDDEN"))

    @ExceptionHandler(InvalidCredentialsException::class)
    fun handleInvalidCredentials(ex: InvalidCredentialsException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ErrorResponse(message = ex.message ?: "Invalid credentials", code = "UNAUTHORIZED"))

    @ExceptionHandler(TokenExpiredException::class, InvalidTokenException::class)
    fun handleTokenError(ex: DomainException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ErrorResponse(message = ex.message ?: "Authentication error", code = "UNAUTHORIZED"))

    @ExceptionHandler(InviteCodeExpiredException::class, InviteCodeAlreadyUsedException::class)
    fun handleInviteCode(ex: DomainException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(message = ex.message ?: "Invalid invite code", code = "BAD_REQUEST"))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val message = ex.bindingResult.fieldErrors
            .joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(message = message, code = "VALIDATION_ERROR"))
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneric(ex: Exception): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse(message = "Internal server error", code = "INTERNAL_ERROR"))
}
