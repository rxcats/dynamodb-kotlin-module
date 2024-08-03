package io.github.rxcats.aws.dynamodb.extensions

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import java.text.NumberFormat
import java.text.ParseException

fun JsonNode.toAttributeValue(): AttributeValue {
    if (this.isObject) {
        val attributesMap = mutableMapOf<String, AttributeValue>()
        val attributesIterable = this.fields().iterator()
        for ((key, value) in attributesIterable) {
            attributesMap[key] = value.toAttributeValue()
        }
        return AttributeValue.builder().m(attributesMap).build()
    } else if (this.isArray) {
        val builder = AttributeValue.builder()
        val childAttributes = mutableListOf<AttributeValue>()
        this.forEach { jsonNode: JsonNode ->
            childAttributes += jsonNode.toAttributeValue()
        }
        return builder.l(childAttributes).build()
    } else if (this.isNumber) {
        return AttributeValue.builder().n(this.asText()).build()
    } else if (this.isBoolean) {
        return AttributeValue.builder().bool(this.asBoolean()).build()
    } else if (this.isTextual) {
        return AttributeValue.builder().s(this.asText()).build()
    } else if (this.isNull) {
        return AttributeValue.builder().nul(true).build()
    }
    throw IllegalStateException("Unexpected node type $this")
}

fun AttributeValue.toJsonNode(): JsonNode {
    if (this.hasM()) {
        val objectNode = JsonNodeFactory.instance.objectNode()
        if (this.m().isNullOrEmpty()) return objectNode

        this.m().entries.forEach { entry: Map.Entry<String, AttributeValue> ->
            objectNode.set<JsonNode>(entry.key, entry.value.toJsonNode())
        }
        return objectNode
    } else if (this.hasL()) {
        val arrayNode = JsonNodeFactory.instance.arrayNode()
        this.l().forEach { attributeValue: AttributeValue ->
            arrayNode.add(attributeValue.toJsonNode())
        }
        return arrayNode
    } else if (this.s() != null) {
        return JsonNodeFactory.instance.textNode(this.s())
    } else if (this.bool() != null) {
        return JsonNodeFactory.instance.booleanNode(this.bool())
    } else if (this.n() != null) {
        try {
            return when (val n: Number = NumberFormat.getInstance().parse(this.n())) {
                is Double -> JsonNodeFactory.instance.numberNode(n)
                is Float -> JsonNodeFactory.instance.numberNode(n)
                is Long -> JsonNodeFactory.instance.numberNode(n)
                is Short -> JsonNodeFactory.instance.numberNode(n)
                is Int -> JsonNodeFactory.instance.numberNode(n)
                else -> throw IllegalStateException("Unknown Numeric Type : $n")
            }
        } catch (e: ParseException) {
            throw IllegalStateException("Invalid number: ${this.n()})")
        }
    } else if (this.nul()) {
        return JsonNodeFactory.instance.nullNode()
    }

    throw IllegalStateException("Unexpected attribute value type : $this")
}
