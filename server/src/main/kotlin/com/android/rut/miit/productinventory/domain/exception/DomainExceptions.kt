package com.android.rut.miit.productinventory.domain.exception

open class DomainException(message: String) : RuntimeException(message)

class EntityNotFoundException(entity: String, id: Any) :
    DomainException("$entity with id '$id' not found")

class DuplicateEntityException(message: String) :
    DomainException(message)

class AccessDeniedException(message: String = "Access denied") :
    DomainException(message)

class InvalidCredentialsException :
    DomainException("Invalid email or password")

class TokenExpiredException(message: String = "Token has expired") :
    DomainException(message)

class InvalidTokenException(message: String = "Invalid token") :
    DomainException(message)

class InviteCodeExpiredException :
    DomainException("Invite code has expired")

class InviteCodeAlreadyUsedException :
    DomainException("Invite code has already been used")

class BarcodeNotFoundException(val barcode: String) :
    DomainException("Product not found for barcode: $barcode")
