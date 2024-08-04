package io.github.rxcats.aws.dynamodb

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.stereotype.Repository
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.extensions.annotations.DynamoDbAtomicCounter
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
import io.github.rxcats.aws.dynamodb.extensions.DynamoDbKtTableName
import kotlin.test.Test

@DynamoDbKtTableName("Sequence")
data class Sequence(
    @get:DynamoDbPartitionKey
    val pk: String = "",

    @get:DynamoDbAtomicCounter(delta = 1, startValue = 1)
    val counter: Long = 0
) {
    fun key(): Key = Key.builder().partitionValue(pk).build()
}

@Repository
class SequenceRepository : SimpleDynamoDbRepository<Sequence>(Sequence::class)

@SpringBootTest(classes = [DynamoDbTestConfiguration::class])
class SequenceRepositoryTest {
    private val log by loggerK

    @Autowired
    private lateinit var repository: SequenceRepository

    companion object {
        @JvmStatic
        @BeforeAll
        fun beforeAll(@Autowired repo: SequenceRepository) {
            repo.createTable()
        }

        @JvmStatic
        @AfterAll
        fun afterAll(@Autowired repo: SequenceRepository) {
            repo.deleteTable()
        }
    }

    @Test
    fun basicTest() {
        val seq = Sequence(pk = "seq#1")
        repository.save(seq)

        val before = repository.getItem(seq.key())!!

        repeat(10) {
            repository.save(seq)
        }

        val after = repository.getItem(seq.key())!!

        assertThat(before.counter + 10).isEqualTo(after.counter)
    }
}
