package io.github.rxcats.aws.dynamodb

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.rxcats.aws.dynamodb.extensions.toAttributeValue
import io.github.rxcats.aws.dynamodb.extensions.toJsonNode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.dynamodb.model.AttributeValue

class AttributeValueConverterTest {

    private val objectMapper = jacksonObjectMapper()

    @Test
    fun objectJsonNodeToAttributeValueTest() {
        val jsonNode = objectMapper.readTree("""{"id":1}""")
        val attributeValue = jsonNode.toAttributeValue()

        assertThat(attributeValue.hasM()).isTrue()
        assertThat(attributeValue.m()).containsKey("id")
    }

    @Test
    fun arrayJsonNodeToAttributeValueTest() {
        val jsonNode = objectMapper.readTree("""[{"id":1}]""")
        val attributeValue = jsonNode.toAttributeValue()

        assertThat(attributeValue.hasL()).isTrue()
        assertThat(attributeValue.l()).hasSize(1)
        assertThat(attributeValue.l().first().m()).containsKey("id")
    }

    @Test
    fun numberJsonNodeToAttributeValueTest() {
        val jsonNode = objectMapper.readTree("""[1,2,3]""")
        val attributeValue = jsonNode.toAttributeValue()

        assertThat(attributeValue.hasL()).isTrue()
        assertThat(attributeValue.l()).hasSize(3)
        attributeValue.l().forEach {
            assertThat(it.n().toInt()).isBetween(1, 3)
        }
    }

    @Test
    fun booleanJsonNodeToAttributeValueTest() {
        val jsonNode = objectMapper.readTree("""[true,false,true]""")
        val attributeValue = jsonNode.toAttributeValue()

        assertThat(attributeValue.hasL()).isTrue()
        assertThat(attributeValue.l()).hasSize(3)

        val values = attributeValue.l().map { it.bool() }
        val trueValues = values.filter { it == true }
        val falseValues = values.filter { it == false }

        assertThat(trueValues).hasSize(2)
        assertThat(falseValues).hasSize(1)
    }

    @Test
    fun textualJsonNodeToAttributeValueTest() {
        val jsonNode = objectMapper.readTree("""["a","b","c"]""")
        val attributeValue = jsonNode.toAttributeValue()

        assertThat(attributeValue.hasL()).isTrue()
        assertThat(attributeValue.l()).hasSize(3)
        assertThat(attributeValue.l().map { it.s() }).containsExactly("a", "b", "c")
    }

    @Test
    fun nullJsonNodeToAttributeValueTest() {
        val jsonNode = objectMapper.readTree("""[null,null,null]""")
        val attributeValue = jsonNode.toAttributeValue()

        assertThat(attributeValue.hasL()).isTrue()
        assertThat(attributeValue.l()).hasSize(3)
        attributeValue.l().forEach {
            assertThat(it.nul()).isTrue()
        }
    }

    @Test
    fun objectAttributeValueToJsonNodeTest() {
        val jsonNode = AttributeValue.builder()
            .m(mapOf("id" to AttributeValue.builder().n("1").build()))
            .build()
            .toJsonNode()

        assertThat(jsonNode.isObject).isTrue()
        assertThat(jsonNode.get("id").asInt()).isEqualTo(1)
    }

    @Test
    fun arrayAttributeValueToJsonNodeTest() {
        val jsonNode = AttributeValue.builder()
            .l(AttributeValue.builder().m(mapOf("id" to AttributeValue.builder().n("1").build())).build())
            .build()
            .toJsonNode()

        assertThat(jsonNode.isArray).isTrue()
        assertThat(jsonNode.first().get("id").asInt()).isEqualTo(1)
    }

    @Test
    fun numberAttributeValueToJsonNodeTest() {
        val jsonNode = AttributeValue.builder()
            .l(
                AttributeValue.builder().n("1").build(),
                AttributeValue.builder().n("2").build(),
                AttributeValue.builder().n("1").build()
            )
            .build()
            .toJsonNode()

        assertThat(jsonNode.isArray).isTrue()
        jsonNode.forEach {
            assertThat(it.asInt()).isBetween(1, 3)
        }
    }

    @Test
    fun booleanAttributeValueToJsonNodeTest() {
        val jsonNode = AttributeValue.builder()
            .l(
                AttributeValue.builder().bool(true).build(),
                AttributeValue.builder().bool(false).build(),
                AttributeValue.builder().bool(true).build()
            )
            .build()
            .toJsonNode()

        assertThat(jsonNode.isArray).isTrue()

        val trueValues = jsonNode.filter { it.asBoolean() }
        val falseValues =jsonNode.filter { !it.asBoolean() }

        assertThat(trueValues).hasSize(2)
        assertThat(falseValues).hasSize(1)
    }

    @Test
    fun textualAttributeValueToJsonNodeTest() {
        val jsonNode = AttributeValue.builder()
            .l(
                AttributeValue.builder().s("a").build(),
                AttributeValue.builder().s("b").build(),
                AttributeValue.builder().s("c").build()
            )
            .build()
            .toJsonNode()

        assertThat(jsonNode.isArray).isTrue()
        assertThat(jsonNode.map { it.asText() }).containsExactly("a", "b", "c")
    }

    @Test
    fun nullAttributeValueToJsonNodeTest() {
        val jsonNode = AttributeValue.builder()
            .l(
                AttributeValue.builder().nul(true).build(),
                AttributeValue.builder().nul(true).build(),
                AttributeValue.builder().nul(true).build()
            )
            .build()
            .toJsonNode()

        assertThat(jsonNode.isArray).isTrue()
        jsonNode.forEach {
            assertThat(it.isNull).isTrue()
        }
    }
}
