package eric.bitria.hexon.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import eric.bitria.hexon.database.tables.Friends
import eric.bitria.hexon.database.tables.Users
import io.ktor.server.config.ApplicationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

object DatabaseFactory {

    private val logger = LoggerFactory.getLogger(javaClass)
    private lateinit var database: Database

    fun init(config: ApplicationConfig) {
        val driverClassName = config.property("storage.driverClassName").getString()
        val jdbcURL = config.property("storage.jdbcURL").getString()
        val username = config.property("storage.username").getString()
        val password = config.property("storage.password").getString()

        val dataSource = retryConnect(jdbcURL, driverClassName, username, password)
        database = Database.connect(dataSource)

        transaction(database) {
            SchemaUtils.create(Users, Friends)
        }
    }

    private fun retryConnect(
        url: String,
        driver: String,
        user: String,
        pass: String,
        maxRetries: Int = 10
    ): HikariDataSource {
        var currentRetry = 0
        while (currentRetry < maxRetries) {
            try {
                return createHikariDataSource(url, driver, user, pass)
            } catch (e: Exception) {
                currentRetry++
                logger.warn("Database connection failed (attempt $currentRetry/$maxRetries). Retrying in 2 seconds...")
                runBlocking { delay(2000) }
            }
        }
        throw RuntimeException("Could not connect to database after $maxRetries attempts")
    }

    private fun createHikariDataSource(
        url: String,
        driver: String,
        user: String,
        pass: String
    ): HikariDataSource =
        HikariDataSource(
            HikariConfig().apply {
                driverClassName = driver
                jdbcUrl = url
                username = user
                password = pass
                maximumPoolSize = 3
                isAutoCommit = false
                transactionIsolation = "TRANSACTION_REPEATABLE_READ"
                // Faster fail for the retry logic to catch it
                initializationFailTimeout = 1000 
                validate()
            }
        )

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO, database) {
            block()
        }
}