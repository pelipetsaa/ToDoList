package com.pelypets.todolist.server.domain.port

import com.pelypets.todolist.server.domain.model.GoogleIdentity
import com.pelypets.todolist.server.domain.model.RefreshTokenRecord
import com.pelypets.todolist.server.domain.model.Todo
import com.pelypets.todolist.server.domain.model.User
import java.util.UUID
import kotlin.time.Instant

interface UserRepository {
    suspend fun findById(id: UUID): User?
    suspend fun findByEmail(email: String): User?
    suspend fun findByGoogleSubject(subject: String): User?

    /** @throws com.pelypets.todolist.server.domain.EmailTakenException if the email is already used. */
    suspend fun create(user: User): User
    suspend fun linkGoogleSubject(userId: UUID, subject: String)
}

interface TodoRepository {
    suspend fun findInRange(userId: UUID, from: Instant, to: Instant): List<Todo>
    suspend fun findById(userId: UUID, id: UUID): Todo?
    suspend fun insert(todo: Todo): Todo
    suspend fun update(todo: Todo): Todo?
    suspend fun delete(userId: UUID, id: UUID): Boolean
}

interface RefreshTokenRepository {
    suspend fun save(record: RefreshTokenRecord)
    suspend fun findByHash(tokenHash: String): RefreshTokenRecord?
    suspend fun deleteByHash(tokenHash: String): Boolean
}

interface PasswordHasher {
    suspend fun hash(raw: String): String
    suspend fun verify(raw: String, hash: String): Boolean
}

interface AccessTokenIssuer {
    val ttlSeconds: Long
    fun issue(userId: UUID): String
}

interface RefreshTokenGenerator {
    fun generate(): String
    fun hash(token: String): String
}

interface GoogleTokenVerifier {
    /** Returns the verified identity, or null if the token is invalid or the email is not verified. */
    suspend fun verify(idToken: String): GoogleIdentity?
}
