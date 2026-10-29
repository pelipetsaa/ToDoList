package com.pelypets.todolist.server.domain

sealed class DomainException(message: String) : RuntimeException(message)

class ValidationException(message: String) : DomainException(message)

class UnauthorizedException(message: String = "Unauthorized") : DomainException(message)

class NotFoundException(message: String = "Not found") : DomainException(message)

class EmailTakenException(message: String = "Email is already registered") : DomainException(message)
