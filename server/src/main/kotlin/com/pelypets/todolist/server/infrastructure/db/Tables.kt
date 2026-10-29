package com.pelypets.todolist.server.infrastructure.db

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.time.Instant
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

object UsersTable : Table("users") {
    val id = javaUUID("id")
    val email = varchar("email", 254).uniqueIndex()
    val passwordHash = varchar("password_hash", 100).nullable()
    val googleSubject = varchar("google_subject", 255).nullable().uniqueIndex()
    val createdAt = timestampWithTimeZone("created_at")

    override val primaryKey = PrimaryKey(id)
}

object RefreshTokensTable : Table("refresh_tokens") {
    val id = javaUUID("id")
    val userId = javaUUID("user_id").references(UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val tokenHash = varchar("token_hash", 64).uniqueIndex()
    val expiresAt = timestampWithTimeZone("expires_at")

    override val primaryKey = PrimaryKey(id)
}

object TodosTable : Table("todos") {
    val id = javaUUID("id")
    val userId = javaUUID("user_id").references(UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val title = varchar("title", 200)
    val description = varchar("description", 2000)
    val dueAt = timestampWithTimeZone("due_at")
    val status = varchar("status", 16)
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")

    override val primaryKey = PrimaryKey(id)
}

internal fun Instant.toOffsetDateTime(): OffsetDateTime = toJavaInstant().atOffset(ZoneOffset.UTC)

internal fun OffsetDateTime.toKotlinInstant(): Instant = toInstant().toKotlinInstant()
