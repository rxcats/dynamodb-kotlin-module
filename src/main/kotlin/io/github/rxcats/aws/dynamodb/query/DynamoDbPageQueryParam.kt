package io.github.rxcats.aws.dynamodb.query

import software.amazon.awssdk.enhanced.dynamodb.Key

data class DynamoDbPageQueryParam(
    val key: Key,
    val queryConditional: DynamoDbQueryConditional,
    val limit: Int = 10,
    val sort: DynamoDbSortDirection = DynamoDbSortDirection.ASC,
)
