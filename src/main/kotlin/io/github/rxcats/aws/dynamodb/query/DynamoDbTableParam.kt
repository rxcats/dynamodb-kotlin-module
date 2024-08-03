package io.github.rxcats.aws.dynamodb.query

data class DynamoDbCreateTableParam(
    val tableName: String,
    val partitionKeyName: String,
    val sortKeyName: String = "",
    val readCapacityUnits: Long = 10L,
    val writeCapacityUnits: Long = 10L,
)

data class DynamoDbUpdateTableParam(
    val tableName: String,
    val readCapacityUnits: Long,
    val writeCapacityUnits: Long,
)
