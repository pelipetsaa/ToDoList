package com.pelypets.todolist.server.infrastructure.db

import com.pelypets.todolist.server.config.DatabaseConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.io.Closeable

class AppDatabase(
    private val dataSource: HikariDataSource,
    val database: Database,
) : Closeable {

    suspend fun <T> query(block: JdbcTransaction.() -> T): T =
        withContext(Dispatchers.IO) { transaction(database) { block() } }

    override fun close() = dataSource.close()

    companion object {
        fun connect(config: DatabaseConfig): AppDatabase {
            val dataSource = HikariDataSource(
                HikariConfig().apply {
                    jdbcUrl = config.url
                    username = config.user
                    password = config.password
                    maximumPoolSize = config.maxPoolSize
                    isAutoCommit = false
                    transactionIsolation = "TRANSACTION_READ_COMMITTED"
                },
            )
            Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate()
            return AppDatabase(dataSource, Database.connect(dataSource))
        }
    }
}
