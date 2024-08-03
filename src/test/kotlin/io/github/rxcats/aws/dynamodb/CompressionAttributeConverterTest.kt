package io.github.rxcats.aws.dynamodb

import io.github.rxcats.aws.dynamodb.converter.CompressionAttributeConverter
import io.github.rxcats.aws.dynamodb.extensions.DynamoDbKtTableName
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.stereotype.Repository
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbConvertedBy
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbIgnore
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
import kotlin.test.Test

@DynamoDbKtTableName("CompressionData")
data class CompressionData(
    @get:DynamoDbPartitionKey
    val pk: String,

    @get:DynamoDbConvertedBy(CompressionAttributeConverter::class)
    val data: ByteArray,
) {
    @get:DynamoDbIgnore
    val dataAsString: String
        get() = data.decodeToString()

    fun key(): Key = Key.builder().partitionValue(pk).build()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CompressionData

        return pk == other.pk
    }

    override fun hashCode(): Int {
        return pk.hashCode()
    }
}

@Repository
class CompressionDataRepository : SimpleDynamoDbRepository<CompressionData>(CompressionData::class)

@SpringBootTest(classes = [DynamoDbTestConfiguration::class])
class CompressionAttributeConverterTest {
    private val log by loggerK

    @Autowired
    private lateinit var repository: CompressionDataRepository

    companion object {
        @JvmStatic
        @BeforeAll
        fun beforeAll(@Autowired repo: CompressionDataRepository) {
            repo.createTable()
        }

        @JvmStatic
        @AfterAll
        fun afterAll(@Autowired repo: CompressionDataRepository) {
            repo.deleteTable()
        }
    }

    @Test
    fun basicTest() {
        val sampleData = """
            {
                "widget": {
                    "debug": "on",
                    "window": {
                        "title": "Sample Konfabulator Widget",
                        "name": "main_window",
                        "width": 500,
                        "height": 500
                    },
                    "image": {
                        "src": "Images/Sun.png",
                        "name": "sun1",
                        "hOffset": 250,
                        "vOffset": 250,
                        "alignment": "center"
                    },
                    "text": {
                        "data": "Click Here",
                        "size": 36,
                        "style": "bold",
                        "name": "text1",
                        "hOffset": 250,
                        "vOffset": 100,
                        "alignment": "center",
                        "onMouseUp": "sun1.opacity = (sun1.opacity / 100) * 90;"
                    }
                }
            }
        """.trimIndent()

        val dataBytes = sampleData.toByteArray()

        val data = CompressionData(
            pk = "compression#1001",
            data = dataBytes,
        )

        repository.save(data)

        val after = repository.getItem(data.key())
        assertThat(after).isNotNull()
        assertThat(after?.dataAsString).isEqualTo(sampleData)
    }

}
