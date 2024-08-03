package io.github.rxcats.aws.dynamodb

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import io.github.rxcats.aws.dynamodb.query.DynamoDbCreateTableParam
import io.github.rxcats.aws.dynamodb.query.DynamoDbTableOperations
import kotlin.test.Test

@SpringBootTest(classes = [DynamoDbTestConfiguration::class])
class DynamoDbTableOperationsTest {
    private val log by loggerK

    @Autowired
    private lateinit var ops: DynamoDbTableOperations

    companion object {
        @JvmStatic
        @AfterAll
        fun afterAll(@Autowired tableOps: DynamoDbTableOperations) {
            tableOps.deleteTable("junit_TableOps")
        }
    }

    @Test
    fun tableOpsTest() {
        val tableName = "junit_TableOps"

        ops.createTable(
            DynamoDbCreateTableParam(
                tableName = tableName,
                partitionKeyName = "pk",
            )
        )

        val desc = ops.describeTable(tableName)

        assertThat(desc.table().tableName()).isEqualTo(tableName)
    }

}
