package io.github.rxcats.aws.dynamodb

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import io.github.rxcats.aws.dynamodb.query.DynamoDbCreateTableParam
import io.github.rxcats.aws.dynamodb.query.DynamoDbTableOperations
import io.github.rxcats.aws.dynamodb.query.DynamoDbUpdateTableParam
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
            forceDeleteTable(tableOps, "junit_TableOps")
            forceDeleteTable(tableOps, "junit_TableUpdateOps")
        }

        private fun forceDeleteTable(tableOps: DynamoDbTableOperations, tableName: String) {
            try {
                tableOps.deleteTable(tableName)
            } catch (e: Exception) {
                // ignore
            }
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

    @Test
    fun updateTableTest() {
        val tableName = "junit_TableUpdateOps"

        ops.createTable(
            DynamoDbCreateTableParam(
                tableName = tableName,
                partitionKeyName = "pk",
            )
        )

        ops.updateTableProvision(
            DynamoDbUpdateTableParam(
                tableName = tableName,
                readCapacityUnits = 20,
                writeCapacityUnits = 20,
            )
        )

        val desc = ops.describeTable(tableName)

        assertThat(desc.table().provisionedThroughput().readCapacityUnits()).isEqualTo(20)
        assertThat(desc.table().provisionedThroughput().writeCapacityUnits()).isEqualTo(20)
    }

}
