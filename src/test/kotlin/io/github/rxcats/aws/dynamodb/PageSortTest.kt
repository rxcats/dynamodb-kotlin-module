package io.github.rxcats.aws.dynamodb

import io.github.rxcats.aws.dynamodb.query.DynamoDbPageQueryParam
import io.github.rxcats.aws.dynamodb.query.DynamoDbQueryConditional
import io.github.rxcats.aws.dynamodb.query.DynamoDbSortDirection
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.stereotype.Repository
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey

data class MessageThread(
    @get:DynamoDbPartitionKey
    val pk: String,

    @get:DynamoDbSortKey
    val threadNo: Int
)

@Repository
class MessageThreadRepository : SimpleDynamoDbRepository<MessageThread>(MessageThread::class)

@SpringBootTest(classes = [DynamoDbTestConfiguration::class])
class MessageThreadTest {

    @Autowired
    private lateinit var repository: MessageThreadRepository

    companion object {
        @JvmStatic
        @BeforeAll
        fun beforeAll(@Autowired repo: MessageThreadRepository) {
            repo.createTable()

            val threadData = (1..10).map { i ->
                MessageThread(pk = "user#1001", threadNo = i)
            }

            repo.batchSaveItems(threadData)
        }

        @JvmStatic
        @AfterAll
        fun afterAll(@Autowired repo: MessageThreadRepository) {
            repo.deleteTable()
        }
    }

    @Test
    fun threadAscTest() {
        val key = Key.builder()
            .partitionValue("user#1001")
            .sortValue(0)
            .build()

        val ascPage = repository.getPage(
            DynamoDbPageQueryParam(
                key = key,
                queryConditional = DynamoDbQueryConditional.SORT_GREATER_THAN,
                limit = 10,
                sort = DynamoDbSortDirection.ASC
            )
        )

        assertThat(ascPage.items().first().threadNo).isEqualTo(1)
        assertThat(ascPage.items().last().threadNo).isEqualTo(10)
    }

    @Test
    fun threadDescTest() {
        val key = Key.builder()
            .partitionValue("user#1001")
            .sortValue(0)
            .build()

        val descPage = repository.getPage(
            DynamoDbPageQueryParam(
                key = key,
                queryConditional = DynamoDbQueryConditional.SORT_GREATER_THAN,
                limit = 10,
                sort = DynamoDbSortDirection.DESC
            )
        )

        assertThat(descPage.items().first().threadNo).isEqualTo(10)
        assertThat(descPage.items().last().threadNo).isEqualTo(1)
    }
}
