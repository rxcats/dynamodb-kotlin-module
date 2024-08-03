package io.github.rxcats.aws.dynamodb.extensions

@Target(AnnotationTarget.PROPERTY)
annotation class DynamoDbKtFlatten

@Target(AnnotationTarget.CLASS)
annotation class DynamoDbKtPreserveEmptyObject

@Target(AnnotationTarget.CLASS)
annotation class DynamoDbKtTableName(val name: String = "")
