[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

# AWS SDK for Java 2.x DynamoDbEnhancedClient Kotlin Module

Kotlin module for 2.x DynamoDbEnhancedClient AWS SDK.

Adapting an idiomatic kotlin data model for use with the v2 dynamodb mapper is a pain, and full of compromises.
data classes emulate a bean, which nullifies much of the advantages of data classes.
This module provides a new `TableSchema` implementation that adds support for kotlin data classes.

- properties can be immutable; i.e. `val` is allowed

## Requirements

Java 17 and 21

## Quickstart with Spring Boot

```kotlin

// configuration DynamoDbClient, DynamoDbEnhancedClient
@Configuration(proxyBeanMethods = false)
@ComponentScan
@EnableAutoConfiguration
class DynamoDbConfiguration

@ConfigurationProperties(prefix = "app.aws.dynamodb")
data class DynamoDbProperties(
    val endpoint: String,
    val region: Region,
    val tableNamePrefix: String,
)

@EnableConfigurationProperties(DynamoDbProperties::class)
@Configuration(proxyBeanMethods = false)
class DynamoDbConfiguration {
    @Bean
    fun awsCredentialsProvider(): AwsCredentialsProvider {
        return StaticCredentialsProvider.create(
            AwsBasicCredentials.create("fake", "fake")
        )
    }

    @Bean
    fun dynamoDbClient(
        awsCredentialsProvider: AwsCredentialsProvider,
        dynamoDbProperties: DynamoDbProperties
    ): DynamoDbClient {
        return DynamoDbClient.builder()
            .credentialsProvider(awsCredentialsProvider)
            .endpointOverride(URI.create(dynamoDbProperties.endpoint))
            .region(dynamoDbProperties.region)
            .build()
    }

    @Bean
    fun dynamoDbEnhancedClient(dynamoDbClient: DynamoDbClient): DynamoDbEnhancedClient {
        return DynamoDbEnhancedClient.builder()
            .dynamoDbClient(dynamoDbClient)
            .extensions(
                AutoGeneratedTimestampRecordExtension.create(),
                AtomicCounterExtension.builder().build(),
                VersionedRecordExtension.builder().build()
            )
            .build()
    }

    @Bean
    fun dynamoDbTableOperations(dynamoDbClient: DynamoDbClient): DynamoDbTableOperations {
        return DynamoDbTableOperations(dynamoDbClient)
    }
}

// create abstract repository
abstract class SimpleDynamoDbRepository<T : Any>(
    private val type: KClass<T>
) : ApplicationContextAware, AbstractDynamoDbRepository<T>() {
    override fun setApplicationContext(applicationContext: ApplicationContext) {
        super.enhancedClient = applicationContext.getBean(DynamoDbEnhancedClient::class.java)
        val properties = applicationContext.getBean(DynamoDbProperties::class.java)
        super.entityType = type
        super.table = enhancedClient.tableOf(type, properties.tableNamePrefix)
    }
}

// create a data class model, making sure to give it a partition key
@DynamoDbKtTableName("Users") // annotations for custom table names
data class User(
    @get:DynamoDbPartitionKey
    val pk: String = "", // properties can be immutable

    val name: String = "",

    @get:DynamoDbAutoGeneratedTimestampAttribute
    @get:DynamoDbUpdateBehavior(UpdateBehavior.WRITE_ALWAYS)
    val updatedAt: Instant = Instant.EPOCH,

    @get:DynamoDbAutoGeneratedTimestampAttribute
    @get:DynamoDbUpdateBehavior(UpdateBehavior.WRITE_IF_NOT_EXISTS)
    val createdAt: Instant = Instant.EPOCH,
) {
    fun key(): Key = Key.builder().partitionValue(pk).build()
}

// create a user repository
@Repository
class UserRepository : SimpleDynamoDbRepository<User>(User::class)

// create a table
repository.createTable()

val before = User(
    pk = "tester#1001",
    name = "tester",
)
println(before)

// save item
val saved: User = repository.save(before)
println(saved)

// get item by key
val after: User = repository.getItem(user.key())
println(after)

// delete item
repository.delete(before.key())


// transaction save item
val transactionSaved: User = repository.transactionSave(before)
println(transactionSaved)

// transaction get item by key
val transactionGetUser: User = repository.transactionGetItem(user.key())
println(transactionGetUser)

// transaction delete item
repository.transactionDelete(before.key())


```
