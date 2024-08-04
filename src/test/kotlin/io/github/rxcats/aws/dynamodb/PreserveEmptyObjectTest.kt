package io.github.rxcats.aws.dynamodb

import io.github.rxcats.aws.dynamodb.extensions.DataClassTableSchema
import io.github.rxcats.aws.dynamodb.extensions.DynamoDbKtPreserveEmptyObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.dynamodb.model.AttributeValue

class PreserveEmptyObjectTest {

    data class Container(
        val id: String,
        val nested: Nested?
    )

    @DynamoDbKtPreserveEmptyObject
    data class Nested(
        val foo: String?,
        val bar: String?
    )

    @Test
    fun preserveEmptyObjectTest() {
        val schema = DataClassTableSchema(Container::class)

        val item = mapOf(
            "id" to AttributeValue.builder().s("id#1001").build(),
            "nested" to AttributeValue.builder().m(emptyMap()).build(),
        )

        val map = schema.mapToItem(item)

        assertThat(map.nested).isNotNull()
        assertThat(map.nested?.foo).isNull()
        assertThat(map.nested?.bar).isNull()
    }

}
