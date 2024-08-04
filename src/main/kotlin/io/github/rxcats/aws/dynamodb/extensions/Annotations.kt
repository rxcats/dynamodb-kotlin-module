package io.github.rxcats.aws.dynamodb.extensions

import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter
import kotlin.reflect.KClass

@Target(AnnotationTarget.PROPERTY)
annotation class DynamoDbKtFlatten

@Target(AnnotationTarget.CLASS)
annotation class DynamoDbKtPreserveEmptyObject

@Target(AnnotationTarget.CLASS)
annotation class DynamoDbKtTableName(val name: String = "")

@Target(AnnotationTarget.PROPERTY)
annotation class DynamoDbKtConvertedBy(val value: KClass<out AttributeConverter<out Any>>)

@Target(AnnotationTarget.PROPERTY)
annotation class DynamoDbKtAttribute(val name: String)
