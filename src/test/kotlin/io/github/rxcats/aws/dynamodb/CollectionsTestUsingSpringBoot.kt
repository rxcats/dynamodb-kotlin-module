package io.github.rxcats.aws.dynamodb

import io.github.rxcats.aws.dynamodb.extensions.DynamoDbKtTableName
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.stereotype.Repository
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey

@DynamoDbKtTableName("Items")
data class Item(
    @get:DynamoDbPartitionKey
    val pk: String,

    val list: List<Int>,

    val map: Map<String, String>,

    val set: Set<Int>
) {
    fun key(): Key = Key.builder().partitionValue(pk).build()
}

@Repository
class ItemRepository : SimpleDynamoDbRepository<Item>(Item::class)

@SpringBootTest(classes = [DynamoDbTestConfiguration::class])
class ItemRepositoryTest {

    @Autowired
    private lateinit var repository: ItemRepository

    companion object {
        @JvmStatic
        @BeforeAll
        fun beforeAll(@Autowired repo: ItemRepository) {
            repo.createTable()
        }

        @JvmStatic
        @AfterAll
        fun afterAll(@Autowired repo: ItemRepository) {
            repo.deleteTable()
        }
    }

    @Test
    fun basicTest() {
        val item = Item(
            pk = "item#1001",
            list = listOf(1, 2, 3),
            map = mapOf("name" to "item#1", "value" to "d"),
            set = setOf(1, 2, 3),
        )

        repository.save(item)

        val after = repository.getItem(item.key())
        assertThat(after).isNotNull()
        assertThat(after?.list).containsExactly(1, 2, 3)
        assertThat(after?.set).containsExactly(1, 2, 3)
        assertThat(after?.map).containsKeys("name", "value")
    }

}
