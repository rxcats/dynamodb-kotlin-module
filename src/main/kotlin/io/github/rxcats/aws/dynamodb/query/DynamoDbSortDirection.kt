package io.github.rxcats.aws.dynamodb.query

enum class DynamoDbSortDirection(val scanIndexForward: Boolean) {
    ASC(true), DESC(false)
}

enum class DynamoDbQueryConditional {
    SORT_BEGINS_WITH,
    SORT_LESS_THAN,
    SORT_GREATER_THAN,
    KEY_EQUAL_TO,
}
