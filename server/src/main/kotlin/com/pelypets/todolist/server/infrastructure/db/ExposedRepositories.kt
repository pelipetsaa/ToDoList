package com.pelypets.todolist.server.infrastructure.db

import com.pelypets.todolist.server.domain.EmailTakenException
import com.pelypets.todolist.server.domain.model.RefreshTokenRecord
import com.pelypets.todolist.server.domain.model.Todo
import com.pelypets.todolist.server.domain.model.TodoStatus
import com.pelypets.todolist.server.domain.model.User
import com.pelypets.todolist.server.domain.port.RefreshTokenRepository
import com.pelypets.todolist.server.domain.port.TodoRepository
import com.pelypets.todolist.server.domain.port.UserRepository
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.util.UUID
import kotlin.time.Instant

private const val UNIQUE_VIOLATION = "23505"

class ExposedUserRepository(private val db: AppDatabase) : UserRepository {

    override suspend fun findById(id: UUID): User? = db.query {
        UsersTable.selectAll().where { UsersTable.id eq id }.singleOrNull()?.toUser()
    }

    override suspend fun findByEmail(email: String): User? = db.query {
        UsersTable.selectAll().where { UsersTable.email eq email }.singleOrNull()?.toUser()
    }

    override suspend fun findByGoogleSubject(subject: String): User? = db.query {
        UsersTable.selectAll().where { UsersTable.googleSubject eq subject }.singleOrNull()?.toUser()
    }

    override suspend fun create(user: User): User = try {
        db.query {
            UsersTable.insert {
                it[id] = user.id
                it[email] = user.email
                it[passwordHash] = user.passwordHash
                it[googleSubject] = user.googleSubject
                it[createdAt] = user.createdAt.toOffsetDateTime()
            }
            user
        }
    } catch (e: ExposedSQLException) {
        if (e.sqlState == UNIQUE_VIOLATION) throw EmailTakenException() else throw e
    }

    override suspend fun linkGoogleSubject(userId: UUID, subject: String) {
        db.query {
            UsersTable.update({ UsersTable.id eq userId }) { it[googleSubject] = subject }
        }
    }

    private fun ResultRow.toUser() = User(
        id = this[UsersTable.id],
        email = this[UsersTable.email],
        passwordHash = this[UsersTable.passwordHash],
        googleSubject = this[UsersTable.googleSubject],
        createdAt = this[UsersTable.createdAt].toKotlinInstant(),
    )
}

class ExposedTodoRepository(private val db: AppDatabase) : TodoRepository {

    override suspend fun findInRange(userId: UUID, from: Instant, to: Instant): List<Todo> = db.query {
        TodosTable.selectAll()
            .where {
                (TodosTable.userId eq userId) and
                    (TodosTable.dueAt greaterEq from.toOffsetDateTime()) and
                    (TodosTable.dueAt less to.toOffsetDateTime())
            }
            .orderBy(TodosTable.dueAt to SortOrder.ASC, TodosTable.createdAt to SortOrder.ASC)
            .map { it.toTodo() }
    }

    override suspend fun findById(userId: UUID, id: UUID): Todo? = db.query {
        TodosTable.selectAll()
            .where { (TodosTable.id eq id) and (TodosTable.userId eq userId) }
            .singleOrNull()
            ?.toTodo()
    }

    override suspend fun insert(todo: Todo): Todo = db.query {
        TodosTable.insert {
            it[id] = todo.id
            it[userId] = todo.userId
            it[title] = todo.title
            it[description] = todo.description
            it[dueAt] = todo.dueAt.toOffsetDateTime()
            it[status] = todo.status.name
            it[createdAt] = todo.createdAt.toOffsetDateTime()
            it[updatedAt] = todo.updatedAt.toOffsetDateTime()
        }
        todo
    }

    override suspend fun update(todo: Todo): Todo? = db.query {
        val updated = TodosTable.update({ (TodosTable.id eq todo.id) and (TodosTable.userId eq todo.userId) }) {
            it[title] = todo.title
            it[description] = todo.description
            it[dueAt] = todo.dueAt.toOffsetDateTime()
            it[status] = todo.status.name
            it[updatedAt] = todo.updatedAt.toOffsetDateTime()
        }
        if (updated > 0) todo else null
    }

    override suspend fun delete(userId: UUID, id: UUID): Boolean = db.query {
        TodosTable.deleteWhere { (TodosTable.id eq id) and (TodosTable.userId eq userId) } > 0
    }

    private fun ResultRow.toTodo() = Todo(
        id = this[TodosTable.id],
        userId = this[TodosTable.userId],
        title = this[TodosTable.title],
        description = this[TodosTable.description],
        dueAt = this[TodosTable.dueAt].toKotlinInstant(),
        status = TodoStatus.valueOf(this[TodosTable.status]),
        createdAt = this[TodosTable.createdAt].toKotlinInstant(),
        updatedAt = this[TodosTable.updatedAt].toKotlinInstant(),
    )
}

class ExposedRefreshTokenRepository(private val db: AppDatabase) : RefreshTokenRepository {

    override suspend fun save(record: RefreshTokenRecord) {
        db.query {
            RefreshTokensTable.insert {
                it[id] = record.id
                it[userId] = record.userId
                it[tokenHash] = record.tokenHash
                it[expiresAt] = record.expiresAt.toOffsetDateTime()
            }
        }
    }

    override suspend fun findByHash(tokenHash: String): RefreshTokenRecord? = db.query {
        RefreshTokensTable.selectAll()
            .where { RefreshTokensTable.tokenHash eq tokenHash }
            .singleOrNull()
            ?.let {
                RefreshTokenRecord(
                    id = it[RefreshTokensTable.id],
                    userId = it[RefreshTokensTable.userId],
                    tokenHash = it[RefreshTokensTable.tokenHash],
                    expiresAt = it[RefreshTokensTable.expiresAt].toKotlinInstant(),
                )
            }
    }

    override suspend fun deleteByHash(tokenHash: String): Boolean = db.query {
        RefreshTokensTable.deleteWhere { RefreshTokensTable.tokenHash eq tokenHash } > 0
    }
}
